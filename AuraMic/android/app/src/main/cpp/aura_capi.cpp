#include "aura_capi.h"

#include "aura_dsp.h"

using aura::NoiseSuppressor;
using aura::Params;

extern "C" {

void *aura_create(int sample_rate) { return new NoiseSuppressor(sample_rate); }
void aura_destroy(void *h) { delete static_cast<NoiseSuppressor *>(h); }
int aura_frame_count(size_t n) { return NoiseSuppressor::frameCount(n); }
int aura_bands(void) { return aura::kBands; }
int aura_features_per_frame(void) { return aura::kFeat; }

int aura_extract_features(void *h, const int16_t *in, size_t n, float *feat, float strength) {
    Params p;
    p.strength = strength;
    return static_cast<NoiseSuppressor *>(h)->extractFeatures(in, n, feat, p);
}

void aura_process(void *h, const int16_t *in, size_t n, int16_t *out, float strength,
                  const float *band_mask, float mask_weight, int normalize) {
    Params p;
    p.strength = strength;
    p.maskWeight = mask_weight;
    p.normalize = normalize != 0;
    static_cast<NoiseSuppressor *>(h)->process(in, n, out, p, band_mask);
}

int aura_band_energies(void *h, const int16_t *in, size_t n, float *out) {
    return static_cast<NoiseSuppressor *>(h)->bandEnergies(in, n, out);
}

}  // extern "C"
