package com.kaafisir.auramic.audio

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/** Result of denoising a clip. */
class DenoiseResult(val pcm: ShortArray, val usedModel: Boolean, val backgroundReductionDb: Float)

/**
 * Orchestrates the two-pass pipeline:
 *  1. native DSP analysis -> features
 *  2. (optional) TFLite mask from those features
 *  3. native synthesis with the blended gain, loudness normalisation and limiting.
 */
@Singleton
class NoiseSuppressionEngine @Inject constructor(private val maskModel: MaskModel) {

    fun process(
        pcm: ShortArray,
        length: Int,
        sampleRate: Int,
        strength: Float,
        normalize: Boolean,
    ): DenoiseResult {
        if (length == 0) return DenoiseResult(ShortArray(0), false, 0f)
        NativeDenoiser(sampleRate).use { dsp ->
            val frames = dsp.frameCount(length)
            val mask = if (maskModel.isAvailable) {
                maskModel.predict(dsp.extractFeatures(pcm, length, strength), frames)
            } else null
            val out = dsp.process(pcm, length, strength, mask, MASK_WEIGHT, normalize)
            return DenoiseResult(out, mask != null, backgroundReductionDb(pcm, out, length, sampleRate))
        }
    }

    companion object {
        /** Exponent of the model mask in the geometric blend with the DSP gain. */
        const val MASK_WEIGHT = 0.5f

        /**
         * Estimates how much quieter the background got: the level of the quietest 20% of
         * 20 ms blocks before vs. after (loudness normalisation is compensated via the loud blocks).
         */
        fun backgroundReductionDb(before: ShortArray, after: ShortArray, length: Int, sampleRate: Int): Float {
            val block = max(1, sampleRate / 50)
            val blocks = length / block
            if (blocks < 5) return 0f
            fun levels(x: ShortArray): FloatArray = FloatArray(blocks) { b ->
                var e = 0.0
                for (i in 0 until block) {
                    val s = x[b * block + i] / 32768.0
                    e += s * s
                }
                (e / block).toFloat()
            }
            fun quietAndLoud(x: ShortArray): Pair<Double, Double> {
                val sorted = levels(x).sorted()
                val q = max(1, blocks / 5)
                val quiet = sorted.take(q).average()
                val loud = sorted.takeLast(max(1, blocks / 5)).average()
                return quiet to loud
            }
            val (qb, lb) = quietAndLoud(before)
            val (qa, la) = quietAndLoud(after)
            val snrBefore = 10 * log10((lb + 1e-12) / (qb + 1e-12))
            val snrAfter = 10 * log10((la + 1e-12) / (qa + 1e-12))
            return (snrAfter - snrBefore).toFloat().coerceIn(0f, 60f)
        }

        fun rms(x: ShortArray, length: Int): Double {
            if (length == 0) return 0.0
            var e = 0.0
            for (i in 0 until length) e += (x[i] / 32768.0).let { it * it }
            return sqrt(e / length)
        }
    }
}
