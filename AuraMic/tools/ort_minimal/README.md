# Minimal ONNX Runtime for AuraMic

The app only needs ~35 operators (4 models: DeepFilterNet3 enc/erb_dec/df_dec + GTCRN), so ONNX Runtime is built
from source with just those operators (`--minimal_build`, no exceptions/RTTI/ML ops, MinSizeRel):
`libonnxruntime.so` goes from 25.8 MB to ~2.3 MB per ABI.

1. `tools/onnx_models/*.onnx` are the source models. Convert them to ORT format (ORT 1.24.3):
   `python -m onnxruntime.tools.convert_onnx_models_to_ort <dir> --optimization_style Fixed --enable_type_reduction`
   (this also writes `required_operators_and_types.config`, kept here).
2. Build: `build_android.sh arm64-v8a` / `armeabi-v7a` (needs the ORT v1.24.3 sources, the dependencies listed in
   `cmake/deps.txt` checked out and passed as `FETCHCONTENT_SOURCE_DIR_*`, and a host `protoc` 3.21.12).
3. Strip (`llvm-strip --strip-unneeded`) and put the result in `app/src/main/jniLibs/<abi>/libonnxruntime.so`; the
   official Java classes + JNI library (`onnxruntime-android` AAR) keep working because the C API is unchanged.
If you add or change a model, redo steps 1-3: an operator that is missing from the config fails at model load time.
