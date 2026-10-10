// Small mixed-radix complex FFT (KISS-FFT style) for sizes with factors 2,3,4,5 - e.g. 960 = 4*4*4*3*5.
// DeepFilterNet3 uses a 960-point STFT, which the radix-2 FFT in aura_dsp.cpp cannot do.
#pragma once
#include <cmath>
#include <vector>

namespace aura {

struct Cpx { float r, i; };

class MixedFft {
public:
    explicit MixedFft(int n) : n_(n), tw_(n) {
        for (int k = 0; k < n; ++k) {
            const double a = -2.0 * M_PI * k / n;
            tw_[k] = {static_cast<float>(std::cos(a)), static_cast<float>(std::sin(a))};
        }
        int m = n, p = 4;
        while (m > 1) {
            while (m % p) {
                p = (p == 4) ? 2 : (p == 2 ? 3 : p + 2);
                if (p > m) p = m;
            }
            m /= p;
            factors_.push_back(p);
            factors_.push_back(m);
        }
    }

    int size() const { return n_; }

    // out[k] = sum_t in[t] * exp(-2 pi i t k / n)   (unnormalised)
    void forward(const Cpx *in, Cpx *out) const { work(out, in, 1, 1, factors_.data()); }

    // out[t] = sum_k in[k] * exp(+2 pi i t k / n)   (unnormalised)
    void inverse(const Cpx *in, Cpx *out) const {
        std::vector<Cpx> tmp(n_);
        for (int k = 0; k < n_; ++k) tmp[k] = {in[k].r, -in[k].i};
        forward(tmp.data(), out);
        for (int k = 0; k < n_; ++k) out[k].i = -out[k].i;
    }

private:
    void work(Cpx *fout, const Cpx *f, int fstride, int in_stride, const int *factors) const {
        Cpx *beg = fout;
        const int p = *factors++;
        const int m = *factors++;
        Cpx *end = fout + p * m;
        if (m == 1) {
            do { *fout = *f; f += fstride * in_stride; } while (++fout != end);
        } else {
            do { work(fout, f, fstride * p, in_stride, factors); f += fstride * in_stride; } while ((fout += m) != end);
        }
        fout = beg;
        butterfly(fout, fstride, m, p);
    }

    void butterfly(Cpx *fout, int fstride, int m, int p) const {
        Cpx scratch[8];
        for (int u = 0; u < m; ++u) {
            int k = u;
            for (int q1 = 0; q1 < p; ++q1) { scratch[q1] = fout[k]; k += m; }
            k = u;
            for (int q1 = 0; q1 < p; ++q1) {
                int twidx = 0;
                Cpx acc = scratch[0];
                for (int q = 1; q < p; ++q) {
                    twidx += fstride * k;
                    if (twidx >= n_) twidx -= n_;
                    const Cpx t = tw_[twidx];
                    acc.r += scratch[q].r * t.r - scratch[q].i * t.i;
                    acc.i += scratch[q].r * t.i + scratch[q].i * t.r;
                }
                fout[k] = acc;
                k += m;
            }
        }
    }

    int n_;
    std::vector<Cpx> tw_;
    std::vector<int> factors_;
};

}  // namespace aura
