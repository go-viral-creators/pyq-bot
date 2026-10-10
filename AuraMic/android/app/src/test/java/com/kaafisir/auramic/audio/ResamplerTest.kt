package com.kaafisir.auramic.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class ResamplerTest {
    private fun sine(freq: Double, sr: Int, seconds: Double) = FloatArray((sr * seconds).toInt()) { (0.5 * sin(2 * PI * freq * it / sr)).toFloat() }
    private fun rms(x: FloatArray, from: Int, to: Int) = sqrt((from until to).sumOf { x[it].toDouble() * x[it] } / (to - from))

    @Test fun upsample44kTo48kKeepsLengthLevelAndFrequency() {
        val x = sine(1000.0, 44_100, 1.0)
        val y = Resampler.resample(x, x.size, 44_100, 48_000)
        assertEquals(48_000.0, y.size.toDouble(), 2.0)
        assertEquals(rms(x, 1000, 40_000), rms(y, 1000, 44_000), 0.01)
        val ref = sine(1000.0, 48_000, 1.0)
        var err = 0.0; for (i in 2000 until 40_000) err += abs(y[i] - ref[i])
        assertTrue("mean abs error ${err / 38_000}", err / 38_000 < 0.01)
    }

    @Test fun sameRateIsIdentity() {
        val x = sine(440.0, 48_000, 0.2)
        assertEquals(x.size, Resampler.resample(x, x.size, 48_000, 48_000).size)
    }

    @Test fun shortConversionRoundTrip() {
        val pcm = ShortArray(10) { (it * 1000 - 5000).toShort() }
        val f = Resampler.toFloat(pcm, 10)
        assertTrue(Resampler.toShort(f, 10).contentEquals(pcm))
    }
}
