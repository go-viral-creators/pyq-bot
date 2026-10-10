#include "polish.h"

#include <algorithm>
#include <cmath>
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
    Biquad hp = highpass(5200, 0.707, sr);
    std::vector<float> sib(x, x + n);
    hp.run(sib.data(), n);
    const double a = std::exp(-1.0 / (0.004 * sr)), r = std::exp(-1.0 / (0.05 * sr));
    double envS = 1e-9, envB = 1e-9, g = 1.0;
    for (size_t i = 0; i < n; ++i) {
        const double s2 = static_cast<double>(sib[i]) * sib[i], b2 = static_cast<double>(x[i]) * x[i];
        envS = s2 > envS ? a * envS + (1 - a) * s2 : r * envS + (1 - r) * s2;
        envB = b2 > envB ? a * envB + (1 - a) * b2 : r * envB + (1 - r) * b2;
        const double ratio = envS / (envB + 1e-12);           // share of energy above 5 kHz
        const double target = ratio > 0.45 ? std::pow(0.45 / ratio, 0.6) : 1.0;  // gentle, max ~ -9 dB
        g = target < g ? 0.5 * g + 0.5 * target : 0.995 * g + 0.005 * target;
        x[i] = static_cast<float>(x[i] - (1.0 - g) * sib[i]);
    }
}

// Downward compressor (soft knee, RMS detector). No make-up gain: loudness is set afterwards.
void compress(float *x, size_t n, int sr, double thrDb, double ratio, double atkMs, double relMs, double kneeDb) {
    const double det = std::exp(-1.0 / (0.010 * sr));
    const double aA = std::exp(-1.0 / (atkMs * 1e-3 * sr)), aR = std::exp(-1.0 / (relMs * 1e-3 * sr));
    double ms = 1e-10, grDb = 0.0;
    for (size_t i = 0; i < n; ++i) {
        ms = det * ms + (1 - det) * static_cast<double>(x[i]) * x[i];
        const double lvl = 10 * std::log10(ms + 1e-12);
        const double over = lvl - thrDb;
        double want = 0;
        if (2 * over < -kneeDb) want = 0;
        else if (2 * std::fabs(over) <= kneeDb) want = (1 / ratio - 1) * std::pow(over + kneeDb / 2, 2) / (2 * kneeDb);
        else want = (1 / ratio - 1) * over;
        want = -want;                                   // positive = reduction in dB
        grDb = want > grDb ? aA * grDb + (1 - aA) * want : aR * grDb + (1 - aR) * want;
        x[i] *= static_cast<float>(std::pow(10.0, -grDb / 20));
    }
}


// Linear-interpolated percentile of an already sorted vector (same convention as numpy.percentile).
double percentileSorted(const std::vector<double> &v, double q) {
    if (v.empty()) return 0.0;
    const double pos = (v.size() - 1) * q / 100.0;
    const size_t lo = static_cast<size_t>(std::floor(pos)), hi = std::min(v.size() - 1, lo + 1);
    return v[lo] + (v[hi] - v[lo]) * (pos - lo);
}

// 10 ms frame levels in dB.
std::vector<double> frameDb(const float *x, size_t n, size_t hop) {
    std::vector<double> e(n / hop);
    for (size_t f = 0; f < e.size(); ++f) {
        double s = 0; for (size_t i = f * hop; i < (f + 1) * hop; ++i) s += static_cast<double>(x[i]) * x[i];
        e[f] = 10 * std::log10(s / hop + 1e-12);
    }
    return e;
}

// Per-frame dB values -> per-sample dB (edge padded), then a one-pole smoother; returns linear gain.
void applyFrameGain(float *x, size_t n, size_t hop, const std::vector<double> &gDb, double pole, double sign) {
    double y = 0.0;
    for (size_t i = 0; i < n; ++i) {
        const size_t f = std::min(gDb.size() - 1, i / hop);
        y = pole * y + (1 - pole) * gDb[f];
        x[i] = static_cast<float>(x[i] * std::pow(10.0, sign * y / 20.0));
    }
}

// Speech-gated slow leveler. Unlike a compressor it only follows the level of *speech* frames and freezes in pauses
// and tails, so it evens out loud/quiet words without lifting room reverb and noise relative to the voice.
void leveler(float *x, size_t n, int sr, double targetDb = -23.0, double maxUp = 12.0, double maxDown = -8.0, double tcSec = 0.35) {
    const size_t hop = static_cast<size_t>(sr / 100);
    std::vector<double> e = frameDb(x, n, hop);
    if (e.size() < 4) return;
    std::vector<double> sorted(e); std::sort(sorted.begin(), sorted.end());
    const double thr = percentileSorted(sorted, 92.0) - 24.0;
    const double a = std::exp(-0.01 / tcSec);
    std::vector<double> g(e.size());
    double cur = 0.0, gcur = 0.0; bool have = false;
    for (size_t i = 0; i < e.size(); ++i) {
        if (e[i] > thr) {
            cur = have ? a * cur + (1 - a) * e[i] : e[i];
            const double gt = std::max(maxDown, std::min(maxUp, targetDb - cur));
            gcur = have ? a * gcur + (1 - a) * gt : gt;
            have = true;
        }
        g[i] = gcur;
    }
    applyFrameGain(x, n, hop, g, 0.995, +1.0);
}

// Gentle downward expander for the quiet tail / room-noise region (thresholds are relative to the speech level).
void expander(float *x, size_t n, int sr, double belowDb = 22.0, double ratio = 1.6, double maxCutDb = 14.0) {
    const size_t hop = static_cast<size_t>(sr / 100);
    std::vector<double> e = frameDb(x, n, hop);
    if (e.size() < 4) return;
    std::vector<double> sorted(e); std::sort(sorted.begin(), sorted.end());
    const double thr = percentileSorted(sorted, 92.0) - belowDb;
    std::vector<double> c(e.size());
    double cur = 0.0;
    for (size_t i = 0; i < e.size(); ++i) {
        const double v = std::min(std::max(thr - e[i], 0.0) * (ratio - 1.0), maxCutDb);
        cur = v > cur ? v : 0.7 * cur + 0.3 * v;
        c[i] = cur;
    }
    applyFrameGain(x, n, hop, c, 0.99, -1.0);
}

// Look-ahead limiter (offline): g[i] = mean of window-minima, which can never exceed the gain needed at i.
void limit(float *x, size_t n, int sr, double ceilingDb) {
    if (n == 0) return;
    const double ceil = std::pow(10.0, ceilingDb / 20);
    const int L = std::max(1, static_cast<int>(0.008 * sr));
    std::vector<float> need(n);
    for (size_t i = 0; i < n; ++i) {
        const double a = std::fabs(x[i]);
        need[i] = a > ceil ? static_cast<float>(ceil / a) : 1.f;
    }
    // sliding minimum over [i-L+1, i+L-1]
    std::vector<float> mn(n);
    std::deque<size_t> dq;
    const long W = L - 1;
    size_t right = 0;
    for (size_t i = 0; i < n; ++i) {
        const size_t hiIdx = std::min(n - 1, i + static_cast<size_t>(W));
        while (right <= hiIdx) {
            while (!dq.empty() && need[dq.back()] >= need[right]) dq.pop_back();
            dq.push_back(right++);
        }
        const long lo = static_cast<long>(i) - W;
        while (!dq.empty() && static_cast<long>(dq.front()) < lo) dq.pop_front();
        mn[i] = need[dq.front()];
    }
    // causal box mean of length L
    std::vector<double> pre(n + 1, 0.0);
    for (size_t i = 0; i < n; ++i) pre[i + 1] = pre[i] + mn[i];
    for (size_t i = 0; i < n; ++i) {
        const size_t lo = i + 1 >= static_cast<size_t>(L) ? i + 1 - L : 0;
        const double g = (pre[i + 1] - pre[lo]) / static_cast<double>(i + 1 - lo);
        // before the first samples the average is over fewer terms; clamp to the exact need for safety
        x[i] = static_cast<float>(x[i] * std::min(g, static_cast<double>(need[i])));
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
    std::vector<double> sq(n);
    for (size_t i = 0; i < n; ++i) { const double y = s2.process(s1.process(x[i])); sq[i] = y * y; }
    std::vector<double> pre(n + 1, 0.0);
    for (size_t i = 0; i < n; ++i) pre[i + 1] = pre[i] + sq[i];
    const size_t block = static_cast<size_t>(0.4 * 48000), step = static_cast<size_t>(0.1 * 48000);
    std::vector<double> blocks;
    if (n < block) blocks.push_back(pre[n] / n);
    else for (size_t s = 0; s + block <= n; s += step) blocks.push_back((pre[s + block] - pre[s]) / block);
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
        hp.run(x, n);
        if (p.preset == kPolishPodcast) {
            peaking(320, 1.0, -1.5, sr).run(x, n);
            peaking(3200, 0.9, 2.2, sr).run(x, n);
            highShelf(9500, 1.8, sr).run(x, n);
            deEss(x, n, sr);
            leveler(x, n, sr);   // replaces the old compressor: it lifted room reverb after every word
            expander(x, n, sr);
        } else {
            compress(x, n, sr, -28.0, 1.6, 10.0, 150.0, 8.0);
        }
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
