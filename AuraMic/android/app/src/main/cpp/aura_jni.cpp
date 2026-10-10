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

// ---- DeepFilterNet3 engine: com.kaafisir.auramic.audio.NativeDfn ------------------------------------------
#define DFN_FN(name) Java_com_kaafisir_auramic_audio_NativeDfn_##name

extern "C" {

JNIEXPORT jlong JNICALL DFN_FN(nativeCreate)(JNIEnv *, jobject) { return reinterpret_cast<jlong>(dfn_create()); }
JNIEXPORT void JNICALL DFN_FN(nativeDestroy)(JNIEnv *, jobject, jlong h) { dfn_destroy(reinterpret_cast<void *>(h)); }

JNIEXPORT jint JNICALL DFN_FN(nativeLoad)(JNIEnv *env, jobject, jlong h, jfloatArray pcm, jint n) {
    jfloat *in = env->GetFloatArrayElements(pcm, nullptr);
    if (!in) return -1;
    const int t = dfn_load(reinterpret_cast<void *>(h), in, static_cast<size_t>(n));
    env->ReleaseFloatArrayElements(pcm, in, JNI_ABORT);
    return t;
}

JNIEXPORT void JNICALL DFN_FN(nativeFillFeatures)(JNIEnv *env, jobject, jlong h, jint start, jint count,
                                                  jfloatArray erb, jfloatArray spec) {
    jfloat *e = env->GetFloatArrayElements(erb, nullptr);
    jfloat *s = env->GetFloatArrayElements(spec, nullptr);
    if (e && s) dfn_fill_features(reinterpret_cast<void *>(h), start, count, e, s);
    if (e) env->ReleaseFloatArrayElements(erb, e, 0);
    if (s) env->ReleaseFloatArrayElements(spec, s, 0);
}

JNIEXPORT void JNICALL DFN_FN(nativeApply)(JNIEnv *env, jobject, jlong h, jint start, jint count, jfloatArray lsnr,
                                           jfloatArray mask, jfloatArray coefs, jfloat attenDb, jfloat minDb,
                                           jfloat maxErbDb, jfloat maxDfDb, jfloat pfBeta, jboolean useDf) {
    jfloat *l = env->GetFloatArrayElements(lsnr, nullptr);
    jfloat *m = env->GetFloatArrayElements(mask, nullptr);
    jfloat *c = coefs ? env->GetFloatArrayElements(coefs, nullptr) : nullptr;
    if (l && m) dfn_apply(reinterpret_cast<void *>(h), start, count, l, m, c, attenDb, minDb, maxErbDb, maxDfDb, pfBeta, useDf ? 1 : 0);
    if (l) env->ReleaseFloatArrayElements(lsnr, l, JNI_ABORT);
    if (m) env->ReleaseFloatArrayElements(mask, m, JNI_ABORT);
    if (c) env->ReleaseFloatArrayElements(coefs, c, JNI_ABORT);
}

JNIEXPORT void JNICALL DFN_FN(nativeOutput)(JNIEnv *env, jobject, jlong h, jfloatArray out) {
    jfloat *o = env->GetFloatArrayElements(out, nullptr);
    if (o) dfn_output(reinterpret_cast<void *>(h), o);
    if (o) env->ReleaseFloatArrayElements(out, o, 0);
}

// ---- Mastering chain: com.kaafisir.auramic.audio.NativePolish ---------------------------------------------
JNIEXPORT void JNICALL Java_com_kaafisir_auramic_audio_NativePolish_nativeRun(
    JNIEnv *env, jobject, jfloatArray x, jint n, jint preset, jboolean normalize, jfloat targetLufs, jfloat ceilingDb,
    jfloatArray report) {
    jfloat *p = env->GetFloatArrayElements(x, nullptr);
    jfloat *r = env->GetFloatArrayElements(report, nullptr);
    if (p) polish_run(p, static_cast<size_t>(n), preset, normalize ? 1 : 0, targetLufs, ceilingDb, r);
    if (p) env->ReleaseFloatArrayElements(x, p, 0);
    if (r) env->ReleaseFloatArrayElements(report, r, 0);
}

JNIEXPORT jfloat JNICALL Java_com_kaafisir_auramic_audio_NativePolish_nativeLufs(JNIEnv *env, jobject, jfloatArray x,
                                                                                  jint n) {
    jfloat *p = env->GetFloatArrayElements(x, nullptr);
    const float v = p ? polish_lufs(p, static_cast<size_t>(n), 48000) : -70.f;
    if (p) env->ReleaseFloatArrayElements(x, p, JNI_ABORT);
    return v;
}

}  // extern "C"
