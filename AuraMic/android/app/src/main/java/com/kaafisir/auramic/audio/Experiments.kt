package com.kaafisir.auramic.audio

/**
 * Switches for offline experiments on the benchmark (hosttest). Every default is "off" = what the app ships with.
 *
 * Measured, and rejected: post-filter, DFN twice, room fix (hurt reverb clips), GTCRN before DFN and GTCRN after DFN.
 * GTCRN after DFN looked good on DNSMOS/PESQ (16 kHz metrics) but GTCRN runs at 16 kHz: it cut everything above 8 kHz
 * (8-12 kHz 24 dB down, >12 kHz gone) and listeners rated the result 1-2 stars out of 5 - muffled. Always check full-band
 * spectra (tools/bench) next to the scores, and listen.
 */
object Experiments {
    @Volatile @JvmField var roomFix = false          // echo cancellation + late-reverb suppression (RoomFix)
    @Volatile @JvmField var dfnTwice = false         // run DeepFilterNet a second time on its own output
    @Volatile @JvmField var gtcrnAfterDfn = false    // run GTCRN after DeepFilterNet (High only) - second cleaning pass
    @Volatile @JvmField var gtcrnBeforeDfn = false   // run GTCRN first, DeepFilterNet after (High only)
}
