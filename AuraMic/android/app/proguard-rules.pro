# JNI: keep the native bridges (method names are resolved by the linker).
-keep class com.kaafisir.auramic.audio.NativeDenoiser { *; }
-keep class com.kaafisir.auramic.audio.NativeDfn { *; }
-keep class com.kaafisir.auramic.audio.NativePolish { *; }
# ONNX Runtime uses JNI + reflection internally.
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# Accessibility + overlay services are referenced from the manifest only.
-keep class com.kaafisir.auramic.service.** extends android.app.Service { *; }
-keep class com.kaafisir.auramic.service.accessibility.AuraAccessibilityService { *; }
