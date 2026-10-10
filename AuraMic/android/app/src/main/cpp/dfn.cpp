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
      synthMem_(kDfnHop, 0.f), anaMem_(kDfnHop, 0.f) {
    for (int i = 0; i < kDfnFft; ++i) {
        const double s = std::sin(0.5 * M_PI * (i + 0.5) / (kDfnFft / 2));
        window_[i] = static_cast<float>(std::sin(0.5 * M_PI * s * s));
    }
}

void DfnEngine::analysis(const float *hop, Cpx *spec) {
    std::vector<Cpx> buf(kDfnFft), out(kDfnFft);
    for (int i = 0; i < kDfnHop; ++i) buf[i] = {anaMem_[i] * window_[i], 0.f};
    for (int i = 0; i < kDfnHop; ++i) buf[kDfnHop + i] = {hop[i] * window_[kDfnHop + i], 0.f};
    std::memcpy(anaMem_.data(), hop, sizeof(float) * kDfnHop);
    fft_.forward(buf.data(), out.data());
    const float norm = 1.f / (static_cast<float>(kDfnFft) * kDfnFft / (2 * kDfnHop));  // libDF wnorm
    for (int k = 0; k < kDfnBins; ++k) spec[k] = {out[k].r * norm, out[k].i * norm};
}

void DfnEngine::synthesis(const Cpx *spec, float *out) {
    std::vector<Cpx> full(kDfnFft), t(kDfnFft);
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
    spec_.assign(static_cast<size_t>(T_) * kDfnBins * 2, 0.f);
    featErb_.assign(static_cast<size_t>(T_) * kDfnErb, 0.f);
    featSpec_.assign(static_cast<size_t>(2) * T_ * kDfnDf, 0.f);
    outPcm_.assign(static_cast<size_t>(T_) * kDfnHop, 0.f);
    std::fill(anaMem_.begin(), anaMem_.end(), 0.f);
    std::fill(synthMem_.begin(), synthMem_.end(), 0.f);
    nextFrame_ = 0;

    // libDF init_norm_states: linspace(-60,-90) for ERB mean norm, linspace(1e-3,1e-4) for unit norm
    std::vector<float> meanState(kDfnErb), unitState(kDfnDf);
    for (int i = 0; i < kDfnErb; ++i) meanState[i] = -60.f + i * (-90.f + 60.f) / (kDfnErb - 1);
    for (int i = 0; i < kDfnDf; ++i) unitState[i] = 0.001f + i * (0.0001f - 0.001f) / (kDfnDf - 1);

    std::vector<float> hop(kDfnHop);
    for (int t = 0; t < T_; ++t) {
        const size_t base = static_cast<size_t>(t) * kDfnHop;
        for (int i = 0; i < kDfnHop; ++i) hop[i] = (base + i < n) ? pcm[base + i] : 0.f;
        Cpx *spec = reinterpret_cast<Cpx *>(&spec_[static_cast<size_t>(t) * kDfnBins * 2]);
        analysis(hop.data(), spec);

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
    std::vector<Cpx> y(kDfnBins), frameOut(kDfnHop);
    for (int i = 0; i < count; ++i) {
        const int o = start + i;
        if (o != nextFrame_ || o >= T_) continue;  // frames must arrive in order
        const Cpx *noisy = reinterpret_cast<const Cpx *>(&spec_[static_cast<size_t>(o) * kDfnBins * 2]);
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
            for (int f = 0; f < kDfnDf; ++f) {
                float ar = 0.f, ai = 0.f;
                for (int k = 0; k < kDfnOrder; ++k) {
                    const int t = o - kDfnLookahead + k;
                    if (t < 0 || t >= T_) continue;
                    const Cpx *x = reinterpret_cast<const Cpx *>(&spec_[static_cast<size_t>(t) * kDfnBins * 2]);
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
        synthesis(y.data(), &outPcm_[static_cast<size_t>(o) * kDfnHop]);
        ++nextFrame_;
    }
}

void DfnEngine::output(float *out) const {
    for (size_t i = 0; i < n_; ++i) {
        const size_t idx = kDfnHop + i;  // one hop of analysis/synthesis delay
        out[i] = idx < outPcm_.size() ? outPcm_[idx] : 0.f;
    }
}

}  // namespace aura
