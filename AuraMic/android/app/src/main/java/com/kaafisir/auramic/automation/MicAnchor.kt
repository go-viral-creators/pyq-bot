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

    private val MIC_WORDS = listOf(
        "voice message", "voice note", "record", "microphone", "mic", "mensaje de voz", "nota de voz", "mensagem de voz", "áudio", "audio",
        "suara", "pesan suara", "आवाज़", "वॉइस", "आवाज", "صوت", "صوتية", "ميكروفون",
    ).map { WhatsAppUi.normalize(it) }

    /** Decides what a candidate button is, or null when it is neither. */
    fun classify(viewId: String?, desc: String?, text: String?): Kind? {
        val id = viewId?.substringAfter(":id/", "")?.takeIf { it.isNotEmpty() }
        val label = WhatsAppUi.normalize(desc ?: text)
        val sendLabel = label in WhatsAppUi.SEND_WORDS
        if (id != null && id in MIC_IDS) return if (sendLabel) Kind.SEND else Kind.MIC
        if (id != null && id in SEND_ONLY_IDS) return Kind.SEND
        if (label.isNotEmpty() && MIC_WORDS.any { label.contains(it) }) return Kind.MIC
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
        for (n in root.walk()) {
            if (!n.enabled && n.viewId == null) continue
            val kind = classify(n.viewId, n.desc, n.text) ?: continue
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
