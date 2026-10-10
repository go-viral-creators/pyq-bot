package com.kaafisir.auramic.audio

/**
 * Kotlin face of the C++ noise suppressor in `src/main/cpp` (see aura_dsp.h for the algorithm).
 * One instance owns one native estimator; call [close] when done. Not thread-safe.
 */
class NativeDenoiser(sampleRate: Int) : AutoCloseable {
    private var handle: Long = nativeCreate(sampleRate)

    /** STFT frames the native side will use for [samples] samples. */
    fun frameCount(samples: Int): Int = nativeFrameCount(samples)

    /**
     * Pass 1: DSP analysis. Returns frames * [FEATURES] floats: per frame
     * [BANDS] SNR features followed by [BANDS] DSP gains. Used as model input.
     */
    fun extractFeatures(pcm: ShortArray, length: Int, strength: Float): FloatArray {
        check(handle != 0L) { "closed" }
        val frames = frameCount(length)
        val out = FloatArray(frames * FEATURES)
        if (frames > 0) {
            val n = nativeExtractFeatures(handle, pcm, length, out, strength)
            check(n == frames) { "native feature extraction failed ($n)" }
        }
        return out
    }

    /**
     * Pass 2: produces the cleaned signal. [bandMask] (frames * [BANDS], values in (0,1]) comes
     * from the TFLite model and is blended with the DSP gain; pass null for DSP only.
     */
    fun process(
        pcm: ShortArray,
        length: Int,
        strength: Float,
        bandMask: FloatArray?,
        maskWeight: Float,
        normalize: Boolean,
    ): ShortArray {
        check(handle != 0L) { "closed" }
        val out = ShortArray(length)
        if (length == 0) return out
        if (bandMask != null) require(bandMask.size == frameCount(length) * BANDS) { "mask shape" }
        val rc = nativeProcess(handle, pcm, length, out, strength, bandMask, maskWeight, normalize)
        check(rc == 0) { "native processing failed ($rc)" }
        return out
    }

    override fun close() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0L
        }
    }

    private external fun nativeCreate(sampleRate: Int): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeFrameCount(samples: Int): Int
    private external fun nativeExtractFeatures(
        handle: Long, pcm: ShortArray, length: Int, features: FloatArray, strength: Float,
    ): Int
    private external fun nativeProcess(
        handle: Long, pcm: ShortArray, length: Int, output: ShortArray, strength: Float,
        mask: FloatArray?, maskWeight: Float, normalize: Boolean,
    ): Int

    companion object {
        const val BANDS = 32
        const val FEATURES = BANDS * 2

        init {
            System.loadLibrary("auramic_dsp")
        }
    }
}
