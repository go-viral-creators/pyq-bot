# JNI: keep the native bridge (method names are resolved by the linker).
-keep class com.kaafisir.auramic.audio.NativeDenoiser { *; }
# LiteRT / TFLite uses reflection + JNI internally.
-keep class org.tensorflow.lite.** { *; }

-dontwarn org.tensorflow.lite.**

# Accessibility + overlay services are referenced from the manifest only.
-keep class com.kaafisir.auramic.service.** extends android.app.Service { *; }
-keep class com.kaafisir.auramic.service.accessibility.AuraAccessibilityService { *; }
