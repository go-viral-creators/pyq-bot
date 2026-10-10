package com.kaafisir.auramic.service.overlay

import androidx.annotation.StringRes

/** What the floating bubble / status pill should currently show. */
sealed interface OverlayState {
    data object Idle : OverlayState
    data class Recording(val elapsedMs: Long, val cancelArmed: Boolean) : OverlayState
    data object Processing : OverlayState
    data object Sending : OverlayState
    /** Short-lived result banner; auto-returns to [Idle]. */
    data class Done(val ok: Boolean, @StringRes val message: Int) : OverlayState
    /** Short-lived hint, e.g. "Hold to record" after a tap. */
    data class Hint(@StringRes val message: Int) : OverlayState

    /** True while a recording/processing cycle owns the bubble (it must not hide or start another). */
    val isBusy: Boolean get() = this is Recording || this is Processing || this is Sending
}
