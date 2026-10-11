package com.kaafisir.auramic.audio

/**
 * JNI face of the DeepFilterNet3 front/back end (STFT, ERB features, deep filtering, iSTFT) in
 * `src/main/cpp/dfn.cpp`. The neural network itself runs in ONNX Runtime ([DfnEnhancer]).
 * One instance = one clip. Not thread-safe.
 */
class NativeDfn : AutoCloseable {
    private var handle: Long = nativeCreate()

    /** Analyses [n] samples (48 kHz mono float) and returns the number of STFT frames. */
    fun load(pcm: FloatArray, n: Int): Int = nativeLoad(handle, pcm, n).also { check(it > 0) { "dfn load failed" } }

    /** Features for frames [start, start+count): erb[count*32], spec[2*count*96] (plane major). Past-the-end frames are zero. */
    fun fillFeatures(start: Int, count: Int, erb: FloatArray, spec: FloatArray) = nativeFillFeatures(handle, start, count, erb, spec)

    fun apply(
        start: Int, count: Int, lsnr: FloatArray, mask: FloatArray, coefs: FloatArray?,
        attenDb: Float, useDf: Boolean,
    ) = nativeApply(handle, start, count, lsnr, mask, coefs, attenDb, MIN_DB, MAX_ERB_DB, MAX_DF_DB, 0f, useDf)

    /** Enhanced signal, aligned with the input. */
    fun output(out: FloatArray) = nativeOutput(handle, out)

    override fun close() {
        if (handle != 0L) { nativeDestroy(handle); handle = 0L }
    }

    private external fun nativeCreate(): Long
    private external fun nativeDestroy(h: Long)
    private external fun nativeLoad(h: Long, pcm: FloatArray, n: Int): Int
    private external fun nativeFillFeatures(h: Long, start: Int, count: Int, erb: FloatArray, spec: FloatArray)
    private external fun nativeApply(
        h: Long, start: Int, count: Int, lsnr: FloatArray, mask: FloatArray, coefs: FloatArray?,
        attenDb: Float, minDb: Float, maxErbDb: Float, maxDfDb: Float, pfBeta: Float, useDf: Boolean,
    )
    private external fun nativeOutput(h: Long, out: FloatArray)

    companion object {
        const val SAMPLE_RATE = 48_000
        const val HOP = 480
        const val ERB = 32
        const val DF_BINS = 96
        const val DF_ORDER = 5
        // libDF's local-SNR "stage gating" (-10 / 30 / 20 dB) skips or zeroes whole frames. On real, reverberant phone
        // speech it also dips the voice by ~9 dB, so it is switched off: this reproduces the official PyTorch model.
        const val MIN_DB = -100f
        const val MAX_ERB_DB = 100f
        const val MAX_DF_DB = 100f

        init { System.loadLibrary("auramic_dsp") }
    }
}
