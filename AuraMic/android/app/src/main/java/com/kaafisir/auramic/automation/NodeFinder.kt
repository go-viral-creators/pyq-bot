package com.kaafisir.auramic.automation

import com.kaafisir.auramic.automation.WhatsAppUi.normalize

/** Stateless lookups over a [UiNode] snapshot. Each returns null rather than guessing wildly. */
object NodeFinder {

    private fun UiNode.hasAnyId(names: List<String>) = names.any { hasId(it) }

    /** Title of the open conversation (contact or group name), or null if not on a chat screen. */
    fun chatTitle(root: UiNode, screenHeight: Int): String? {
        root.walk().firstOrNull { it.hasAnyId(WhatsAppUi.CHAT_TITLE_IDS) && !it.text.isNullOrBlank() }
            ?.let { return it.text!!.trim() }
        // Fallback when ids changed: the biggest text in the top toolbar area is the name.
        if (screenHeight <= 0) return null
        val limit = screenHeight * 0.16
        val hasComposer = root.walk().any { it.hasId("entry") || it.hasId("voice_note_btn") || it.hasId("conversation_entry_container") }
        if (!hasComposer) return null
        return root.walk()
            .filter { !it.text.isNullOrBlank() && it.bounds.height > 0 && it.bounds.top < limit && !it.editable && ((it.text?.length ?: 0) <= 60) }
            .maxByOrNull { it.bounds.height }
            ?.text?.trim()
    }

    /** The "Send" button of the media preview / confirmation dialog, clickable node or null. */
    fun sendButton(root: UiNode): UiNode? {
        val byId = root.walk().firstOrNull { it.hasAnyId(WhatsAppUi.SEND_IDS) && it.enabled }
        byId?.clickableSelfOrAncestor()?.let { return it }
        val byLabel = root.walk().firstOrNull { n ->
            n.enabled && !n.editable && (matchesWord(n.desc, WhatsAppUi.SEND_WORDS) || matchesWord(n.text, WhatsAppUi.SEND_WORDS))
        }
        return byLabel?.clickableSelfOrAncestor()
    }

    private fun matchesWord(value: String?, words: Set<String>): Boolean {
        val v = normalize(value)
        return v.isNotEmpty() && v in words
    }

    /** True when the share target picker (list of chats) is on screen. */
    fun isPicker(root: UiNode): Boolean =
        root.walk().any { it.hasAnyId(WhatsAppUi.PICKER_ROW_NAME_IDS) }

    /** The row for [chatName] in the picker list; exact match first, then "starts with". */
    fun chatRow(root: UiNode, chatName: String): UiNode? {
        val target = normalize(chatName)
        if (target.isEmpty()) return null
        val candidates = root.walk()
            .filter { it.hasAnyId(WhatsAppUi.PICKER_ROW_NAME_IDS) && !it.text.isNullOrBlank() }
            .toList()
        // Exact match wins. A prefix match (e.g. a truncated name) is accepted only if unambiguous,
        // so "Mom" can never silently pick "Mom 2".
        val hit = candidates.firstOrNull { normalize(it.text) == target }
            ?: candidates.filter { normalize(it.text).startsWith(target) }.singleOrNull()
            // Ids may vanish in a future release: with no id-matched rows at all, accept a plain
            // non-editable text node whose text is exactly the chat name.
            ?: if (candidates.isEmpty()) {
                root.walk().firstOrNull {
                    !it.editable && it.className?.endsWith("TextView") == true && normalize(it.text) == target &&
                        it.clickableSelfOrAncestor() != null
                }
            } else null
        return hit?.clickableSelfOrAncestor()
    }

    fun searchButton(root: UiNode): UiNode? {
        val n = root.walk().firstOrNull {
            it.hasAnyId(WhatsAppUi.SEARCH_BUTTON_IDS) || (it.clickable && matchesWord(it.desc, WhatsAppUi.SEARCH_WORDS))
        }
        return n?.clickableSelfOrAncestor() ?: n
    }

    /**
     * The picker's search box. Matched by id; a generic EditText is accepted only while the chat
     * picker is on screen - never on a conversation screen, where the only EditText is the user's
     * own message box (typing there would corrupt their draft).
     */
    fun searchInput(root: UiNode): UiNode? {
        root.walk().firstOrNull { it.editable && it.hasAnyId(WhatsAppUi.SEARCH_INPUT_IDS) }?.let { return it }
        if (!isPicker(root)) return null
        return root.walk().firstOrNull { it.editable && !it.hasId("entry") && it.className?.endsWith("EditText") == true }
    }

    fun scrollable(root: UiNode): UiNode? = root.walk().firstOrNull { it.scrollable }
}
