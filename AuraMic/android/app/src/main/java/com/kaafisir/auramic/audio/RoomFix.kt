package com.kaafisir.auramic.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * "Fix the room only if the room is the problem": measures the recording first and applies a treatment only when the
 * measurement says it is needed, so dry / outdoor recordings pass through untouched (no quality loss).
 *
 *  1. Echo: a repeated copy of the voice (e.g. a hard wall) shows up as a sharp peak in the autocorrelation of the
 *     whitened signal. If such a peak is found its delay and strength are estimated and the echo is cancelled with the
 *     inverse comb filter y[n] = x[n] - g * y[n - delay]. No peak -> nothing happens.
 *  2. Reverb: a modulation-energy ratio (speech modulates its envelope 4-20 Hz, reverberation smears that into steadier
 *     energy) gives a 0..1 reverb strength. Below 0.05 nothing happens; above it a late-reverb suppressor runs with
 *     parameters scaled by the strength.
 * Thresholds were calibrated on read speech with synthetic echoes and room responses (RT60 0.2-0.9 s).
 */
object RoomFix {
    class Result(
        val voice: FloatArray,
        val echoDelayMs: Float?,
        val echoGain: Float,
        val reverbStrength: Float,
        val modulationRatio: Float,
    )

    /** [gain] is the estimated strength of the echo relative to the direct voice (0..0.6). */
    class Echo(val delayMs: Float, val c: Float, val z: Float, val gain: Float)

    // MER above DRY_MER is treated as "dry room", below WET_MER as "very reverberant"; linear in between.
    private const val DRY_MER = 12.0
    private const val WET_MER = 7.0
    private const val MIN_STRENGTH = 0.05

    fun process(x: FloatArray, n: Int, sr: Int = 48_000, enableEcho: Boolean = true, enableReverb: Boolean = true): Result {
        var y = if (x.size == n) x else x.copyOf(n)
        var echoMs: Float? = null; var echoG = 0f
        if (enableEcho) {
            detectEcho(y, n, sr)?.let { e ->
                y = cancelEcho(y, n, sr, e.delayMs, e.gain); echoMs = e.delayMs; echoG = e.gain
                AudioLog.i(TAG, "echo found: ${e.delayMs} ms, c=${e.c}, z=${e.z} -> cancelled (g=${e.gain})")
            }
        }
        var strength = 0.0; var mer = Double.NaN
        if (enableReverb && n > sr * 3) {
            mer = modulationEnergyRatio(y, n, sr)
            strength = ((DRY_MER - mer) / (DRY_MER - WET_MER)).coerceIn(0.0, 1.0)
            if (strength >= MIN_STRENGTH) {
                y = lateReverbSuppress(y, n, sr, rt60 = 0.3 + 0.4 * strength, alpha = 0.6 + 1.2 * strength, floorDb = -12.0)
            }
            AudioLog.i(TAG, "reverb: modulation ratio %.1f -> strength %.2f".format(mer, strength))
        }
        return Result(y, echoMs, echoG, strength.toFloat(), mer.toFloat())
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Echo
    // ---------------------------------------------------------------------------------------------------------------

    fun detectEcho(x: FloatArray, n: Int, sr: Int, minMs: Int = 40, maxMs: Int = 400, minC: Double = 0.12, minZ: Double = 25.0): Echo? {
        if (n < sr * 3) return null
        val fs = 16_000
        val full = Resampler.resample(x, n, sr, fs)
        val m = min(full.size, fs * 20)
        val s = DoubleArray(m) { full[it].toDouble() }

        // short-time level (20 ms centred box on x^2), used for speech detection and level normalisation
        val pre = DoubleArray(m + 1); for (i in 0 until m) pre[i + 1] = pre[i] + s[i] * s[i]
        val e = DoubleArray(m) { i -> val lo = max(0, i - 160); val hi = min(m, i + 160); sqrt((pre[hi] - pre[lo]) / 320.0) + 1e-5 }
        val sorted = e.copyOf().also { it.sort() }
        val thr = sorted[(0.9 * (m - 1)).toInt()] * 0.1
        val active = ArrayList<Double>(m)
        for (i in 0 until m) if (e[i] > thr) active += s[i]
        if (active.size < fs) return null

        // LPC whitening (order 24) estimated on the active samples
        val order = 24
        val a = lpc(DoubleArray(min(active.size, fs * 20)) { active[it] }, order) ?: return null
        val r = DoubleArray(m) { i ->
            var acc = 0.0
            for (k in 0..min(order, i)) acc += a[k] * s[i - k]
            acc / e[i]
        }

        // autocorrelation via FFT
        var size = 1; while (size < 2 * m) size = size shl 1
        val re = DoubleArray(size); val im = DoubleArray(size)
        System.arraycopy(r, 0, re, 0, m)
        val fft = Fft(size)
        fft.transform(re, im)
        for (i in 0 until size) { re[i] = re[i] * re[i] + im[i] * im[i]; im[i] = 0.0 }
        fft.transform(re, im, inverse = true)
        val maxLag = maxMs * fs / 1000; val lo = minMs * fs / 1000
        if (maxLag >= m || re[0] <= 0.0) return null
        val seg = DoubleArray(maxLag - lo + 1) { re[lo + it] / re[0] }
        var pk = 0
        for (i in seg.indices) if (seg[i] > seg[pk]) pk = i
        val c = seg[pk]
        val absSorted = DoubleArray(seg.size) { abs(seg[it]) }.also { it.sort() }
        val med = absSorted[absSorted.size / 2]
        val z = c / (med * 1.4826 + 1e-9)
        if (c < minC || z < minZ) return null
        val d = pk + lo

        // Refine the strength: the right gain is the one that leaves no correlation at the echo lag in the whitened output.
        var bestG = 0.05; var bestErr = Double.MAX_VALUE
        var g = 0.05
        val yc = DoubleArray(m)
        while (g <= 0.6001) {
            for (i in 0 until m) yc[i] = s[i] - if (i >= d) g * yc[i - d] else 0.0
            var num = 0.0; var den = 1e-12
            for (i in 0 until m) {
                var acc = 0.0
                for (k in 0..min(order, i)) acc += a[k] * yc[i - k]
                r[i] = acc / e[i]
                den += r[i] * r[i]
            }
            for (i in d until m) num += r[i] * r[i - d]
            val err = abs(num / den)
            if (err < bestErr) { bestErr = err; bestG = g }
            g += 0.025
        }
        return Echo((d * 1000.0 / fs).toFloat(), c.toFloat(), z.toFloat(), bestG.toFloat())
    }

    /** Levinson-Durbin; returns a[0..order] with a[0] = 1 (prediction-error filter) or null if degenerate. */
    private fun lpc(x: DoubleArray, order: Int): DoubleArray? {
        val r = DoubleArray(order + 1)
        for (k in 0..order) { var acc = 0.0; for (i in k until x.size) acc += x[i] * x[i - k]; r[k] = acc }
        if (r[0] <= 1e-9) return null
        r[0] *= 1.0 + 1e-9
        val a = DoubleArray(order + 1); a[0] = 1.0
        var err = r[0]
        for (i in 1..order) {
            var acc = r[i]
            for (j in 1 until i) acc += a[j] * r[i - j]
            val k = -acc / err
            val tmp = a.copyOf()
            for (j in 1 until i) a[j] = tmp[j] + k * tmp[i - j]
            a[i] = k
            err *= 1 - k * k
            if (err <= 0) return null
        }
        return a
    }

    /** Removes an echo of delay [delayMs] and strength [gain]; the recursion also removes its repeats. */
    fun cancelEcho(x: FloatArray, n: Int, sr: Int, delayMs: Float, gain: Float): FloatArray {
        val g = gain.coerceIn(0f, 0.6f).toDouble()
        val d = (delayMs * sr / 1000f).roundToInt().coerceAtLeast(1)
        val y = FloatArray(n)
        for (i in 0 until n) y[i] = (x[i] - if (i >= d) g * y[i - d] else 0.0).toFloat()
        return y
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Reverb
    // ---------------------------------------------------------------------------------------------------------------

    /** SRMR-lite: energy of 4-20 Hz envelope modulations over 20-64 Hz ones, summed over 10 bands (higher = drier). */
    fun modulationEnergyRatio(x: FloatArray, n: Int, sr: Int, bands: Int = 10, lo: Double = 150.0, hi: Double = 6000.0): Double {
        val nfft = 1024; val hop = (sr * 0.004).roundToInt()          // 4 ms envelope rate
        val fft = Fft(nfft)
        val win = DoubleArray(nfft) { 0.5 - 0.5 * cos(2 * PI * it / nfft) }
        val frames = 1 + n / hop
        val edges = DoubleArray(bands + 1) { lo * (hi / lo).pow(it.toDouble() / bands) }
        val bandOf = IntArray(nfft / 2 + 1) { k ->
            val f = k * sr.toDouble() / nfft
            var b = -1; for (i in 0 until bands) if (f >= edges[i] && f < edges[i + 1]) b = i; b
        }
        val env = Array(bands) { DoubleArray(frames) }
        val re = DoubleArray(nfft); val im = DoubleArray(nfft)
        for (t in 0 until frames) {
            val start = t * hop - nfft / 2
            for (i in 0 until nfft) { val idx = start + i; re[i] = if (idx in 0 until n) x[idx] * win[i] else 0.0; im[i] = 0.0 }
            fft.transform(re, im)
            val acc = DoubleArray(bands)
            for (k in 0..nfft / 2) { val b = bandOf[k]; if (b >= 0) acc[b] += re[k] * re[k] + im[k] * im[k] }
            for (b in 0 until bands) env[b][t] = sqrt(acc[b])
        }
        val fr = sr.toDouble() / hop
        val wl = 64; val m256 = Fft(256)
        val hann = DoubleArray(wl) { 0.5 - 0.5 * cos(2 * PI * it / (wl - 1)) }
        var num = 0.0; var den = 0.0
        val sr2 = DoubleArray(256); val si2 = DoubleArray(256)
        for (b in 0 until bands) {
            val e = env[b]
            if ((e.maxOrNull() ?: 0.0) < 1e-10) continue
            var s = 0
            while (s + wl <= frames) {
                var mean = 0.0; for (i in 0 until wl) mean += e[s + i]; mean /= wl
                for (i in 0 until 256) { sr2[i] = if (i < wl) (e[s + i] - mean) * hann[i] else 0.0; si2[i] = 0.0 }
                m256.transform(sr2, si2)
                for (k in 0..128) {
                    val fm = k * fr / 256
                    val p = sr2[k] * sr2[k] + si2[k] * si2[k]
                    if (fm >= 4 && fm < 20) num += p else if (fm >= 20 && fm < 64) den += p
                }
                s += wl / 2
            }
        }
        return num / (den + 1e-12)
    }

    /** Lebart/Habets-style late reverberation suppression (power subtraction of the decayed, delayed total power). */
    fun lateReverbSuppress(
        x: FloatArray, n: Int, sr: Int, rt60: Double, alpha: Double, floorDb: Double, td: Double = 0.05,
    ): FloatArray {
        val nfft = 1024; val hop = 256; val bins = nfft / 2 + 1
        val fft = Fft(nfft)
        val win = DoubleArray(nfft) { 0.5 - 0.5 * cos(2 * PI * it / nfft) }
        val frames = (n + hop - 1) / hop + 1
        val specRe = Array(frames) { FloatArray(bins) }; val specIm = Array(frames) { FloatArray(bins) }
        val power = Array(frames) { FloatArray(bins) }
        val re = DoubleArray(nfft); val im = DoubleArray(nfft)
        for (t in 0 until frames) {
            val start = t * hop - nfft / 2
            for (i in 0 until nfft) { val idx = start + i; re[i] = if (idx in 0 until n) x[idx] * win[i] else 0.0; im[i] = 0.0 }
            fft.transform(re, im)
            for (k in 0 until bins) { specRe[t][k] = re[k].toFloat(); specIm[t][k] = im[k].toFloat(); power[t][k] = (re[k] * re[k] + im[k] * im[k]).toFloat() }
        }
        val nd = (td * sr / hop).roundToInt().coerceAtLeast(1)
        val delta = 3 * ln(10.0) / rt60
        val decay = exp(-2 * delta * td).toFloat()
        val gmin2 = 10.0.pow(floorDb / 10).toFloat()
        val gain = Array(frames) { FloatArray(bins) }
        for (t in 0 until frames) {
            val src = if (t >= nd) power[t - nd] else null
            for (k in 0 until bins) {
                var late = 0f
                if (src != null) {
                    val a = if (k > 0) src[k - 1] else 0f; val c = if (k < bins - 1) src[k + 1] else 0f
                    late = decay * (a + src[k] + c) / 3f
                }
                gain[t][k] = sqrt(max(1f - (alpha.toFloat() * late) / (power[t][k] + 1e-12f), gmin2))
            }
        }
        val out = DoubleArray(n + nfft); val env = DoubleArray(n + nfft)
        for (t in 0 until frames) {
            for (k in 0 until bins) {
                val g = (gain[max(0, t - 1)][k] + gain[t][k] + gain[min(frames - 1, t + 1)][k]) / 3f     // smooth over time
                re[k] = specRe[t][k] * g.toDouble(); im[k] = specIm[t][k] * g.toDouble()
                if (k in 1 until nfft / 2) { re[nfft - k] = re[k]; im[nfft - k] = -im[k] }
            }
            im[0] = 0.0; im[nfft / 2] = 0.0
            fft.transform(re, im, inverse = true)
            val start = t * hop - nfft / 2
            for (i in 0 until nfft) {
                val idx = start + i
                if (idx in 0 until n) { out[idx] += re[i] / nfft * win[i]; env[idx] += win[i] * win[i] }
            }
        }
        return FloatArray(n) { if (env[it] > 1e-8) (out[it] / env[it]).toFloat() else x[it] }
    }

    private const val TAG = "RoomFix"
}
