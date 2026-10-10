#include "aura_dsp.h"

#include <algorithm>
#include <cmath>
#include <cstring>

namespace aura {

namespace {

constexpr float kPi = 3.14159265358979323846f;
constexpr float kEps = 1e-12f;

// Exponential integral E1(x), x > 0 (Abramowitz & Stegun 5.1.53 / 5.1.56).
inline float expint1(float x) {
    if (x < 1.0f) {
        return -std::log(x) +
               (-0.57721566f +
                x * (0.99999193f +
                     x * (-0.24991055f +
                          x * (0.05519968f + x * (-0.00976004f + x * 0.00107857f)))));
    }
    const float num = x * x + 2.334733f * x + 0.250621f;
    const float den = x * x + 3.330657f * x + 1.681534f;
    return (num / den) * std::exp(-x) / x;
}

inline float melOf(float hz) { return 2595.0f * std::log10(1.0f + hz / 700.0f); }
inline float hzOf(float mel) { return 700.0f * (std::pow(10.0f, mel / 2595.0f) - 1.0f); }

}  // namespace

// ---------------------------------------------------------------------------------------
// FFT
// ---------------------------------------------------------------------------------------
Fft::Fft() : cos_(kFft / 2), sin_(kFft / 2), rev_(kFft) {
    for (int i = 0; i < kFft / 2; ++i) {
        const double a = -2.0 * M_PI * i / kFft;
        cos_[i] = static_cast<float>(std::cos(a));
        sin_[i] = static_cast<float>(std::sin(a));
    }
    int bits = 0;
    while ((1 << bits) < kFft) ++bits;
    for (int i = 0; i < kFft; ++i) {
        int r = 0;
        for (int b = 0; b < bits; ++b)
            if (i & (1 << b)) r |= 1 << (bits - 1 - b);
        rev_[i] = r;
    }
}

void Fft::forward(float *re, float *im) const {
    for (int i = 0; i < kFft; ++i) {
        const int j = rev_[i];
        if (j > i) {
            std::swap(re[i], re[j]);
            std::swap(im[i], im[j]);
        }
    }
    for (int len = 2; len <= kFft; len <<= 1) {
        const int half = len >> 1;
        const int step = kFft / len;
        for (int i = 0; i < kFft; i += len) {
            for (int j = 0; j < half; ++j) {
                const float wr = cos_[j * step];
                const float wi = sin_[j * step];
                const int a = i + j;
                const int b = a + half;
                const float tr = re[b] * wr - im[b] * wi;
                const float ti = re[b] * wi + im[b] * wr;
                re[b] = re[a] - tr;
                im[b] = im[a] - ti;
                re[a] += tr;
                im[a] += ti;
            }
        }
    }
}

void Fft::inverse(float *re, float *im) const {
    // ifft(x) = swap(fft(swap(x))) / N
    forward(im, re);
    const float s = 1.0f / kFft;
    for (int i = 0; i < kFft; ++i) {
        re[i] *= s;
        im[i] *= s;
    }
}

// ---------------------------------------------------------------------------------------
// Per-run gain estimator
// ---------------------------------------------------------------------------------------
struct NoiseSuppressor::Core {
    Core(const std::vector<float> &seed, float strength, int sampleRate)
        : lambda(seed), ps(seed), pmin(seed), ptmp(seed), speechProb(kBins, 0.0f),
          gPrev(kBins, 0.5f), gammaPrev(kBins, 1.0f), gSmooth(kBins, 1.0f), delta(kBins),
          pf(kBins), gRaw(kBins), gTmp(kBins) {
        const float floorDb = 10.0f + 22.0f * std::min(1.0f, std::max(0.0f, strength));
        gMin = std::pow(10.0f, -floorDb / 20.0f);
        xiMin = gMin * gMin * 0.5f;
        // Minimum-tracking window ~1.2 s.
        window = std::max(32, static_cast<int>(1.2f * sampleRate / kHop));
        const float binHz = static_cast<float>(sampleRate) / kFft;
        for (int k = 0; k < kBins; ++k) {
            const float hz = k * binHz;
            delta[k] = hz < 1000.0f ? 3.0f : (hz < 3000.0f ? 4.0f : 5.0f);
        }
        lowCutBins = std::max(1, static_cast<int>(std::ceil(60.0f / binHz)));
    }

    // power: kBins linear power values. gain: kBins output (smoothed DSP gain).
    // gammaOut (optional): a-posteriori SNR per bin, for feature extraction.
    void step(const float *power, float *gain, float *gammaOut) {
        // 1. smoothed power & minimum statistics
        for (int k = 0; k < kBins; ++k) {
            const float l = power[k > 0 ? k - 1 : 0];
            const float r = power[k < kBins - 1 ? k + 1 : kBins - 1];
            pf[k] = 0.25f * l + 0.5f * power[k] + 0.25f * r;
            ps[k] = 0.8f * ps[k] + 0.2f * pf[k];
        }
        const bool reset = (frame % window) == 0;
        for (int k = 0; k < kBins; ++k) {
            if (reset) {
                pmin[k] = std::min(ptmp[k], ps[k]);
                ptmp[k] = ps[k];
            } else {
                pmin[k] = std::min(pmin[k], ps[k]);
                ptmp[k] = std::min(ptmp[k], ps[k]);
            }
        }
        // 2. speech presence & noise PSD update (MCRA)
        for (int k = 0; k < kBins; ++k) {
            const float ratio = ps[k] / std::max(pmin[k], kEps);
            const float indicator = ratio > delta[k] ? 1.0f : 0.0f;
            speechProb[k] = 0.2f * speechProb[k] + 0.8f * indicator;
            const float alpha = 0.95f + 0.05f * speechProb[k];
            lambda[k] = alpha * lambda[k] + (1.0f - alpha) * power[k];
            if (lambda[k] < kEps) lambda[k] = kEps;
        }
        // 3. decision-directed a-priori SNR and MMSE-LSA gain
        for (int k = 0; k < kBins; ++k) {
            const float gamma = std::min(power[k] / lambda[k], 1000.0f);
            float xi = 0.95f * gPrev[k] * gPrev[k] * gammaPrev[k] +
                       0.05f * std::max(gamma - 1.0f, 0.0f);
            xi = std::max(xi, xiMin);
            const float w = xi / (1.0f + xi);
            const float v = std::min(std::max(w * gamma, 1e-6f), 50.0f);
            float g = w * std::exp(0.5f * expint1(v));
            g = std::min(g, 1.0f);
            // OM-LSA: weight by the a-posteriori speech presence probability so that bins
            // that look like pure noise are pushed to the floor instead of "LSA residual".
            const float absentPrior = 0.7f - 0.4f * speechProb[k];
            const float pSpeech =
                1.0f / (1.0f + (absentPrior / (1.0f - absentPrior)) * (1.0f + xi) *
                                   std::exp(-std::min(v, 60.0f)));
            g = std::pow(g, pSpeech) * std::pow(gMin, 1.0f - pSpeech);
            gRaw[k] = g;
            gPrev[k] = g;
            gammaPrev[k] = gamma;
            if (gammaOut) gammaOut[k] = gamma;
        }
        // 4. temporal smoothing (fast attack, slower release) against musical noise
        for (int k = 0; k < kBins; ++k) {
            const float g = std::max(gRaw[k], gMin);
            gSmooth[k] = g > gSmooth[k] ? 0.4f * gSmooth[k] + 0.6f * g
                                        : 0.75f * gSmooth[k] + 0.25f * g;
        }
        // 5. light frequency smoothing + rumble cut
        for (int k = 0; k < kBins; ++k) {
            const float l = gSmooth[k > 0 ? k - 1 : 0];
            const float r = gSmooth[k < kBins - 1 ? k + 1 : kBins - 1];
            float g = 0.15f * l + 0.7f * gSmooth[k] + 0.15f * r;
            if (k < lowCutBins) g = std::min(g, gMin);
            gain[k] = std::max(g, gMin);
        }
        ++frame;
    }

    std::vector<float> lambda, ps, pmin, ptmp, speechProb, gPrev, gammaPrev, gSmooth, delta, pf,
        gRaw, gTmp;
    float gMin = 0.1f;
    float xiMin = 0.01f;
    int window = 200;
    int lowCutBins = 2;
    long frame = 0;
};

// ---------------------------------------------------------------------------------------
// NoiseSuppressor
// ---------------------------------------------------------------------------------------
NoiseSuppressor::NoiseSuppressor(int sampleRate)
    : sampleRate_(sampleRate), window_(kFft), bandEdge_(kBands + 1) {
    // periodic sqrt-Hann
    for (int i = 0; i < kFft; ++i) {
        const float hann = 0.5f - 0.5f * std::cos(2.0f * kPi * i / kFft);
        window_[i] = std::sqrt(hann);
    }
    // mel-spaced band edges (in bins), strictly increasing
    const float nyq = sampleRate * 0.5f;
    const float melMax = melOf(nyq);
    const float binHz = static_cast<float>(sampleRate) / kFft;
    bandEdge_[0] = 0;
    for (int b = 1; b < kBands; ++b) {
        int e = static_cast<int>(std::lround(hzOf(melMax * b / kBands) / binHz));
        e = std::max(e, bandEdge_[b - 1] + 1);
        bandEdge_[b] = std::min(e, kBins - (kBands - b));
    }
    bandEdge_[kBands] = kBins;
}

int NoiseSuppressor::frameCount(size_t n) {
    if (n == 0) return 0;
    return static_cast<int>((n - 1 + kFft - kHop) / kHop) + 1;
}

namespace {
// Copies the windowed frame f of the clip into buf (zero padded outside the clip).
inline void loadFrame(const int16_t *in, size_t n, int f, const std::vector<float> &win,
                      float *re, float *im) {
    const long start = static_cast<long>(f) * kHop - (kFft - kHop);
    for (int i = 0; i < kFft; ++i) {
        const long idx = start + i;
        const float s = (idx >= 0 && idx < static_cast<long>(n)) ? in[idx] * (1.0f / 32768.0f)
                                                                  : 0.0f;
        re[i] = s * win[i];
        im[i] = 0.0f;
    }
}
}  // namespace

void NoiseSuppressor::estimateNoiseSeed(const int16_t *in, size_t n,
                                        std::vector<float> &seed) const {
    const int frames = frameCount(n);
    seed.assign(kBins, 1e-9f);
    if (frames == 0) return;
    // Pass A: time-domain energy per frame (cheap).
    std::vector<float> energy(frames);
    for (int f = 0; f < frames; ++f) {
        const long start = static_cast<long>(f) * kHop - (kFft - kHop);
        double e = 0.0;
        for (int i = 0; i < kFft; ++i) {
            const long idx = start + i;
            if (idx >= 0 && idx < static_cast<long>(n)) {
                const double s = in[idx] * (1.0 / 32768.0) * window_[i];
                e += s * s;
            }
        }
        energy[f] = static_cast<float>(e);
    }
    // Only use frames fully inside the clip for the seed.
    const int firstFull = (kFft - kHop + kHop - 1) / kHop;  // frames starting at >= 0
    std::vector<float> sorted;
    for (int f = firstFull; f < frames; ++f) sorted.push_back(energy[f]);
    if (sorted.size() < 4) {  // tiny clip: use everything
        sorted = energy;
    }
    const size_t q = std::max<size_t>(1, sorted.size() / 5);  // quietest 20%
    std::nth_element(sorted.begin(), sorted.begin() + (q - 1), sorted.end());
    const float thr = sorted[q - 1];
    // Pass B: average power of those frames.
    std::vector<float> re(kFft), im(kFft);
    std::fill(seed.begin(), seed.end(), 0.0f);
    int used = 0;
    for (int f = 0; f < frames && used < 4000; ++f) {
        if (energy[f] > thr) continue;
        loadFrame(in, n, f, window_, re.data(), im.data());
        fft_.forward(re.data(), im.data());
        for (int k = 0; k < kBins; ++k) seed[k] += re[k] * re[k] + im[k] * im[k];
        ++used;
    }
    const float inv = used > 0 ? 0.9f / used : 0.0f;
    for (int k = 0; k < kBins; ++k) seed[k] = std::max(seed[k] * inv, 1e-9f);
}

int NoiseSuppressor::extractFeatures(const int16_t *in, size_t n, float *feat, const Params &p) {
    const int frames = frameCount(n);
    if (frames == 0) return 0;
    std::vector<float> seed;
    estimateNoiseSeed(in, n, seed);
    Core core(seed, p.strength, sampleRate_);
    std::vector<float> re(kFft), im(kFft), power(kBins), gain(kBins), gamma(kBins);
    for (int f = 0; f < frames; ++f) {
        loadFrame(in, n, f, window_, re.data(), im.data());
        fft_.forward(re.data(), im.data());
        for (int k = 0; k < kBins; ++k) power[k] = re[k] * re[k] + im[k] * im[k];
        core.step(power.data(), gain.data(), gamma.data());
        float *out = feat + static_cast<size_t>(f) * kFeat;
        for (int b = 0; b < kBands; ++b) {
            const int lo = bandEdge_[b], hi = bandEdge_[b + 1];
            float sg = 0.0f, sgain = 0.0f;
            for (int k = lo; k < hi; ++k) {
                sg += gamma[k];
                sgain += gain[k];
            }
            const float cnt = static_cast<float>(hi - lo);
            const float db = 10.0f * std::log10(sg / cnt + 1e-3f);
            out[b] = std::min(40.0f, std::max(-10.0f, db)) / 25.0f;
            out[kBands + b] = sgain / cnt;
        }
    }
    return frames;
}

void NoiseSuppressor::process(const int16_t *in, size_t n, int16_t *out, const Params &p,
                              const float *bandMask) {
    const int frames = frameCount(n);
    if (frames == 0) return;
    std::vector<float> seed;
    estimateNoiseSeed(in, n, seed);
    Core core(seed, p.strength, sampleRate_);
    const float gMin = core.gMin;
    const float w = std::min(1.0f, std::max(0.0f, p.maskWeight));

    // Per-bin interpolation tables for the band mask.
    std::vector<int> b0(kBins), b1(kBins);
    std::vector<float> tt(kBins);
    if (bandMask) {
        std::vector<float> centre(kBands);
        for (int b = 0; b < kBands; ++b)
            centre[b] = 0.5f * (bandEdge_[b] + bandEdge_[b + 1] - 1);
        int b = 0;
        for (int k = 0; k < kBins; ++k) {
            while (b < kBands - 1 && centre[b + 1] <= k) ++b;
            if (k <= centre[0]) {
                b0[k] = b1[k] = 0;
                tt[k] = 0;
            } else if (k >= centre[kBands - 1]) {
                b0[k] = b1[k] = kBands - 1;
                tt[k] = 0;
            } else {
                b0[k] = b;
                b1[k] = b + 1;
                tt[k] = (k - centre[b]) / (centre[b + 1] - centre[b]);
            }
        }
    }

    std::vector<float> acc(n, 0.0f);
    std::vector<float> re(kFft), im(kFft), power(kBins), gain(kBins);
    for (int f = 0; f < frames; ++f) {
        loadFrame(in, n, f, window_, re.data(), im.data());
        fft_.forward(re.data(), im.data());
        for (int k = 0; k < kBins; ++k) power[k] = re[k] * re[k] + im[k] * im[k];
        core.step(power.data(), gain.data(), nullptr);
        if (bandMask) {
            const float *m = bandMask + static_cast<size_t>(f) * kBands;
            for (int k = 0; k < kBins; ++k) {
                const float mk = m[b0[k]] * (1.0f - tt[k]) + m[b1[k]] * tt[k];
                const float blended =
                    std::pow(gain[k], 1.0f - w) * std::pow(std::max(mk, 1e-4f), w);
                gain[k] = std::max(blended, gMin);
            }
        }
        // Apply gain to the positive half and mirror it (real signal => hermitian spectrum).
        for (int k = 0; k < kBins; ++k) {
            re[k] *= gain[k];
            im[k] *= gain[k];
        }
        for (int k = 1; k < kFft / 2; ++k) {
            re[kFft - k] = re[k];
            im[kFft - k] = -im[k];
        }
        fft_.inverse(re.data(), im.data());
        const long start = static_cast<long>(f) * kHop - (kFft - kHop);
        for (int i = 0; i < kFft; ++i) {
            const long idx = start + i;
            if (idx >= 0 && idx < static_cast<long>(n)) acc[idx] += re[i] * window_[i];
        }
    }
    // sqrt-Hann analysis * sqrt-Hann synthesis at 75% overlap sums to 2.
    for (size_t i = 0; i < n; ++i) {
        const float v = std::max(-1.0f, std::min(1.0f, acc[i] * 0.5f));
        out[i] = static_cast<int16_t>(std::lrint(v * 32767.0f));
    }
    if (p.normalize) normalizeAndLimit(out, n, p);
}

int NoiseSuppressor::bandEnergies(const int16_t *in, size_t n, float *out) const {
    const int frames = frameCount(n);
    std::vector<float> re(kFft), im(kFft);
    for (int f = 0; f < frames; ++f) {
        loadFrame(in, n, f, window_, re.data(), im.data());
        fft_.forward(re.data(), im.data());
        for (int b = 0; b < kBands; ++b) {
            float s = 0.0f;
            for (int k = bandEdge_[b]; k < bandEdge_[b + 1]; ++k)
                s += re[k] * re[k] + im[k] * im[k];
            out[static_cast<size_t>(f) * kBands + b] = s / (bandEdge_[b + 1] - bandEdge_[b]);
        }
    }
    return frames;
}

void NoiseSuppressor::normalizeAndLimit(int16_t *buf, size_t n, const Params &p) const {
    const size_t block = std::max<size_t>(1, sampleRate_ / 50);  // 20 ms
    const size_t blocks = n / block;
    float gain = 1.0f;
    if (blocks >= 3) {
        std::vector<float> ms(blocks);
        for (size_t b = 0; b < blocks; ++b) {
            double e = 0.0;
            for (size_t i = 0; i < block; ++i) {
                const double s = buf[b * block + i] * (1.0 / 32768.0);
                e += s * s;
            }
            ms[b] = static_cast<float>(e / block);
        }
        std::vector<float> sorted = ms;
        const size_t idx95 = std::min(blocks - 1, static_cast<size_t>(blocks * 0.95));
        std::nth_element(sorted.begin(), sorted.begin() + idx95, sorted.end());
        const float ref = sorted[idx95];
        if (ref > 1e-8f) {  // louder than about -80 dBFS
            const float cutoff = ref * 0.003f;  // within 25 dB of the loud blocks
            double sum = 0.0;
            int cnt = 0;
            for (float m : ms)
                if (m >= cutoff) {
                    sum += m;
                    ++cnt;
                }
            const float level = static_cast<float>(sum / std::max(cnt, 1));
            const float levelDb = 10.0f * std::log10(level + 1e-12f);
            if (levelDb > -60.0f) {
                const float gDb = p.targetRmsDb - levelDb;
                gain = std::pow(10.0f, std::min(18.0f, std::max(-6.0f, gDb)) / 20.0f);
            }
        }
    }
    const size_t fade = std::min<size_t>(n / 2, sampleRate_ / 200);  // 5 ms
    for (size_t i = 0; i < n; ++i) {
        float x = buf[i] * (1.0f / 32768.0f) * gain;
        const float a = std::fabs(x);
        if (a > 0.7f) x = std::copysign(0.7f + 0.3f * std::tanh((a - 0.7f) / 0.3f), x);
        if (i < fade) x *= static_cast<float>(i) / fade;
        if (n - 1 - i < fade) x *= static_cast<float>(n - 1 - i) / fade;
        buf[i] = static_cast<int16_t>(std::lrint(std::max(-1.0f, std::min(1.0f, x)) * 32767.0f));
    }
}

}  // namespace aura
