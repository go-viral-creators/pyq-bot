package com.kaafisir.auramic.audio

import com.kaafisir.auramic.domain.model.SilenceCut
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/**
 * Auto-cut of silent parts. Works on the *denoised* signal, where pauses are genuinely quiet, so a simple
 * level detector is reliable:
 *   - 10 ms frames, speech threshold = max(-55 dBFS, 90th-percentile level - 32 dB), 80 ms hang-over
 *   - GENTLE shortens pauses > 0.75 s down to 0.35 s; TIGHT shortens pauses > 0.4 s down to 0.16 s;
 *     PAUSES (the app default) cuts every silence longer than 0.3 s down to about 0.1 s so words never run together
 *   - leading silence is trimmed to 80 ms, trailing to 150 ms
 * Cuts happen in the middle of a pause with an 8 ms cross-fade, so breaths and room tone around words stay natural.
 */
object SilenceCutter {
    class Result(val pcm: FloatArray, val length: Int, val removedSamples: Int)

    /** [minPauseMs] is measured between the hang-overs, so a pause counts as (minPauseMs + 2 x hangMs) of real silence. */
    private class Cfg(val minPauseMs: Int, val keepMs: Int, val hangMs: Int = HANG_MS)
    private val gentle = Cfg(750, 350)
    private val tight = Cfg(400, 160)
    private val pauses = Cfg(minPauseMs = 240, keepMs = 40, hangMs = 30)   // silence > 0.3 s is cut down to ~0.1 s

    private const val FRAME_MS = 10
    private const val HANG_MS = 80
    private const val LEAD_KEEP_MS = 80
    private const val TRAIL_KEEP_MS = 150
    private const val FADE_MS = 8

    fun cut(x: FloatArray, n: Int, sr: Int, mode: SilenceCut): Result {
        if (mode == SilenceCut.OFF || n < sr / 2) return Result(x, n, 0)
        val cfg = when (mode) { SilenceCut.TIGHT -> tight; SilenceCut.PAUSES -> pauses; else -> gentle }
        val frame = sr * FRAME_MS / 1000
        val frames = n / frame
        if (frames < 20) return Result(x, n, 0)

        val db = FloatArray(frames) { f ->
            var e = 0.0
            for (i in f * frame until (f + 1) * frame) e += x[i].toDouble() * x[i]
            (10 * log10(e / frame + 1e-12)).toFloat()
        }
        val sorted = db.sortedArray()
        val ref = sorted[min(frames - 1, (frames * 0.9).toInt())]
        if (ref < -60f) return Result(x, n, 0)              // essentially silent clip: leave it alone
        val thr = max(-55f, ref - 32f)

        val speech = BooleanArray(frames) { db[it] > thr }
        val hang = cfg.hangMs / FRAME_MS
        val padded = speech.copyOf()
        for (f in 0 until frames) if (speech[f]) {
            for (k in max(0, f - hang)..min(frames - 1, f + hang)) padded[k] = true
        }
        if (padded.none { it }) return Result(x, n, 0)

        // collect the sample ranges to keep
        val keep = ArrayList<IntArray>()           // [startSample, endSample)
        var f = 0
        var segStart = 0
        val minPause = cfg.minPauseMs / FRAME_MS
        while (f < frames) {
            if (padded[f]) { f++; continue }
            var g = f
            while (g < frames && !padded[g]) g++
            val runStart = f * frame
            val runEnd = g * frame
            val len = g - f
            val isLeading = f == 0
            val isTrailing = g == frames
            val removeFrom: Int
            val removeTo: Int
            when {
                isLeading -> { removeFrom = 0; removeTo = max(0, runEnd - LEAD_KEEP_MS * sr / 1000) }
                isTrailing -> { removeFrom = runStart + TRAIL_KEEP_MS * sr / 1000; removeTo = n }
                len >= minPause -> {
                    val keepSamples = cfg.keepMs * sr / 1000
                    removeFrom = runStart + keepSamples / 2
                    removeTo = runEnd - keepSamples / 2
                }
                else -> { removeFrom = 0; removeTo = 0 }
            }
            if (removeTo - removeFrom > frame * 2) {
                if (removeFrom > segStart) keep.add(intArrayOf(segStart, removeFrom)) else if (removeFrom == 0) { /* leading cut */ }
                segStart = removeTo
            }
            f = g
        }
        if (segStart < n) keep.add(intArrayOf(segStart, n))
        val total = keep.sumOf { it[1] - it[0] }
        if (total >= n - frame) return Result(x, n, 0)

        val out = FloatArray(total)
        val fade = FADE_MS * sr / 1000
        var pos = 0
        for ((idx, seg) in keep.withIndex()) {
            val len = seg[1] - seg[0]
            System.arraycopy(x, seg[0], out, pos, len)
            if (idx > 0 && len > fade && pos >= fade) {
                // Smooth the join: the cut is inside a pause, so a short fade-out / fade-in pair is inaudible.
                for (i in 0 until fade) out[pos + i] *= i / fade.toFloat()
                for (i in 1..fade) out[pos - i] *= (i - 1).toFloat() / fade
            }
            pos += len
        }
        return Result(out, total, n - total)
    }

    /** Helper for tests / UI: RMS in dBFS. */
    fun rmsDb(x: FloatArray, from: Int, to: Int): Float {
        var e = 0.0
        for (i in from until to) e += x[i].toDouble() * x[i]
        return (10 * log10(e / max(1, to - from) + 1e-12)).toFloat()
    }
}
