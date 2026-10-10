// AuraMic AI - native noise suppression core.
//
// A two-pass, offline (whole-clip) spectral noise suppressor:
//   * 1024-point sqrt-Hann STFT, 75% overlap (hop 256), sample-rate independent.
//   * Noise PSD tracked with an MCRA-style speech-presence-controlled recursive average,
//     seeded from the quietest frames of the clip (we have the whole clip, so we use it).
//   * Gain: decision-directed a-priori SNR + Ephraim-Malah MMSE log-spectral amplitude.
//   * Optional per-band mask coming from a TFLite model is blended in geometrically.
//   * Optional loudness normalisation + soft limiter for a "studio" level.
//
// The same code is compiled for the host (see tools/) so it can be unit-tested and used
// to generate training data for the TFLite refinement model.
#pragma once

#include <cstddef>
#include <cstdint>
#include <vector>

namespace aura {

constexpr int kFft = 1024;
constexpr int kHop = 256;
constexpr int kBins = kFft / 2 + 1;
constexpr int kBands = 32;
// Per-frame feature vector handed to the model: [kBands SNR features | kBands DSP gains]
constexpr int kFeat = kBands * 2;

class Fft {
public:
    Fft();
    // In-place complex FFT of size kFft.
    void forward(float *re, float *im) const;
    // In-place inverse FFT (includes the 1/N scale).
    void inverse(float *re, float *im) const;

private:
    std::vector<float> cos_, sin_;
    std::vector<int> rev_;
};

struct Params {
    float strength = 0.7f;      // 0..1, maps to the gain floor (-10 dB .. -32 dB)
    float maskWeight = 0.5f;    // 0..1, exponent of the model mask in the geometric blend
    bool normalize = true;      // loudness normalise + soft limit
    float targetRmsDb = -19.0f; // speech RMS target when normalising
};

class NoiseSuppressor {
public:
    explicit NoiseSuppressor(int sampleRate);

    // Number of STFT frames used for a clip of n samples.
    static int frameCount(size_t n);

    // Pass 1: runs the DSP gain estimator and writes kFeat floats per frame to `feat`
    // (must hold frameCount(n) * kFeat floats). Returns the frame count.
    int extractFeatures(const int16_t *in, size_t n, float *feat, const Params &p);

    // Pass 2: full processing. `bandMask` is either null or frameCount(n) * kBands floats
    // in (0,1]. `out` must hold n samples.
    void process(const int16_t *in, size_t n, int16_t *out, const Params &p,
                 const float *bandMask);

    // Debug helper (training tools): per-frame band energies (linear power, mean over bins).
    int bandEnergies(const int16_t *in, size_t n, float *out) const;

    int sampleRate() const { return sampleRate_; }

private:
    struct Core;  // per-run estimator state
    void estimateNoiseSeed(const int16_t *in, size_t n, std::vector<float> &seed) const;
    void normalizeAndLimit(int16_t *buf, size_t n, const Params &p) const;

    int sampleRate_;
    Fft fft_;
    std::vector<float> window_;
    std::vector<int> bandEdge_;  // kBands + 1 bin edges
};

}  // namespace aura
