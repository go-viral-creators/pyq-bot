// JNI bridge for com.kaafisir.auramic.audio.NativeDenoiser
#include <jni.h>

#include "aura_capi.h"

#define JNI_FN(name) Java_com_kaafisir_auramic_audio_NativeDenoiser_##name

extern "C" {

JNIEXPORT jlong JNICALL JNI_FN(nativeCreate)(JNIEnv *, jobject, jint sampleRate) {
    return reinterpret_cast<jlong>(aura_create(sampleRate));
}

JNIEXPORT void JNICALL JNI_FN(nativeDestroy)(JNIEnv *, jobject, jlong handle) {
    aura_destroy(reinterpret_cast<void *>(handle));
}

JNIEXPORT jint JNICALL JNI_FN(nativeFrameCount)(JNIEnv *, jobject, jint samples) {
    return aura_frame_count(static_cast<size_t>(samples));
}

JNIEXPORT jint JNICALL JNI_FN(nativeBands)(JNIEnv *, jobject) { return aura_bands(); }

JNIEXPORT jint JNICALL JNI_FN(nativeFeatureSize)(JNIEnv *, jobject) {
    return aura_features_per_frame();
}

JNIEXPORT jint JNICALL JNI_FN(nativeExtractFeatures)(JNIEnv *env, jobject, jlong handle,
                                                     jshortArray pcm, jint length,
                                                     jfloatArray feat, jfloat strength) {
    jshort *in = env->GetShortArrayElements(pcm, nullptr);
    jfloat *out = env->GetFloatArrayElements(feat, nullptr);
    if (!in || !out) {
        if (in) env->ReleaseShortArrayElements(pcm, in, JNI_ABORT);
        if (out) env->ReleaseFloatArrayElements(feat, out, JNI_ABORT);
        return -1;
    }
    const int frames = aura_extract_features(reinterpret_cast<void *>(handle), in,
                                             static_cast<size_t>(length), out, strength);
    env->ReleaseShortArrayElements(pcm, in, JNI_ABORT);
    env->ReleaseFloatArrayElements(feat, out, 0);
    return frames;
}

JNIEXPORT jint JNICALL JNI_FN(nativeProcess)(JNIEnv *env, jobject, jlong handle, jshortArray pcm,
                                             jint length, jshortArray output, jfloat strength,
                                             jfloatArray mask, jfloat maskWeight,
                                             jboolean normalize) {
    jshort *in = env->GetShortArrayElements(pcm, nullptr);
    jshort *out = env->GetShortArrayElements(output, nullptr);
    jfloat *m = mask ? env->GetFloatArrayElements(mask, nullptr) : nullptr;
    if (!in || !out || (mask && !m)) {
        if (in) env->ReleaseShortArrayElements(pcm, in, JNI_ABORT);
        if (out) env->ReleaseShortArrayElements(output, out, JNI_ABORT);
        if (m) env->ReleaseFloatArrayElements(mask, m, JNI_ABORT);
        return -1;
    }
    aura_process(reinterpret_cast<void *>(handle), in, static_cast<size_t>(length), out, strength,
                 m, maskWeight, normalize ? 1 : 0);
    env->ReleaseShortArrayElements(pcm, in, JNI_ABORT);
    env->ReleaseShortArrayElements(output, out, 0);
    if (m) env->ReleaseFloatArrayElements(mask, m, JNI_ABORT);
    return 0;
}

}  // extern "C"
