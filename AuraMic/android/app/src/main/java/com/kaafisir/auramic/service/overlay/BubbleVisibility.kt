package com.kaafisir.auramic.service.overlay

/**
 * When the floating mic is on screen. Pure logic (unit tested); [OverlayService] feeds it the latest signals.
 *
 * The important rule: a bubble that was sitting on WhatsApp's mic must go away the moment that mic is gone (the user
 * left the chat). Showing it in a free-floating spot instead - the "grace" fallback below - is only for *entering* a
 * chat whose mic button we cannot find, never for leaving one (that made the bubble hop to the middle of the screen
 * and stay there for seconds).
 */
class BubbleVisibility {
    sealed interface Decision {
        data object Hide : Decision
        data object Show : Decision
        /** Chat open but WhatsApp's voice button not found: show a floating bubble after a short grace period. */
        data object ShowAfterGrace : Decision
    }

    private var wasAnchored = false

    fun decide(inChatApp: Boolean, chatOpen: Boolean, anchored: Boolean, accessibilityConnected: Boolean): Decision {
        if (!inChatApp) { wasAnchored = false; return Decision.Hide }
        if (!accessibilityConnected) return Decision.Show          // no way to tell what is on screen: follow the app
        if (anchored) { wasAnchored = true; return Decision.Show }
        if (!chatOpen) { wasAnchored = false; return Decision.Hide } // chat list, status, calls, settings...
        return if (wasAnchored) Decision.Hide else Decision.ShowAfterGrace
    }
}
