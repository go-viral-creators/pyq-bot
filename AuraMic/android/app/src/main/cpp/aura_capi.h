// Plain C API over aura::NoiseSuppressor. Used by the JNI layer on Android and by
// ctypes on the host (tools/train_mask_model.py, tools/test_dsp.py).
#pragma once
#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

void *aura_create(int sample_rate);
void aura_destroy(void *handle);
int aura_frame_count(size_t n_samples);
int aura_bands(void);
int aura_features_per_frame(void);
int aura_extract_features(void *handle, const int16_t *in, size_t n, float *feat, float strength);
// band_mask may be NULL.
void aura_process(void *handle, const int16_t *in, size_t n, int16_t *out, float strength,
                  const float *band_mask, float mask_weight, int normalize);
int aura_band_energies(void *handle, const int16_t *in, size_t n, float *out);

// ---- DeepFilterNet3 front/back end (network runs in ONNX Runtime on the Kotlin side) -----------------
void *dfn_create(void);
void dfn_destroy(void *h);
int dfn_load(void *h, const float *pcm, size_t n);  // returns frame count T
void dfn_fill_features(void *h, int start, int count, float *erb, float *spec);
void dfn_apply(void *h, int start, int count, const float *lsnr, const float *mask, const float *coefs,
               float atten_lim_db, float min_db, float max_erb_db, float max_df_db, float post_filter_beta, int use_df);
void dfn_output(void *h, float *out);

// ---- Mastering chain -------------------------------------------------------------------------------------
// report[4] = {input LUFS, output LUFS, gain dB, peak dB}
void polish_run(float *x, size_t n, int preset, int normalize, float target_lufs, float ceiling_db, float *report);
float polish_lufs(const float *x, size_t n, int sample_rate);

#ifdef __cplusplus
}
#endif
