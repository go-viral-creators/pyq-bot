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

#ifdef __cplusplus
}
#endif
