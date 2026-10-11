package com.kaafisir.auramic.audio

/** Switches for offline experiments on the benchmark (hosttest). Production code never sets them: every default is "off". */
object Experiments {
    @Volatile @JvmField var roomFix = false          // echo cancellation + late-reverb suppression (RoomFix)
    @Volatile @JvmField var dfnTwice = false         // run DeepFilterNet a second time on its own output
    @Volatile @JvmField var gtcrnAfterDfn = false    // run GTCRN after DeepFilterNet (High only)
    @Volatile @JvmField var gtcrnBeforeDfn = false   // run GTCRN first, DeepFilterNet after (High only)
}
