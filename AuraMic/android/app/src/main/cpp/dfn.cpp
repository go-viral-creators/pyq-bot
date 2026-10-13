#include "dfn.h"

#include <algorithm>
#include <cmath>
#include <cstring>

namespace aura {

namespace {
constexpr float kPi = 3.14159265358979323846f;
constexpr float kAlpha = 0.99f;  // exp(-hop/sr/tau) rounded the way libDF's calc_norm_alpha does (tau = 1 s)

float freq2erb(float hz) { return 9.265f * std::log1p(hz / (24.7f * 9.265f)); }
float erb2freq(float erb) { return 24.7f * 9.265f * (std::exp(erb / 9.265f) - 1.f); }

// libDF::erb_fb(sr, fft_size, nb_bands, min_nb_freqs)
std::vector<int> erbFb(int sr, int fft, int nb, int minFreqs) {
    const float nyq = sr / 2.f, width = static_cast<float>(sr) / fft;
    const float lo = freq2erb(0.f), hi = freq2erb(nyq);
    std::vector<int> erb(nb, 0);
    const float step = (hi - lo) / nb;
    int prev = 0, over = 0;
    for (int i = 1; i <= nb; ++i) {
        const float f = erb2freq(lo + i * step);
        const int fb = static_cast<int>(std::lround(f / width));
        int nbf = fb - prev - over;
        if (nbf < minFreqs) {
            over = minFreqs - nbf;
            nbf = minFreqs;
        } else {
            over = 0;
        }
        erb[i - 1] = nbf;
        prev = fb;
    }
    erb[nb - 1] += 1;
    int sum = 0;
    for (int v : erb) sum += v;
    const int tooLarge = sum - (fft / 2 + 1);
    if (tooLarge > 0) erb[nb - 1] -= tooLarge;
    return erb;
}
}  // namespace

DfnEngine::DfnEngine()
    : fft_(kDfnFft), window_(kDfnFft), erbWidths_(erbFb(kDfnSr, kDfnFft, kDfnErb, 2)),
      synthMem_(kDfnHop, 0.f), anaMem_(kDfnHop, 0.f), ring_(static_cast<size_t>(kRing) * kDfnBins * 2, 0.f),
      fftIn_(kDfnFft), fftOut_(kDfnFft) {
    std::fill(ringFrame_, ringFrame_ + kRing, -1);
    for (int i = 0; i < kDfnFft; ++i) {
        const double s = std::sin(0.5 * M_PI * (i + 0.5) / (kDfnFft / 2));
        window_[i] = static_cast<float>(std::sin(0.5 * M_PI * s * s));
    }
}

void DfnEngine::analysis(const float *hop, Cpx *spec) {
    for (int i = 0; i < kDfnHop; ++i) fftIn_[i] = {anaMem_[i] * window_[i], 0.f};
    for (int i = 0; i < kDfnHop; ++i) fftIn_[kDfnHop + i] = {hop[i] * window_[kDfnHop + i], 0.f};
    std::memcpy(anaMem_.data(), hop, sizeof(float) * kDfnHop);
    fft_.forward(fftIn_.data(), fftOut_.data());
    const float norm = 1.f / (static_cast<float>(kDfnFft) * kDfnFft / (2 * kDfnHop));  // libDF wnorm
    for (int k = 0; k < kDfnBins; ++k) spec[k] = {fftOut_[k].r * norm, fftOut_[k].i * norm};
}

void DfnEngine::spectrumOf(int t, Cpx *spec) {
    // Same arithmetic as analysis() with anaMem_ = hop t-1.
    const size_t base = static_cast<size_t>(t) * kDfnHop;
    for (int i = 0; i < kDfnHop; ++i) {
        const float prev = t > 0 ? pcm_[base - kDfnHop + i] : 0.f;
        fftIn_[i] = {prev * window_[i], 0.f};
        fftIn_[kDfnHop + i] = {pcm_[base + i] * window_[kDfnHop + i], 0.f};
    }
    fft_.forward(fftIn_.data(), fftOut_.data());
    const float norm = 1.f / (static_cast<float>(kDfnFft) * kDfnFft / (2 * kDfnHop));
    for (int k = 0; k < kDfnBins; ++k) spec[k] = {fftOut_[k].r * norm, fftOut_[k].i * norm};
}

const Cpx *DfnEngine::frame(int t) {
    const int slot = t % kRing;
    Cpx *spec = reinterpret_cast<Cpx *>(&ring_[static_cast<size_t>(slot) * kDfnBins * 2]);
    if (ringFrame_[slot] != t) { spectrumOf(t, spec); ringFrame_[slot] = t; }
    return spec;
}

void DfnEngine::synthesis(const Cpx *spec, float *out) {
    std::vector<Cpx> &full = fftIn_, &t = fftOut_;
    full[0] = {spec[0].r, 0.f};  // realfft ignores the imaginary part of DC / Nyquist
    for (int k = 1; k < kDfnFft / 2; ++k) {
        full[k] = spec[k];
        full[kDfnFft - k] = {spec[k].r, -spec[k].i};
    }
    full[kDfnFft / 2] = {spec[kDfnFft / 2].r, 0.f};
    fft_.inverse(full.data(), t.data());
    for (int i = 0; i < kDfnHop; ++i) out[i] = t[i].r * window_[i] + synthMem_[i];
    for (int i = 0; i < kDfnHop; ++i) synthMem_[i] = t[kDfnHop + i].r * window_[kDfnHop + i];
}

int DfnEngine::load(const float *pcm, size_t n) {
    n_ = n;
    T_ = frameCountFor(n);
    pcm_.assign(static_cast<size_t>(T_) * kDfnHop, 0.f);
    std::memcpy(pcm_.data(), pcm, sizeof(float) * n);
    featErb_.assign(static_cast<size_t>(T_) * kDfnErb, 0.f);
    featSpec_.assign(static_cast<size_t>(2) * T_ * kDfnDf, 0.f);
    std::fill(anaMem_.begin(), anaMem_.end(), 0.f);
    std::fill(ringFrame_, ringFrame_ + kRing, -1);
    std::vector<Cpx> specBuf(kDfnBins);
    std::fill(synthMem_.begin(), synthMem_.end(), 0.f);
    nextFrame_ = 0;

    // libDF init_norm_states: linspace(-60,-90) for ERB mean norm, linspace(1e-3,1e-4) for unit norm
    std::vector<float> meanState(kDfnErb), unitState(kDfnDf);
    for (int i = 0; i < kDfnErb; ++i) meanState[i] = -60.f + i * (-90.f + 60.f) / (kDfnErb - 1);
    for (int i = 0; i < kDfnDf; ++i) unitState[i] = 0.001f + i * (0.0001f - 0.001f) / (kDfnDf - 1);

    for (int t = 0; t < T_; ++t) {
        Cpx *spec = specBuf.data();
        analysis(&pcm_[static_cast<size_t>(t) * kDfnHop], spec);

        // ERB feature: band mean power -> dB -> running mean normalisation
        float *erb = &featErb_[static_cast<size_t>(t) * kDfnErb];
        int off = 0;
        for (int b = 0; b < kDfnErb; ++b) {
            const int w = erbWidths_[b];
            float acc = 0.f;
            for (int j = 0; j < w; ++j) acc += spec[off + j].r * spec[off + j].r + spec[off + j].i * spec[off + j].i;
            off += w;
            float v = std::log10(acc / w + 1e-10f) * 10.f;
            meanState[b] = v * (1.f - kAlpha) + meanState[b] * kAlpha;
            erb[b] = (v - meanState[b]) / 40.f;
        }
        // complex feature: first 96 bins, running unit norm
        for (int f = 0; f < kDfnDf; ++f) {
            const float mag = std::sqrt(spec[f].r * spec[f].r + spec[f].i * spec[f].i);
            unitState[f] = mag * (1.f - kAlpha) + unitState[f] * kAlpha;
            const float inv = 1.f / std::sqrt(unitState[f]);
            featSpec_[(0 * static_cast<size_t>(T_) + t) * kDfnDf + f] = spec[f].r * inv;
            featSpec_[(1 * static_cast<size_t>(T_) + t) * kDfnDf + f] = spec[f].i * inv;
        }
    }
    return T_;
}

void DfnEngine::fillFeatures(int start, int count, float *erb, float *spec) const {
    for (int i = 0; i < count; ++i) {
        const int t = start + i;
        const bool valid = t >= 0 && t < T_;
        for (int b = 0; b < kDfnErb; ++b) erb[static_cast<size_t>(i) * kDfnErb + b] = valid ? featErb_[static_cast<size_t>(t) * kDfnErb + b] : 0.f;
        for (int c = 0; c < 2; ++c)
            for (int f = 0; f < kDfnDf; ++f)
                spec[(static_cast<size_t>(c) * count + i) * kDfnDf + f] =
                    valid ? featSpec_[(static_cast<size_t>(c) * T_ + t) * kDfnDf + f] : 0.f;
    }
}

void DfnEngine::applyFrames(int start, int count, const float *lsnr, const float *mask, const float *coefs,
                            const DfnParams &p) {
    float lim = 0.f;
    if (p.attenLimDb < 100.f) lim = std::pow(10.f, -std::fabs(p.attenLimDb) / 20.f);
    std::vector<Cpx> y(kDfnBins);
    for (int i = 0; i < count; ++i) {
        const int o = start + i;
        if (o != nextFrame_ || o >= T_) continue;  // frames must arrive in order
        // Bring frames up to o+2 into the ring *before* hop o of pcm_ is overwritten by this frame's output.
        for (int t = std::max(0, o - kDfnLookahead); t <= std::min(T_ - 1, o + kDfnLookahead); ++t) frame(t);
        const Cpx *noisy = frame(o);
        std::memcpy(y.data(), noisy, sizeof(Cpx) * kDfnBins);

        const float l = lsnr[i];
        bool gain, zero, df;
        if (l < p.minDb)        { gain = false; zero = true;  df = false; }
        else if (l > p.maxErbDb) { gain = false; zero = false; df = false; }
        else if (l > p.maxDfDb)  { gain = true;  zero = false; df = false; }
        else                     { gain = true;  zero = false; df = true; }

        if (gain) {
            int off = 0;
            for (int b = 0; b < kDfnErb; ++b) {
                const float g = mask[static_cast<size_t>(i) * kDfnErb + b];
                for (int j = 0; j < erbWidths_[b]; ++j) { y[off + j].r *= g; y[off + j].i *= g; }
                off += erbWidths_[b];
            }
        } else if (zero) {
            std::memset(y.data(), 0, sizeof(Cpx) * kDfnBins);
        }
        if (df && p.useDf && coefs) {
            const float *c = coefs + static_cast<size_t>(i) * kDfnDf * kDfnOrder * 2;
            const Cpx *taps[kDfnOrder];
            for (int k = 0; k < kDfnOrder; ++k) {
                const int t = o - kDfnLookahead + k;
                taps[k] = (t < 0 || t >= T_) ? nullptr : frame(t);
            }
            for (int f = 0; f < kDfnDf; ++f) {
                float ar = 0.f, ai = 0.f;
                for (int k = 0; k < kDfnOrder; ++k) {
                    const Cpx *x = taps[k];
                    if (!x) continue;
                    const float cr = c[(f * kDfnOrder + k) * 2], ci = c[(f * kDfnOrder + k) * 2 + 1];
                    ar += x[f].r * cr - x[f].i * ci;
                    ai += x[f].r * ci + x[f].i * cr;
                }
                y[f] = {ar, ai};
            }
        }
        if (gain && p.postFilterBeta > 0.f) {  // libDF post_filter on groups of 4 bins
            const float beta = p.postFilterBeta, eps = 1e-12f;
            for (int k = 0; k + 3 < kDfnBins; k += 4) {
                for (int j = 0; j < 4; ++j) {
                    const float en = std::hypot(y[k + j].r, y[k + j].i), nn = std::hypot(noisy[k + j].r, noisy[k + j].i);
                    const float g = std::max(std::min(en / (nn + eps), 1.f), eps);
                    const float gs = g * std::sin(g * kPi / 2.f);
                    const float pf = ((beta + 1.f) * g / (1.f + beta * (g / gs) * (g / gs))) / g;
                    y[k + j].r *= pf; y[k + j].i *= pf;
                }
            }
        }
        if (lim > 0.f) {
            for (int k = 0; k < kDfnBins; ++k) {
                y[k].r = y[k].r * (1.f - lim) + noisy[k].r * lim;
                y[k].i = y[k].i * (1.f - lim) + noisy[k].i * lim;
            }
        }
        synthesis(y.data(), &pcm_[static_cast<size_t>(o) * kDfnHop]);
        ++nextFrame_;
    }
}

void DfnEngine::output(float *out) const {
    for (size_t i = 0; i < n_; ++i) {
        const size_t idx = kDfnHop + i;  // one hop of analysis/synthesis delay
        out[i] = idx < pcm_.size() ? pcm_[idx] : 0.f;
    }
}

}  // namespace aura
