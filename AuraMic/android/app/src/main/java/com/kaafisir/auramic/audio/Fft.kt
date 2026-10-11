package com.kaafisir.auramic.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** In-place radix-2 complex FFT of a fixed power-of-two size (double precision). Not thread-safe (stateless apart from tables). */
class Fft(val n: Int) {
    init { require(n >= 2 && n and (n - 1) == 0) { "FFT size must be a power of two" } }

    private val cosT = DoubleArray(n / 2) { cos(2 * PI * it / n) }
    private val sinT = DoubleArray(n / 2) { -sin(2 * PI * it / n) }
    private val rev = IntArray(n).also { r ->
        val bits = Integer.numberOfTrailingZeros(n)
        for (i in 0 until n) r[i] = Integer.reverse(i) ushr (32 - bits)
    }

    /** Forward (e^-i) transform, or the unscaled inverse when [inverse] is true. */
    fun transform(re: DoubleArray, im: DoubleArray, inverse: Boolean = false) {
        for (i in 0 until n) {
            val j = rev[i]
            if (j > i) { val a = re[i]; re[i] = re[j]; re[j] = a; val b = im[i]; im[i] = im[j]; im[j] = b }
        }
        var len = 2
        while (len <= n) {
            val half = len / 2; val step = n / len
            var i = 0
            while (i < n) {
                for (k in 0 until half) {
                    val wr = cosT[k * step]; val wi = if (inverse) -sinT[k * step] else sinT[k * step]
                    val a = i + k; val b = a + half
                    val tr = re[b] * wr - im[b] * wi; val ti = re[b] * wi + im[b] * wr
                    re[b] = re[a] - tr; im[b] = im[a] - ti; re[a] += tr; im[a] += ti
                }
                i += len
            }
            len = len shl 1
        }
    }
}
