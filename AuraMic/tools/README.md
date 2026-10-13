# AuraMic tools

| file | purpose |
|---|---|
| `gen_strings.py` | single source of truth for all UI copy; generates `res/values*/strings.xml` for 7 languages |
| `aura_synth.py` | synthetic speech / noise generators + ctypes wrapper around the native DSP |
| `test_dsp.py` | quality/regression test for the C++ noise suppressor (host build) |
| `dfn_reference.py` | numpy + official `libdf` reference of the offline DeepFilterNet3 pipeline |
| `pro_grants.py` | admin tool for free-Pro codes: `genkey` (once), `issue --days N [--bind REQUEST_CODE]`, `revoke --id`, `list`; private key stays on your computer |
| `bench/RESULTS.md` | listening-test results (rounds 3-5) and the decisions taken from them |
| `dfn_native.py` / `test_dfn.py` | drives the C++ port + ONNX models exactly like the Kotlin code; verifies features, exactness, quality, LUFS |

## Host build of the native engine

```bash
cmake -S android/app/src/main/cpp -B /tmp/aura-host -DCMAKE_BUILD_TYPE=Release
cmake --build /tmp/aura-host
python tools/test_dsp.py /tmp/aura-host/libauramic_host.so
```

## DeepFilterNet3 verification
```bash
pip install onnxruntime onnx soundfile scipy numpy deepfilterlib
# models: https://github.com/Rikorose/DeepFilterNet/tree/main/models  (DeepFilterNet3_onnx.tar.gz)
python tools/test_dfn.py /tmp/aura-host/libauramic_host.so <model dir> noisy.wav clean.wav
cd android && ./gradlew :hosttest:test -Pauramic.hostlib=/tmp/aura-host -Pauramic.sample=noisy.wav
```
