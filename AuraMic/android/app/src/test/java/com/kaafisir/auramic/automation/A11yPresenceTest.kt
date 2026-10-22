package com.kaafisir.auramic.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class A11yPresenceTest {
    private val wa = "com.whatsapp"
    private fun presence() = A11yPresence(setOf(wa, "com.whatsapp.w4b"))

    @Test fun anEventFromTheChatAppIsProof() {
        val p = presence()
        p.onEvent(wa, 100)
        assertEquals(wa, p.packageName)
    }

    @Test fun otherPackagesAreIgnored() {
        val p = presence()
        p.onEvent("com.example", 100)
        assertNull(p.packageName)
    }

    @Test fun oneStrangeReadDoesNotMeanGone() {          // keyboard / system window as the active window for a moment
        val p = presence()
        p.onEvent(wa, 0)
        p.onRead("com.google.android.inputmethod.latin", 800)
        assertEquals(wa, p.packageName)
    }

    @Test fun twoReadsOfAnotherAppOverTimeClearIt() {
        val p = presence()
        p.onEvent(wa, 0)
        p.onRead("com.miui.home", 500)
        p.onRead("com.miui.home", 1_000)
        assertNull(p.packageName)
    }

    @Test fun twoQuickReadsAreNotEnough() {
        val p = presence()
        p.onEvent(wa, 0)
        p.onRead("com.miui.home", 100)
        p.onRead("com.miui.home", 200)
        assertEquals(wa, p.packageName)                   // less than clearAfterMs since the last proof
    }

    @Test fun aSuccessfulReadRefreshesAndResetsTheCount() {
        val p = presence()
        p.onEvent(wa, 0)
        p.onRead("com.miui.home", 900)
        p.onRead(wa, 950)
        p.onRead("com.miui.home", 1_000)
        assertEquals(wa, p.packageName)
    }

    @Test fun unreadableForAWhileExpires() {
        val p = presence()
        p.onEvent(wa, 0)
        p.onRead(null, 4_000)
        assertEquals(wa, p.packageName)
        p.onRead(null, 5_500)
        assertNull(p.packageName)
    }
}
