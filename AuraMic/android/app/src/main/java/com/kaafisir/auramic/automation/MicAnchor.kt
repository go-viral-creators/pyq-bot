package com.kaafisir.auramic.automation

import kotlin.math.min

/** Where WhatsApp's own round voice button is on screen. [isMic] is false while it shows "Send" (text typed). */
data class MicAnchor(val bounds: Bounds, val isMic: Boolean) {
    val centerX: Int get() = (bounds.left + bounds.right) / 2
    val centerY: Int get() = (bounds.top + bounds.bottom) / 2
    /** Diameter of the round button (touch targets are often square, so the shorter side). */
    val size: Int get() = min(bounds.width, bounds.height)
}

/**
 * Finds WhatsApp's voice (mic) button so AuraMic's bubble can sit exactly on top of it.
 * Pure Kotlin over [UiNode] snapshots (the live service uses [classify] on a few id lookups for speed).
 */
object MicAnchorFinder {
    enum class Kind { MIC, SEND }

    /** Resource-id suffixes of the mic button, newest known first. */
    val MIC_IDS = listOf("voice_note_btn", "conversation_entry_action_button", "voice_note_button", "ptt_button")
    /** Resource-id suffixes of the same button once it turned into "Send". */
    val SEND_ONLY_IDS = listOf("send", "send_btn", "send_button")

    /** Exact accessible labels of the voice button (never substrings: "Audio call", "Microphone access" etc. must not match). */
    private val MIC_LABELS = listOf(
        "voice message", "voice note", "record voice message", "mensaje de voz", "nota de voz", "mensagem de voz", "grabar mensaje de voz",
        "gravar mensagem de voz", "pesan suara", "rekam pesan suara", "वॉइस संदेश", "वॉयस मैसेज", "वॉइस मैसेज", "रिकॉर्ड", "رسالة صوتية", "تسجيل رسالة صوتية",
    ).map { WhatsAppUi.normalize(it) }.toSet()

    /** Decides what a candidate button is, or null when it is neither. */
    fun classify(viewId: String?, desc: String?, text: String?): Kind? {
        val id = viewId?.substringAfter(":id/", "")?.takeIf { it.isNotEmpty() }
        val label = WhatsAppUi.normalize(desc ?: text)
        val sendLabel = label in WhatsAppUi.SEND_WORDS
        if (id != null && id in MIC_IDS) return if (sendLabel) Kind.SEND else Kind.MIC
        if (id != null && id in SEND_ONLY_IDS) return Kind.SEND
        if (label in WhatsAppUi.SEND_WORDS && id == null) return null          // a bare "Send" label is not specific enough
        if (label in MIC_LABELS) return Kind.MIC
        return null
    }

    /** Whether [b] is plausible for the composer button: lower part of the screen, a few dp to ~25 % of the width. */
    fun plausible(b: Bounds, screenW: Int, screenH: Int): Boolean =
        b.width > 0 && b.height > 0 && b.centerY() > screenH * 0.45f && b.width < screenW * 0.3f && b.height < screenW * 0.3f &&
            b.width > screenW * 0.05f && b.height > screenW * 0.05f

    private fun Bounds.centerY() = (top + bottom) / 2

    /** Searches a snapshot; mic wins over send when both exist. */
    fun find(root: UiNode, screenW: Int, screenH: Int): MicAnchor? {
        var send: MicAnchor? = null
        val hasComposer = root.walk().any { it.editable }
        for (n in root.walk()) {
            if (!n.enabled && n.viewId == null) continue
            val kind = classify(n.viewId, n.desc, n.text) ?: continue
            // A label alone is not enough (chat list, status, calls have similar buttons): a message box must be on screen too.
            val byId = n.viewId?.substringAfter(":id/", "")?.let { it in MIC_IDS || it in SEND_ONLY_IDS } == true
            if (!byId && !hasComposer) continue
            if (!plausible(n.bounds, screenW, screenH)) continue
            // The labelled node is sometimes an inner icon: use the clickable ancestor's bounds when it is a plausible button.
            val b = n.clickableSelfOrAncestor()?.bounds?.takeIf { plausible(it, screenW, screenH) } ?: n.bounds
            val a = MicAnchor(b, kind == Kind.MIC)
            if (a.isMic) return a
            if (send == null) send = a
        }
        return send
    }
}

/**
 * Remembers the last mic position across a missed read, so the bubble does not jump during screen transitions - but
 * only as long as it makes sense:
 *  - the chat window could not be read at all (transition, tree being rebuilt): keep it [unreadableHoldMs];
 *  - the window WAS read and has no mic / conversation (chat list, status, another app): the user left the chat,
 *    keep it only [leftHoldMs] so the bubble disappears right away.
 */
class AnchorHolder(private val unreadableHoldMs: Long = 1_200, private val leftHoldMs: Long = 150) {
    var last: MicAnchor? = null
        private set
    private var lastAt = 0L

    /** True while [update] is returning a remembered anchor instead of a fresh one. */
    var holding = false
        private set

    /** Forget the remembered position at once (the user left the chat). */
    fun reset() { last = null; holding = false }

    /** [seen] = anchor found by this read, [windowRead] = the app window's tree could be read. */
    fun update(seen: MicAnchor?, windowRead: Boolean, now: Long): MicAnchor? {
        if (seen != null) { last = seen; lastAt = now; holding = false; return seen }
        val hold = if (windowRead) leftHoldMs else unreadableHoldMs
        val a = last?.takeIf { now - lastAt < hold }
        if (a == null) last = null
        holding = a != null
        return a
    }
}
