package com.kaafisir.auramic.audio

import com.kaafisir.auramic.domain.model.VoicePreset

/** Mastering chain in C++ (EQ, de-esser, compressor, BS.1770 LUFS normalisation, look-ahead limiter). 48 kHz mono. */
object NativePolish {
    class Report(val inputLufs: Float, val outputLufs: Float, val gainDb: Float, val peakDb: Float)

    init { System.loadLibrary("auramic_dsp") }

    /** In-place. */
    fun run(x: FloatArray, n: Int, preset: VoicePreset, normalize: Boolean, targetLufs: Float, ceilingDb: Float = -1.5f): Report {
        val rep = FloatArray(4)
        nativeRun(x, n, preset.ordinal, normalize, targetLufs, ceilingDb, rep)
        return Report(rep[0], rep[1], rep[2], rep[3])
    }

    fun lufs(x: FloatArray, n: Int): Float = nativeLufs(x, n)

    private external fun nativeRun(x: FloatArray, n: Int, preset: Int, normalize: Boolean, target: Float, ceiling: Float, report: FloatArray)
    private external fun nativeLufs(x: FloatArray, n: Int): Float
}
