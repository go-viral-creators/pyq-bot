package com.kaafisir.auramic.audio

import com.kaafisir.auramic.domain.model.SilenceCut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class SilenceCutterTest {
    private val sr = 48_000

    /** speech-like bursts (200 Hz tone, -20 dBFS) separated by exact digital silence. */
    private fun clip(lead: Double, bursts: List<Double>, gaps: List<Double>, tail: Double): FloatArray {
        val parts = ArrayList<FloatArray>()
        parts += FloatArray((lead * sr).toInt())
        bursts.forEachIndexed { i, d ->
            parts += FloatArray((d * sr).toInt()) { k -> (0.1 * sin(2 * PI * 200 * k / sr)).toFloat() }
            if (i < gaps.size) parts += FloatArray((gaps[i] * sr).toInt())
        }
        parts += FloatArray((tail * sr).toInt())
        return parts.reduce { a, b -> a + b }
    }

    @Test fun offLeavesAudioUntouched() {
        val x = clip(1.0, listOf(1.0), emptyList(), 1.0)
        val r = SilenceCutter.cut(x, x.size, sr, SilenceCut.OFF)
        assertEquals(x.size, r.length); assertEquals(0, r.removedSamples)
    }

    @Test fun gentleShortensLongPausesAndTrimsEnds() {
        val x = clip(1.0, listOf(1.0, 1.0), listOf(2.0), 1.5)   // 6.5 s total
        val r = SilenceCutter.cut(x, x.size, sr, SilenceCut.GENTLE)
        val seconds = r.length / sr.toDouble()
        // speech 2.0 s + pause ~0.35 s + lead ~0.08..0.2 s + tail ~0.15..0.3 s
        assertTrue("got $seconds s", seconds in 2.4..3.2)
        assertEquals(x.size - r.length, r.removedSamples)
    }

    @Test fun shortPausesAreKept() {
        val x = clip(0.1, listOf(1.0, 1.0), listOf(0.3), 0.1)
        val r = SilenceCutter.cut(x, x.size, sr, SilenceCut.GENTLE)
        assertTrue("short pause must survive", r.removedSamples < sr / 10)
    }

    @Test fun pausesModeCutsEveryPauseOverThreeHundredMs() {
        val x = clip(0.2, listOf(1.0, 1.0, 1.0), listOf(0.35, 1.5), 0.2)
        val r = SilenceCutter.cut(x, x.size, sr, SilenceCut.PAUSES)
        // 3 s of speech + 2 pauses cut to ~0.1 s + trimmed ends: far less than the 5.25 s input
        assertTrue("got ${r.length / sr.toDouble()} s", r.length / sr.toDouble() < 3.6)
        // a 0.25 s pause is below the limit and must survive
        val y = clip(0.1, listOf(1.0, 1.0), listOf(0.25), 0.1)
        assertTrue(SilenceCutter.cut(y, y.size, sr, SilenceCut.PAUSES).removedSamples < sr / 10)
    }

    @Test fun tightCutsMoreThanGentle() {
        val x = clip(0.5, listOf(1.0, 1.0, 1.0), listOf(0.6, 0.6), 0.5)
        val g = SilenceCutter.cut(x, x.size, sr, SilenceCut.GENTLE).length
        val t = SilenceCutter.cut(x, x.size, sr, SilenceCut.TIGHT).length
        assertTrue(t < g)
    }

    @Test fun speechEnergyIsPreserved() {
        val x = clip(1.0, listOf(1.0, 1.0), listOf(2.0), 1.0)
        val r = SilenceCutter.cut(x, x.size, sr, SilenceCut.TIGHT)
        fun energy(a: FloatArray, n: Int) = (0 until n).sumOf { a[it].toDouble() * a[it] }
        assertEquals(energy(x, x.size), energy(r.pcm, r.length), energy(x, x.size) * 0.02)
    }

    @Test fun silentAndTinyClipsAreSafe() {
        val silent = FloatArray(sr * 2)
        assertEquals(silent.size, SilenceCutter.cut(silent, silent.size, sr, SilenceCut.TIGHT).length)
        val tiny = FloatArray(100) { 0.1f }
        assertEquals(100, SilenceCutter.cut(tiny, 100, sr, SilenceCut.TIGHT).length)
    }
}
