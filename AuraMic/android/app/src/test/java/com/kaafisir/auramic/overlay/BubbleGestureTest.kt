package com.kaafisir.auramic.overlay

import com.kaafisir.auramic.service.overlay.BubbleGesture
import org.junit.Assert.assertEquals
import org.junit.Test

class BubbleGestureTest {
    private val events = mutableListOf<String>()
    private val pending = mutableListOf<Pair<Long, () -> Unit>>()
    private val scheduler = BubbleGesture.Scheduler { d, a ->
        val entry = d to a
        pending += entry
        BubbleGesture.Cancelable { pending.remove(entry) }
    }
    private val listener = object : BubbleGesture.Listener {
        override fun onHoldStart() { events += "hold" }
        override fun onCancelArmed(armed: Boolean) { events += "armed=$armed" }
        override fun onRelease(cancelled: Boolean) { events += "release(cancel=$cancelled)" }
        override fun onDrag(dx: Float, dy: Float) { events += "drag($dx,$dy)" }
        override fun onDragEnd() { events += "dragEnd" }
        override fun onTap() { events += "tap" }
    }
    private fun gesture() = BubbleGesture(scheduler, listener, slopPx = 10f, cancelDistancePx = 100f)
    private fun fireTimer() { pending.toList().forEach { it.second() }; pending.clear() }

    @Test fun quickTouchIsATap() {
        val g = gesture(); g.down(0f, 0f); g.up()
        assertEquals(listOf("tap"), events)
    }

    @Test fun holdThenReleaseSends() {
        val g = gesture(); g.down(0f, 0f); fireTimer(); g.up()
        assertEquals(listOf("hold", "release(cancel=false)"), events)
    }

    @Test fun slidingLeftArmsCancelAndRelease() {
        val g = gesture(); g.down(500f, 0f); fireTimer()
        g.move(450f, 0f)          // within threshold
        g.move(380f, 5f)          // beyond 100px left
        g.move(300f, 5f)          // still armed: no duplicate event
        g.up()
        assertEquals(listOf("hold", "armed=true", "release(cancel=true)"), events)
    }

    @Test fun slidingBackDisarmsCancel() {
        val g = gesture(); g.down(500f, 0f); fireTimer()
        g.move(350f, 0f); g.move(480f, 0f); g.up()
        assertEquals(listOf("hold", "armed=true", "armed=false", "release(cancel=false)"), events)
    }

    @Test fun movingBeforeHoldDelayDragsTheBubble() {
        val g = gesture(); g.down(0f, 0f); g.move(30f, 0f); g.move(40f, 10f); g.up()
        assertEquals(listOf("drag(30.0,0.0)", "drag(10.0,10.0)", "dragEnd"), events)
        assertEquals(0, pending.size) // hold timer was cancelled
    }

    @Test fun systemCancelWhileRecordingDiscards() {
        val g = gesture(); g.down(0f, 0f); fireTimer(); g.cancelEvent()
        assertEquals(listOf("hold", "release(cancel=true)"), events)
    }

    @Test fun upRightAfterSlopDragDoesNotRecord() {
        val g = gesture(); g.down(0f, 0f); g.move(50f, 0f); fireTimer(); g.up()
        assertEquals(listOf("drag(50.0,0.0)", "dragEnd"), events)
    }
}
