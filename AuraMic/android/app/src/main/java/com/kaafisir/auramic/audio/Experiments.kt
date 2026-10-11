package com.kaafisir.auramic.audio

/**
 * Switches for offline experiments on the benchmark (hosttest). Defaults are what the app ships with: only [gtcrnAfterDfn] is on
 * (benchmark of 22 clips: OVRL 3.25 -> 3.29, residual noise -46 -> -54 dB, echo clips +0.16 OVRL, clean clips unchanged).
 * Everything else measured no better or worse: post-filter, DFN twice, room fix (hurt reverb clips), GTCRN before DFN.
 */
object Experiments {
    @Volatile @JvmField var roomFix = false          // echo cancellation + late-reverb suppression (RoomFix)
    @Volatile @JvmField var dfnTwice = false         // run DeepFilterNet a second time on its own output
    @Volatile @JvmField var gtcrnAfterDfn = true     // run GTCRN after DeepFilterNet (High only) - second cleaning pass
    @Volatile @JvmField var gtcrnBeforeDfn = false   // run GTCRN first, DeepFilterNet after (High only)
}
