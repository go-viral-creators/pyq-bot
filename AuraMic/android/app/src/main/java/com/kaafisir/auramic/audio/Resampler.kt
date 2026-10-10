package com.kaafisir.auramic.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Windowed-sinc resampler (Hann window, 16 zero crossings per side). Used only when the mic did not give 48 kHz. */
object Resampler {
    private const val HALF = 16
    private const val PHASES = 512

    fun resample(x: FloatArray, n: Int, from: Int, to: Int): FloatArray {
        if (from == to || n == 0) return x.copyOf(n)
        val ratio = from.toDouble() / to
        val outN = floor(n / ratio).toInt()
        val cutoff = min(1.0, to.toDouble() / from) * 0.97     // anti-alias when going down
        val taps = 2 * HALF
        // kernel[phase][tap]: tap k corresponds to input index floor(t) - HALF + 1 + k
        val kernel = Array(PHASES) { ph ->
            val frac = ph.toDouble() / PHASES
            FloatArray(taps) { k ->
                val d = (k - HALF + 1) - frac            // distance from the output position in input samples
                val arg = PI * cutoff * d
                val sinc = if (d == 0.0) 1.0 else sin(arg) / arg
                val w = 0.5 * (1 + cos(PI * d / HALF))   // Hann
                (cutoff * sinc * w).toFloat()
            }
        }
        val out = FloatArray(outN)
        for (j in 0 until outN) {
            val t = j * ratio
            val base = floor(t).toInt()
            val ph = min(PHASES - 1, ((t - base) * PHASES + 0.5).toInt())
            val ker = kernel[ph]
            var acc = 0f
            val i0 = base - HALF + 1
            for (k in 0 until taps) {
                val i = i0 + k
                if (i in 0 until n) acc += x[i] * ker[k]
            }
            out[j] = acc
        }
        return out
    }

    /** Linear PCM16 -> float conversion. */
    fun toFloat(pcm: ShortArray, n: Int): FloatArray = FloatArray(n) { pcm[it] / 32768f }

    fun toShort(x: FloatArray, n: Int): ShortArray = ShortArray(n) { i ->
        val v = (x[i] * 32767f).let { if (it >= 0) it + 0.5f else it - 0.5f }
        max(-32768f, min(32767f, v)).toInt().toShort()
    }
}
