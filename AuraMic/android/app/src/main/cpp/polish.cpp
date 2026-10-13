#include "polish.h"

#include <algorithm>
#include <cmath>
#include <cstdlib>
#include <deque>
#include <vector>

namespace aura {

namespace {
constexpr double kPi = 3.14159265358979323846;

struct Biquad {
    double b0 = 1, b1 = 0, b2 = 0, a1 = 0, a2 = 0, z1 = 0, z2 = 0;
    float process(float x) {
        const double y = b0 * x + z1;
        z1 = b1 * x - a1 * y + z2;
        z2 = b2 * x - a2 * y;
        return static_cast<float>(y);
    }
    void run(float *x, size_t n) { for (size_t i = 0; i < n; ++i) x[i] = process(x[i]); }
    void set(double nb0, double nb1, double nb2, double na0, double na1, double na2) {
        b0 = nb0 / na0; b1 = nb1 / na0; b2 = nb2 / na0; a1 = na1 / na0; a2 = na2 / na0;
    }
};

// RBJ audio-EQ-cookbook designs
Biquad highpass(double f, double q, double sr) {
    Biquad b; const double w = 2 * kPi * f / sr, c = std::cos(w), a = std::sin(w) / (2 * q);
    b.set((1 + c) / 2, -(1 + c), (1 + c) / 2, 1 + a, -2 * c, 1 - a); return b;
}
Biquad peaking(double f, double q, double gainDb, double sr) {
    Biquad b; const double A = std::pow(10, gainDb / 40), w = 2 * kPi * f / sr, c = std::cos(w), a = std::sin(w) / (2 * q);
    b.set(1 + a * A, -2 * c, 1 - a * A, 1 + a / A, -2 * c, 1 - a / A); return b;
}
Biquad lowShelf(double f, double gainDb, double sr) {
    Biquad b; const double A = std::pow(10, gainDb / 40), w = 2 * kPi * f / sr, c = std::cos(w), s = std::sin(w);
    const double al = s / 2 * std::sqrt(2.0), t = 2 * std::sqrt(A) * al;
    b.set(A * ((A + 1) - (A - 1) * c + t), 2 * A * ((A - 1) - (A + 1) * c), A * ((A + 1) - (A - 1) * c - t),
          (A + 1) + (A - 1) * c + t, -2 * ((A - 1) + (A + 1) * c), (A + 1) + (A - 1) * c - t); return b;
}
Biquad highShelf(double f, double gainDb, double sr) {
    Biquad b; const double A = std::pow(10, gainDb / 40), w = 2 * kPi * f / sr, c = std::cos(w), s = std::sin(w);
    const double al = s / 2 * std::sqrt(2.0), t = 2 * std::sqrt(A) * al;
    b.set(A * ((A + 1) + (A - 1) * c + t), -2 * A * ((A - 1) + (A + 1) * c), A * ((A + 1) + (A - 1) * c - t),
          (A + 1) - (A - 1) * c + t, 2 * ((A - 1) - (A + 1) * c), (A + 1) - (A - 1) * c - t); return b;
}

// De-esser: attenuate only the >5 kHz part when sibilance energy jumps above the voiced level.
void deEss(float *x, size_t n, int sr) {
    // Streaming: the high-passed copy of sample i is computed just before x[i] is changed (no clip-sized buffer).
    Biquad hp = highpass(5200, 0.707, sr);
    const double a = std::exp(-1.0 / (0.004 * sr)), r = std::exp(-1.0 / (0.05 * sr));
    double envS = 1e-9, envB = 1e-9, g = 1.0;
    for (size_t i = 0; i < n; ++i) {
        const float sib = hp.process(x[i]);
        const double s2 = static_cast<double>(sib) * sib, b2 = static_cast<double>(x[i]) * x[i];
        envS = s2 > envS ? a * envS + (1 - a) * s2 : r * envS + (1 - r) * s2;
        envB = b2 > envB ? a * envB + (1 - a) * b2 : r * envB + (1 - r) * b2;
        const double ratio = envS / (envB + 1e-12);           // share of energy above 5 kHz
        const double target = ratio > 0.45 ? std::pow(0.45 / ratio, 0.6) : 1.0;  // gentle, max ~ -9 dB
        g = target < g ? 0.5 * g + 0.5 * target : 0.995 * g + 0.005 * target;
        x[i] = static_cast<float>(x[i] - (1.0 - g) * sib);
    }
}

constexpr double kGateDepthDb = 0.0;   // pause-noise expander depth (0 = off)
constexpr double kGateBelowDb = 36.0;  // pauses = frames this far below the speech level
// Experiment override (host benchmark only): environment variable, falls back to the compiled default.
double envOr(const char *name, double def) { const char *v = std::getenv(name); return v ? std::atof(v) : def; }

// Downward compressor (soft knee, RMS detector). No make-up gain: loudness is set afterwards.
void compress(float *x, size_t n, int sr, double thrDb, double ratio, double atkMs, double relMs, double kneeDb) {
    const double det = std::exp(-1.0 / (0.010 * sr));
    const double aA = std::exp(-1.0 / (atkMs * 1e-3 * sr)), aR = std::exp(-1.0 / (relMs * 1e-3 * sr));
    // Below the knee no gain reduction is wanted: compare the detector in the power domain and skip the log there.
    const double kneeLoMs = std::pow(10.0, (thrDb - kneeDb / 2) / 10.0) - 1e-12;
    double ms = 1e-10, grDb = 0.0;
    for (size_t i = 0; i < n; ++i) {
        ms = det * ms + (1 - det) * static_cast<double>(x[i]) * x[i];
        double want = 0;
        if (ms > kneeLoMs) {
            const double lvl = 10 * std::log10(ms + 1e-12);
            const double over = lvl - thrDb;
            if (2 * over < -kneeDb) want = 0;
            else if (2 * std::fabs(over) <= kneeDb) want = (1 / ratio - 1) * std::pow(over + kneeDb / 2, 2) / (2 * kneeDb);
            else want = (1 / ratio - 1) * over;
            want = -want;                               // positive = reduction in dB
        }
        grDb = want > grDb ? aA * grDb + (1 - aA) * want : aR * grDb + (1 - aR) * want;
        // Gains within 1e-7 dB of unity are exactly 1.0f after rounding: skip the pow.
        if (grDb > 1e-7) x[i] *= static_cast<float>(std::pow(10.0, -grDb / 20));
    }
}

// Pause-noise expander: DeepFilterNet leaves a faint floor in pauses that the make-up gain of the loudness stage lifts
// again. Frames that stay far below the speech level (no frame above the threshold within the hold / look-ahead
// window, so word onsets and tails are untouched) are attenuated by at most `depthDb`, opening fast and closing slowly.
void expandPauses(float *x, size_t n, int sr, double depthDb, double belowSpeechDb) {
    const size_t F = static_cast<size_t>(0.010 * sr);
    if (depthDb <= 0.0 || F == 0 || n < F * 40) return;
    const size_t nf = n / F;
    std::vector<double> lvl(nf);
    for (size_t f = 0; f < nf; ++f) {
        double e = 0; for (size_t i = f * F; i < (f + 1) * F; ++i) e += static_cast<double>(x[i]) * x[i];
        lvl[f] = 10 * std::log10(e / F + 1e-12);
    }
    std::vector<double> sorted;
    for (double v : lvl) if (v > -80.0) sorted.push_back(v);
    if (sorted.size() < 20) return;
    std::sort(sorted.begin(), sorted.end());
    const double speech = sorted[static_cast<size_t>(0.9 * (sorted.size() - 1))];
    const double thr = speech - belowSpeechDb, knee = 6.0;
    const long HOLD = 8, AHEAD = 3;                       // frames: 80 ms after, 30 ms before
    std::vector<double> want(nf, 0.0);
    for (size_t f = 0; f < nf; ++f) {
        double m = -200;
        for (long k = static_cast<long>(f) - HOLD; k <= static_cast<long>(f) + AHEAD; ++k)
            if (k >= 0 && k < static_cast<long>(nf)) m = std::max(m, lvl[k]);
        want[f] = depthDb * std::min(1.0, std::max(0.0, (thr - m) / knee));
    }
    const double aOpen = std::exp(-1.0 / (0.004 * sr)), aClose = std::exp(-1.0 / (0.100 * sr));
    double g = 0.0;                                       // current reduction in dB
    for (size_t i = 0; i < n; ++i) {
        const double t = want[std::min(nf - 1, i / F)];
        g = t < g ? aOpen * g + (1 - aOpen) * t : aClose * g + (1 - aClose) * t;
        x[i] *= static_cast<float>(std::pow(10.0, -g / 20));
    }
}

// Look-ahead limiter (offline): g[i] = mean of window-minima, which can never exceed the gain needed at i.
// Streaming form with O(window) memory: need[] lives in a ring buffer, the window minimum in a monotonic deque and
// the box mean in a running sum.
void limit(float *x, size_t n, int sr, double ceilingDb) {
    if (n == 0) return;
    const double ceil = std::pow(10.0, ceilingDb / 20);
    const size_t L = static_cast<size_t>(std::max(1, static_cast<int>(0.008 * sr)));
    const size_t W = L - 1;
    size_t R = 1;                                         // ring for need[] (indices i-W .. i+W), power of two
    while (R < 2 * L + 2) R <<= 1;
    const size_t M = R - 1;
    std::vector<float> need(R), mnRing(L);
    size_t ringPos = 0;                                   // i % L without a division
    auto needAt = [&](size_t j) {                         // computed from x[j] before x[j] is changed (j > i)
        const double a = std::fabs(x[j]);
        return a > ceil ? static_cast<float>(ceil / a) : 1.f;
    };
    std::deque<size_t> dq;
    size_t right = 0;
    double sum = 0.0;
    for (size_t i = 0; i < n; ++i) {
        const size_t hiIdx = std::min(n - 1, i + W);
        while (right <= hiIdx) {
            const float v = needAt(right);
            need[right & M] = v;
            while (!dq.empty() && need[dq.back() & M] >= v) dq.pop_back();
            dq.push_back(right++);
        }
        while (!dq.empty() && dq.front() + W < i) dq.pop_front();
        const float mn = need[dq.front() & M];
        // causal box mean of length L over the window minima
        sum += mn;
        if (i >= L) sum -= mnRing[ringPos];
        mnRing[ringPos] = mn;
        if (++ringPos == L) ringPos = 0;
        const size_t cnt = std::min(i + 1, L);
        const double g = sum / static_cast<double>(cnt);
        // before the first samples the average is over fewer terms; clamp to the exact need for safety
        x[i] = static_cast<float>(x[i] * std::min(g, static_cast<double>(need[i & M])));
    }
}
}  // namespace

float measureLufs(const float *x, size_t n, int sr) {
    if (n == 0) return -70.f;
    // K-weighting (BS.1770-4 coefficients are specified for 48 kHz)
    Biquad s1, s2;
    s1.b0 = 1.53512485958697; s1.b1 = -2.69169618940638; s1.b2 = 1.19839281085285; s1.a1 = -1.69065929318241; s1.a2 = 0.73248077421585;
    s2.b0 = 1.0; s2.b1 = -2.0; s2.b2 = 1.0; s2.a1 = -1.99004745483398; s2.a2 = 0.99007225036621;
    (void)sr;
    // Sum of squares per 100 ms step; a 400 ms block is 4 consecutive steps (no clip-sized buffers).
    const size_t block = static_cast<size_t>(0.4 * 48000), step = static_cast<size_t>(0.1 * 48000);
    std::vector<double> steps;
    steps.reserve(n / step + 1);
    double total = 0, cur = 0;
    size_t inStep = 0;
    for (size_t i = 0; i < n; ++i) {
        const double y = s2.process(s1.process(x[i]));
        const double sq = y * y;
        total += sq; cur += sq;
        if (++inStep == step) { steps.push_back(cur); cur = 0; inStep = 0; }
    }
    std::vector<double> blocks;
    const size_t per = block / step;                      // 4
    if (n < block) blocks.push_back(total / n);
    else for (size_t k = 0; k + per <= steps.size(); ++k) {
        double b = 0; for (size_t j = 0; j < per; ++j) b += steps[k + j];
        blocks.push_back(b / block);
    }
    auto lufs = [](double ms) { return -0.691 + 10 * std::log10(ms + 1e-14); };
    std::vector<double> gated;
    for (double b : blocks) if (lufs(b) > -70.0) gated.push_back(b);
    if (gated.empty()) return -70.f;
    double mean = 0; for (double b : gated) mean += b; mean /= gated.size();
    const double rel = lufs(mean) - 10.0;
    double acc = 0; size_t cnt = 0;
    for (double b : gated) if (lufs(b) > rel) { acc += b; ++cnt; }
    return cnt ? static_cast<float>(lufs(acc / cnt)) : static_cast<float>(lufs(mean));
}

PolishReport polish(float *x, size_t n, const PolishParams &p) {
    PolishReport rep;
    if (n == 0) return rep;
    const int sr = p.sampleRate;
    rep.inputLufs = measureLufs(x, n, sr);
    if (p.preset != kPolishOff) {
        Biquad hp = highpass(p.preset == kPolishPodcast ? 80 : 70, 0.707, sr);
        if (p.preset == kPolishPodcast) {
            // One pass through the whole EQ cascade (same per-sample order as five separate passes).
            Biquad ls = lowShelf(150, 2.0, sr), pk1 = peaking(320, 1.0, -1.5, sr), pk2 = peaking(3200, 0.9, 2.2, sr), hs = highShelf(9500, 1.8, sr);
            for (size_t i = 0; i < n; ++i) x[i] = hs.process(pk2.process(pk1.process(ls.process(hp.process(x[i])))));
            deEss(x, n, sr);
            compress(x, n, sr, -26.0, 2.8, 6.0, 120.0, 8.0);
        } else {
            hp.run(x, n);
            compress(x, n, sr, -28.0, 1.6, 10.0, 150.0, 8.0);
        }
        expandPauses(x, n, sr, envOr("AURA_GATE_DEPTH", kGateDepthDb), envOr("AURA_GATE_BELOW", kGateBelowDb));
    }
    if (p.normalize) {
        const float cur = measureLufs(x, n, sr);
        if (cur > -69.f) {
            float gDb = p.targetLufs - cur;
            gDb = std::max(-12.f, std::min(24.f, gDb));
            const float g = std::pow(10.f, gDb / 20.f);
            for (size_t i = 0; i < n; ++i) x[i] *= g;
            rep.gainDb = gDb;
        }
    }
    limit(x, n, sr, p.ceilingDb);
    float peak = 0.f;
    for (size_t i = 0; i < n; ++i) { x[i] = std::max(-1.f, std::min(1.f, x[i])); peak = std::max(peak, std::fabs(x[i])); }
    rep.peakDb = 20.f * std::log10(peak + 1e-9f);
    rep.outputLufs = measureLufs(x, n, sr);
    return rep;
}

}  // namespace aura
