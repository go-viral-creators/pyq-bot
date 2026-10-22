package com.kaafisir.auramic.service.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundTrackerTest {
    private val R = ResumedActivityTracker.RESUMED
    private val P = ResumedActivityTracker.PAUSED
    private val wa = "com.whatsapp"

    @Test fun lastResumedWins() {
        val t = ForegroundTracker()
        t.onEvent(1_000, R, "com.miui.home", "Launcher")
        t.onEvent(2_000, R, wa, "Home")
        assertEquals(wa, t.packageName)
    }

    @Test fun anEventThatArrivesLateStillEndsInTheRightState() {
        val t = ForegroundTracker()
        t.onEvent(1_000, R, wa, "Home")
        t.onEvent(3_000, P, wa, "Home")
        t.onEvent(3_100, R, "com.miui.home", "Launcher")
        t.onEvent(2_000, R, wa, "Conversation")           // late: older than the events already seen
        // order by time: R Home, R Conversation, P Home (ignored: not the front one), R launcher
        assertEquals("com.miui.home", t.packageName)
    }

    @Test fun theOldActivitysPauseAfterTheNewOnesResumeDoesNotHideTheApp() {
        val t = ForegroundTracker()
        t.onEvent(1_000, R, wa, "Home")
        t.onEvent(2_000, R, wa, "Conversation")
        t.onEvent(2_010, P, wa, "Home")
        assertEquals(wa, t.packageName)
    }

    @Test fun readingTheSameWindowAgainChangesNothing() {
        val t = ForegroundTracker()
        repeat(5) {
            t.onEvent(1_000, R, wa, "Conversation")
            t.onEvent(1_500, P, wa, "Conversation")
        }
        assertNull(t.packageName)
        repeat(5) { t.onEvent(1_000, R, wa, "Conversation") }
        assertNull(t.packageName)                          // still paused: duplicates did not re-apply the old RESUMED
    }

    @Test fun aLongStayInOneAppIsNotForgotten() {
        val t = ForegroundTracker(keepMs = 15_000)
        t.onEvent(1_000, R, wa, "Conversation")
        t.prune(now = 120_000)                             // far outside the window
        assertEquals(wa, t.packageName)
    }

    @Test fun resetForgetsEverything() {
        val t = ForegroundTracker()
        t.onEvent(1_000, R, wa, "Conversation")
        t.reset()
        assertNull(t.packageName)
    }
}
