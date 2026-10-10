// "Podcast polish": the mastering chain applied after AI denoising.
//   high-pass -> preset EQ -> de-esser -> compressor -> LUFS normalisation (BS.1770-4) -> look-ahead limiter
// All processing is offline (whole clip) so the limiter can look ahead without added latency.
#pragma once
#include <cstddef>

namespace aura {

enum PolishPreset { kPolishOff = 0, kPolishNatural = 1, kPolishPodcast = 2 };

struct PolishParams {
    int preset = kPolishPodcast;
    bool normalize = true;
    float targetLufs = -16.f;
    float ceilingDb = -1.5f;  // sample-peak ceiling (leaves head-room for inter-sample peaks)
    int sampleRate = 48000;
};

struct PolishReport {
    float inputLufs = -70.f;
    float outputLufs = -70.f;
    float gainDb = 0.f;
    float peakDb = -120.f;
};

// In-place processing of `n` mono float samples in [-1, 1].
PolishReport polish(float *x, size_t n, const PolishParams &p);

// Integrated loudness (LUFS) with BS.1770-4 K-weighting and gating. -70 for silence.
float measureLufs(const float *x, size_t n, int sampleRate);

}  // namespace aura
