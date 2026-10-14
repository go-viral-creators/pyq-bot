package com.kaafisir.auramic.overlay

import com.kaafisir.auramic.service.overlay.BubbleVisibility
import com.kaafisir.auramic.service.overlay.BubbleVisibility.Decision
import org.junit.Assert.assertEquals
import org.junit.Test

class BubbleVisibilityTest {
    private fun BubbleVisibility.d(app: Boolean = true, chat: Boolean, anchored: Boolean, connected: Boolean = true) = decide(app, chat, anchored, connected)

    @Test fun anchoredBubbleGoesAwayWhenTheMicIsGoneEvenIfTheChatLooksOpen() {
        val v = BubbleVisibility()
        assertEquals(Decision.Show, v.d(chat = true, anchored = true))
        // leaving the chat: mic gone, but the old conversation is still readable for a moment -> must NOT float to the middle
        assertEquals(Decision.Hide, v.d(chat = true, anchored = false))
        assertEquals(Decision.Hide, v.d(chat = true, anchored = false))
        assertEquals(Decision.Hide, v.d(chat = false, anchored = false))
    }

    @Test fun enteringAChatWithoutAFindableMicStillGetsTheFloatingFallback() {
        val v = BubbleVisibility()
        assertEquals(Decision.ShowAfterGrace, v.d(chat = true, anchored = false))
        assertEquals(Decision.Show, v.d(chat = true, anchored = true))
    }

    @Test fun nextChatStartsFresh() {
        val v = BubbleVisibility()
        v.d(chat = true, anchored = true)
        v.d(chat = false, anchored = false)                          // chat list resets
        assertEquals(Decision.ShowAfterGrace, v.d(chat = true, anchored = false))
    }

    @Test fun leavingWhatsAppOrAccessibilityOff() {
        val v = BubbleVisibility()
        v.d(chat = true, anchored = true)
        assertEquals(Decision.Hide, v.d(app = false, chat = false, anchored = false))
        assertEquals(Decision.Show, v.d(chat = false, anchored = false, connected = false))   // cannot tell: follow WhatsApp
    }
}
