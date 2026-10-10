package com.kaafisir.auramic.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

class RoomFixTest {
    private val sr = 48_000

    /** Syllable-like bursts of noise with a changing spectral tilt, separated by short pauses. */
    private fun speechLike(seconds: Int, seed: Long = 1): FloatArray {
        val rnd = Random(seed); val x = FloatArray(seconds * sr); var i = 0
        while (i < x.size) {
            val len = (0.08 + rnd.nextDouble() * 0.25) * sr; val gap = (0.04 + rnd.nextDouble() * 0.2) * sr
            val tilt = 0.5 + rnd.nextDouble() * 0.45; var prev = 0.0
            val f0 = 90 + rnd.nextDouble() * 120
            for (k in 0 until len.toInt()) {
                if (i + k >= x.size) break
                val env = sin(PI * k / len)
                prev = tilt * prev + rnd.nextGaussian() * 0.12
                val voiced = 0.25 * sin(2 * PI * f0 * k / sr) + 0.15 * sin(2 * PI * 2 * f0 * k / sr)
                x[i + k] = (env * (prev + voiced)).toFloat()
            }
            i += (len + gap).toInt()
        }
        return x
    }

    private fun withEcho(x: FloatArray, delayMs: Int, gain: Double): FloatArray {
        val d = delayMs * sr / 1000
        return FloatArray(x.size) { (x[it] + if (it >= d) gain * x[it - d] else 0.0).toFloat() }
    }

    private fun reverberate(x: FloatArray, rt60: Double): FloatArray {
        val rnd = Random(7); val n = (rt60 * 1.0 * sr).toInt()
        val h = FloatArray(n) { (rnd.nextGaussian() * exp(-6.9 * it / (rt60 * sr)) * 0.02).toFloat() }
        h[0] = 1f
        val y = FloatArray(x.size)
        // sparse convolution: skip silent samples
        for (i in x.indices) { val v = x[i]; if (v == 0f) continue; val m = minOf(n, x.size - i); for (k in 0 until m) y[i + k] += v * h[k] }
        return y
    }

    @Test fun fftMatchesTheDefinitionAndInverts() {
        val n = 64; val rnd = Random(3)
        val re = DoubleArray(n) { rnd.nextGaussian() }; val im = DoubleArray(n)
        val ref = DoubleArray(n) { k -> (0 until n).sumOf { re[it] * cos(2 * PI * k * it / n) } }
        val r2 = re.copyOf(); val i2 = im.copyOf()
        Fft(n).transform(r2, i2)
        for (k in 0 until n) assertEquals(ref[k], r2[k], 1e-9)
        Fft(n).transform(r2, i2, inverse = true)
        for (k in 0 until n) assertEquals(re[k], r2[k] / n, 1e-9)
    }

    @Test fun anEchoIsFoundAndCancelled() {
        val dry = speechLike(14); val x = withEcho(dry, 130, 0.35)
        val e = RoomFix.detectEcho(x, x.size, sr)
        assertNotNull("echo must be detected", e)
        assertEquals(130f, e!!.delayMs, 3f)
        val g = e.gain
        val y = RoomFix.cancelEcho(x, x.size, sr, e.delayMs, g)
        assertTrue("estimated gain $g", g in 0.2f..0.5f)
        fun err(a: FloatArray) = sqrt((sr until a.size).sumOf { ((a[it] - dry[it]).toDouble()).let { d -> d * d } } / (a.size - sr))
        assertTrue("echo error should drop (${err(x)} -> ${err(y)})", err(y) < 0.6 * err(x))
    }

    @Test fun noEchoMeansNothingIsDetectedOrChanged() {
        val x = speechLike(14, seed = 5)
        assertNull(RoomFix.detectEcho(x, x.size, sr))
        val r = RoomFix.process(x, x.size, sr, enableReverb = false)
        assertNull(r.echoDelayMs)
        assertTrue(r.voice.indices.all { r.voice[it] == x[it] })
    }

    @Test fun reverbyAudioScoresLowerThanDryAndTriggersTheTreatment() {
        val dry = speechLike(14, seed = 9); val wet = reverberate(dry, 0.8)
        val md = RoomFix.modulationEnergyRatio(dry, dry.size, sr); val mw = RoomFix.modulationEnergyRatio(wet, wet.size, sr)
        assertTrue("dry $md should be above wet $mw", md > mw * 1.5)
        val r = RoomFix.process(wet, wet.size, sr, enableEcho = false)
        assertTrue("strength ${r.reverbStrength} (MER ${r.modulationRatio})", r.reverbStrength > 0.3f)
        assertEquals(wet.size, r.voice.size)
        assertTrue(r.voice.all { it.isFinite() })
    }

    @Test fun lateReverbSuppressionLowersTheTailButKeepsTheLength() {
        val dry = speechLike(10, seed = 2); val wet = reverberate(dry, 0.7)
        val y = RoomFix.lateReverbSuppress(wet, wet.size, sr, rt60 = 0.6, alpha = 1.5, floorDb = -12.0)
        assertEquals(wet.size, y.size)
        fun energy(a: FloatArray) = a.sumOf { it.toDouble() * it }
        assertTrue(energy(y) < energy(wet))
        assertTrue(y.all { it.isFinite() && abs(it) < 10f })
    }

    @Test fun shortClipsAreLeftAlone() {
        val x = speechLike(2); val r = RoomFix.process(x, x.size, sr)
        assertTrue(r.voice.indices.all { r.voice[it] == x[it] }); assertEquals(0f, r.reverbStrength, 0f)
    }
}
