#include "aura_capi.h"

#include "aura_dsp.h"
#include "dfn.h"
#include "polish.h"

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


void *dfn_create(void) { return new aura::DfnEngine(); }
void dfn_destroy(void *h) { delete static_cast<aura::DfnEngine *>(h); }
int dfn_load(void *h, const float *pcm, size_t n) { return static_cast<aura::DfnEngine *>(h)->load(pcm, n); }
void dfn_fill_features(void *h, int start, int count, float *erb, float *spec) {
    static_cast<aura::DfnEngine *>(h)->fillFeatures(start, count, erb, spec);
}
void dfn_apply(void *h, int start, int count, const float *lsnr, const float *mask, const float *coefs,
               float atten_lim_db, float min_db, float max_erb_db, float max_df_db, float post_filter_beta, int use_df) {
    aura::DfnParams p;
    p.attenLimDb = atten_lim_db; p.minDb = min_db; p.maxErbDb = max_erb_db; p.maxDfDb = max_df_db;
    p.postFilterBeta = post_filter_beta; p.useDf = use_df != 0;
    static_cast<aura::DfnEngine *>(h)->applyFrames(start, count, lsnr, mask, coefs, p);
}
void dfn_output(void *h, float *out) { static_cast<aura::DfnEngine *>(h)->output(out); }

void polish_run(float *x, size_t n, int preset, int normalize, float target_lufs, float ceiling_db, float *report) {
    aura::PolishParams p;
    p.preset = preset; p.normalize = normalize != 0; p.targetLufs = target_lufs; p.ceilingDb = ceiling_db;
    const aura::PolishReport r = aura::polish(x, n, p);
    if (report) { report[0] = r.inputLufs; report[1] = r.outputLufs; report[2] = r.gainDb; report[3] = r.peakDb; }
}
float polish_lufs(const float *x, size_t n, int sample_rate) { return aura::measureLufs(x, n, sample_rate); }

}  // extern "C"
