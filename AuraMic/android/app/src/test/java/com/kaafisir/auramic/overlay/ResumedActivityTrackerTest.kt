package com.kaafisir.auramic.overlay

import com.kaafisir.auramic.service.overlay.ResumedActivityTracker
import com.kaafisir.auramic.service.overlay.ResumedActivityTracker.Companion.PAUSED
import com.kaafisir.auramic.service.overlay.ResumedActivityTracker.Companion.RESUMED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResumedActivityTrackerTest {
    private val wa = "com.whatsapp"

    @Test fun openingAChatWithResumeBeforePauseKeepsWhatsAppInFront() {
        val t = ResumedActivityTracker()
        t.onEvent(RESUMED, wa, "com.whatsapp.HomeActivity")
        // Android 10+ order: the conversation resumes first, then the chat list pauses.
        t.onEvent(RESUMED, wa, "com.whatsapp.Conversation")
        t.onEvent(PAUSED, wa, "com.whatsapp.HomeActivity")
        assertEquals(wa, t.packageName)
    }

    @Test fun classicPauseThenResumeOrder() {
        val t = ResumedActivityTracker()
        t.onEvent(RESUMED, wa, "com.whatsapp.HomeActivity")
        t.onEvent(PAUSED, wa, "com.whatsapp.HomeActivity")
        t.onEvent(RESUMED, wa, "com.whatsapp.Conversation")
        assertEquals(wa, t.packageName)
    }

    @Test fun leavingWhatsAppClearsIt() {
        val t = ResumedActivityTracker()
        t.onEvent(RESUMED, wa, "com.whatsapp.Conversation")
        t.onEvent(PAUSED, wa, "com.whatsapp.Conversation")
        assertNull(t.packageName)
        t.onEvent(RESUMED, wa, "com.whatsapp.Conversation")
        t.onEvent(RESUMED, "com.android.launcher", "Launcher")
        t.onEvent(PAUSED, wa, "com.whatsapp.Conversation")       // late pause of the old app changes nothing
        assertEquals("com.android.launcher", t.packageName)
    }

    @Test fun eventsWithoutClassNameStillWork() {
        val t = ResumedActivityTracker()
        t.onEvent(RESUMED, wa, null)
        t.onEvent(PAUSED, wa, null)
        assertNull(t.packageName)
    }
}
