package com.kaafisir.auramic.automation

/**
 * Whether the accessibility service itself can see a chat app in front. It is a second opinion next to the usage
 * events ([com.kaafisir.auramic.service.overlay.ForegroundAppDetector]): on some phones those events come late or
 * not at all, and the floating mic then never showed up - or vanished - for no visible reason.
 *
 * The service only receives events from the chat apps, so an event is positive proof. "Gone" needs more care: the
 * keyboard or a system window can be the active window for a moment while the chat app is still in front, so the
 * chat app must be missing from two reads in a row, for at least [clearAfterMs].
 */
class A11yPresence(private val supported: Set<String>, private val clearAfterMs: Long = 700, private val staleMs: Long = 5_000) {
    /** The chat app seen in front, or null. */
    var packageName: String? = null
        private set
    private var lastSeen = 0L
    private var otherReads = 0

    fun reset() { packageName = null; lastSeen = 0L; otherReads = 0 }

    /** A window-state event from [pkg] (one of the chat apps) arrived. */
    fun onEvent(pkg: String, now: Long) {
        if (pkg !in supported) return
        packageName = pkg; lastSeen = now; otherReads = 0
    }

    /** Result of reading the active window: its package, or null when no window could be read. */
    fun onRead(rootPackage: String?, now: Long) {
        when {
            rootPackage != null && rootPackage in supported -> { packageName = rootPackage; lastSeen = now; otherReads = 0 }
            rootPackage != null -> {
                otherReads++
                if (otherReads >= 2 && now - lastSeen >= clearAfterMs) packageName = null
            }
            else -> if (packageName != null && now - lastSeen > staleMs) packageName = null   // nothing readable for a long time
        }
    }
}
