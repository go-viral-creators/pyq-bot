# AuraMic tools

| file | purpose |
|---|---|
| `gen_strings.py` | single source of truth for all UI copy; generates `res/values*/strings.xml` for 7 languages |
| `aura_synth.py` | synthetic speech / noise generators + ctypes wrapper around the native DSP |
| `test_dsp.py` | quality/regression test for the C++ noise suppressor (host build) |
| `train_mask_model.py` | trains and exports `app/src/main/assets/aura_mask.tflite` |

## Host build of the native engine

```bash
cmake -S android/app/src/main/cpp -B /tmp/aura-host -DCMAKE_BUILD_TYPE=Release
cmake --build /tmp/aura-host
python tools/test_dsp.py /tmp/aura-host/libauramic_host.so
```

## Retrain / replace the TFLite model

```bash
pip install tensorflow numpy scipy
python tools/train_mask_model.py /tmp/aura-host/libauramic_host.so android/app/src/main/assets/aura_mask.tflite
```

Model contract (see `MaskModel.kt`): input `float32 [N, 320]` (5 frames x 64 features), output
`float32 [N, 32]` in (0,1). The shipped model is trained on **synthetic** speech/noise only;
the native DSP is the primary denoiser and the model is blended in with exponent 0.5.
Replace it with a model trained on real speech (same contract) for best quality.
