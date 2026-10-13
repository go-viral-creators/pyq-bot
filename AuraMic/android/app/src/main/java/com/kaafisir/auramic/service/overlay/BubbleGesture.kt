package com.kaafisir.auramic.service.overlay

/**
 * Touch state machine for the floating mic - pure Kotlin so the same logic drives the real
 * overlay (View touch events) and the interactive tutorial (Compose pointer events), and is
 * unit-tested without Android.
 *
 *   down -> (moved beyond slop within [holdDelayMs]) -> DRAG   (reposition the bubble)
 *        -> (held still for [holdDelayMs])           -> HOLD   (recording)
 *             while HOLD: dragging left past [cancelDistancePx] arms "cancel"
 *   up   -> TAP / release(cancelled?) / dragEnd
 *
 * Taps are counted: three quick taps (each within [tapGapMs] of the previous one) are a TRIPLE TAP, which switches the
 * bubble between "fixed on WhatsApp's mic" and "move anywhere". A single / double tap is reported only once the
 * window has passed, so the first tap of a triple tap never shows a hint. Holding or dragging resets the count.
 */
class BubbleGesture(
    private val scheduler: Scheduler,
    private val listener: Listener,
    private val slopPx: Float,
    private val cancelDistancePx: Float,
    private val holdDelayMs: Long = 200L,
    private val tapGapMs: Long = 350L,
) {
    fun interface Cancelable { fun cancel() }
    fun interface Scheduler { fun postDelayed(delayMs: Long, action: () -> Unit): Cancelable }

    interface Listener {
        fun onHoldStart()
        fun onCancelArmed(armed: Boolean)
        fun onRelease(cancelled: Boolean)
        fun onDrag(dx: Float, dy: Float)
        fun onDragEnd()
        fun onTap()
        /** Three quick taps: toggle fixed / movable. */
        fun onTripleTap() {}
    }

    enum class Mode { IDLE, PENDING, HOLDING, DRAGGING }

    /**
     * When false (bubble sits on WhatsApp's own mic button) the bubble never moves: touching and then moving
     * before the hold delay simply starts recording, exactly like WhatsApp's button.
     */
    var allowDrag = true

    var mode = Mode.IDLE
        private set
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var armed = false
    private var timer: Cancelable? = null
    private var taps = 0
    private var tapTimer: Cancelable? = null

    fun down(x: Float, y: Float) {
        reset()
        downX = x; downY = y; lastX = x; lastY = y
        mode = Mode.PENDING
        timer = scheduler.postDelayed(holdDelayMs) {
            if (mode == Mode.PENDING) {
                mode = Mode.HOLDING
                clearTaps()
                listener.onHoldStart()
            }
        }
    }

    fun move(x: Float, y: Float) {
        when (mode) {
            Mode.PENDING -> {
                val dx = x - downX
                val dy = y - downY
                if (dx * dx + dy * dy > slopPx * slopPx) {
                    timer?.cancel()
                    clearTaps()
                    if (allowDrag) {
                        mode = Mode.DRAGGING
                        listener.onDrag(x - lastX, y - lastY)
                        lastX = x; lastY = y
                    } else {
                        mode = Mode.HOLDING
                        listener.onHoldStart()
                        val nowArmed = (x - downX) < -cancelDistancePx
                        if (nowArmed != armed) { armed = nowArmed; listener.onCancelArmed(armed) }
                    }
                }
            }
            Mode.DRAGGING -> {
                listener.onDrag(x - lastX, y - lastY)
                lastX = x; lastY = y
            }
            Mode.HOLDING -> {
                val nowArmed = (x - downX) < -cancelDistancePx
                if (nowArmed != armed) {
                    armed = nowArmed
                    listener.onCancelArmed(armed)
                }
            }
            Mode.IDLE -> Unit
        }
    }

    fun up() {
        timer?.cancel()
        when (mode) {
            Mode.PENDING -> countTap()
            Mode.HOLDING -> listener.onRelease(armed)
            Mode.DRAGGING -> listener.onDragEnd()
            Mode.IDLE -> Unit
        }
        reset()
    }

    /** System cancelled the touch stream (e.g. window removed): never send a half recording. */
    fun cancelEvent() {
        timer?.cancel()
        when (mode) {
            Mode.HOLDING -> listener.onRelease(true)
            Mode.DRAGGING -> listener.onDragEnd()
            else -> Unit
        }
        reset()
    }

    private fun countTap() {
        tapTimer?.cancel()
        taps++
        if (taps >= 3) {
            clearTaps()
            listener.onTripleTap()
            return
        }
        tapTimer = scheduler.postDelayed(tapGapMs) {
            if (taps > 0) { clearTaps(); listener.onTap() }
        }
    }

    private fun clearTaps() {
        tapTimer?.cancel(); tapTimer = null
        taps = 0
    }

    private fun reset() {
        timer?.cancel()
        timer = null
        mode = Mode.IDLE
        armed = false
    }
}
