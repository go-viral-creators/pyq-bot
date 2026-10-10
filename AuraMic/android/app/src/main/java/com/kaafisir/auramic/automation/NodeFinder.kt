package com.kaafisir.auramic.automation

import com.kaafisir.auramic.automation.WhatsAppUi.normalize

/** Stateless lookups over a [UiNode] snapshot. Each returns null rather than guessing wildly. */
object NodeFinder {

    private fun UiNode.hasAnyId(names: List<String>) = names.any { hasId(it) }

    private fun screenHeight(root: UiNode): Int = max(root.bounds.bottom, root.walk().maxOfOrNull { it.bounds.bottom } ?: 0)
    private fun screenWidth(root: UiNode): Int = max(root.bounds.right, root.walk().maxOfOrNull { it.bounds.right } ?: 0)
    private fun max(a: Int, b: Int) = if (a > b) a else b

    /**
     * True on a conversation screen: the app's own message box is present. Known ids first; otherwise any editable
     * node in the lower half of the screen (works for Telegram / Instagram too).
     */
    fun hasComposer(root: UiNode): Boolean {
        if (root.walk().any { it.hasAnyId(WhatsAppUi.COMPOSER_IDS) }) return true
        val h = screenHeight(root)
        return h > 0 && root.walk().any { it.editable && it.bounds.top > h * 0.55 }
    }

    /** Title of the open conversation (contact or group name), or null if not on a chat screen. */
    fun chatTitle(root: UiNode, screenHeight: Int): String? {
        root.walk().firstOrNull { it.hasAnyId(WhatsAppUi.CHAT_TITLE_IDS) && !it.text.isNullOrBlank() }
            ?.let { return it.text!!.trim() }
        // Fallback when ids changed / other apps: the biggest text in the top toolbar area is the name.
        val h = if (screenHeight > 0) screenHeight else screenHeight(root)
        if (h <= 0 || !hasComposer(root)) return null
        val limit = h * 0.17
        return root.walk()
            .filter { !it.text.isNullOrBlank() && it.bounds.height > 0 && it.bounds.top < limit && !it.editable && (it.text?.length ?: 0) <= 60 }
            .maxByOrNull { it.bounds.height }
            ?.text?.trim()
    }

    /**
     * The "Send" button of the media preview / confirmation dialog. Strategies, strictest first:
     * ids, exact labels in many languages, short "Send ..." labels, and (only when [allowPositional]) an
     * icon-only button in the lower-right corner.
     */
    fun sendButton(root: UiNode, allowPositional: Boolean = false): UiNode? {
        root.walk().firstOrNull { it.hasAnyId(WhatsAppUi.SEND_IDS) && it.enabled }?.clickableSelfOrAncestor()?.let { return it }
        root.walk().firstOrNull { n ->
            n.enabled && !n.editable && (matchesWord(n.desc, WhatsAppUi.SEND_WORDS) || matchesWord(n.text, WhatsAppUi.SEND_WORDS))
        }?.let { n -> return n.clickableSelfOrAncestor() ?: n.takeIf { it.bounds.width > 0 } }
        root.walk().firstOrNull { n ->
            n.enabled && !n.editable && (startsWithSend(n.text) || startsWithSend(n.desc)) && n.clickableSelfOrAncestor() != null
        }?.let { return it.clickableSelfOrAncestor() }
        if (!allowPositional) return null
        val w = screenWidth(root); val h = screenHeight(root)
        if (w <= 0 || h <= 0) return null
        return root.walk()
            .filter { n ->
                n.enabled && n.clickable && !n.editable && n.bounds.width in 1..(w * 0.3).toInt() && n.bounds.height in 1..(w * 0.3).toInt() &&
                    n.bounds.centerX() > w * 0.6 && n.bounds.centerY() > h * 0.7 &&
                    (n.className?.let { it.endsWith("ImageButton") || it.endsWith("ImageView") || it.endsWith("FloatingActionButton") || it.endsWith("Button") } ?: false)
            }
            .maxByOrNull { it.bounds.centerX() + it.bounds.centerY() }
    }

    private fun Bounds.centerX() = (left + right) / 2
    private fun Bounds.centerY() = (top + bottom) / 2

    private fun matchesWord(value: String?, words: Set<String>): Boolean {
        val v = normalize(value)
        return v.isNotEmpty() && v in words
    }

    private fun startsWithSend(value: String?): Boolean {
        val v = normalize(value)
        if (v.isEmpty() || v.length > 14) return false
        return WhatsAppUi.SEND_WORDS.any { w -> w.length >= 4 && (v.startsWith("$w ") || v.startsWith("$w(")) }
    }

    /** True when the share target picker (list of chats) is on screen. */
    fun isPicker(root: UiNode): Boolean = root.walk().any { it.hasAnyId(WhatsAppUi.PICKER_ROW_NAME_IDS) }

    /**
     * The row for [chatName] in the picker list; exact match first, then an unambiguous prefix, then (apps whose ids we
     * do not know, e.g. Telegram) any text node with exactly that text outside the conversation toolbar.
     */
    fun chatRow(root: UiNode, chatName: String): UiNode? {
        val target = normalize(chatName)
        if (target.isEmpty()) return null
        val candidates = root.walk().filter { it.hasAnyId(WhatsAppUi.PICKER_ROW_NAME_IDS) && !it.text.isNullOrBlank() }.toList()
        val byId = candidates.firstOrNull { normalize(it.text) == target }
            ?: candidates.filter { normalize(it.text).startsWith(target) }.singleOrNull()
        if (byId != null) return byId.clickableSelfOrAncestor() ?: byId
        if (candidates.isNotEmpty() || hasComposer(root)) return null   // ids known but no match, or still on the chat screen
        val h = screenHeight(root)
        val node = root.walk().firstOrNull {
            !it.editable && it.bounds.height > 0 && it.bounds.top > h * 0.08 &&
                (normalize(it.text) == target || normalize(it.desc) == target)
        } ?: return null
        return node.clickableSelfOrAncestor() ?: node       // tap fallback uses the node's own bounds
    }

    /** First chat row of the picker (most recent chat) - only used when the user opted in. */
    fun firstRow(root: UiNode): UiNode? =
        root.walk().firstOrNull { it.hasAnyId(WhatsAppUi.PICKER_ROW_NAME_IDS) && !it.text.isNullOrBlank() }
            ?.let { it.clickableSelfOrAncestor() ?: it }

    fun searchButton(root: UiNode): UiNode? {
        val n = root.walk().firstOrNull {
            it.hasAnyId(WhatsAppUi.SEARCH_BUTTON_IDS) || (it.clickable && matchesWord(it.desc, WhatsAppUi.SEARCH_WORDS))
        }
        return n?.clickableSelfOrAncestor() ?: n
    }

    /**
     * The picker's search box. Matched by id; a generic EditText is accepted only while the chat picker is on screen -
     * never on a conversation screen, where the only EditText is the user's own message box (typing there would
     * corrupt their draft).
     */
    fun searchInput(root: UiNode): UiNode? {
        root.walk().firstOrNull { it.editable && it.hasAnyId(WhatsAppUi.SEARCH_INPUT_IDS) }?.let { return it }
        if (!isPicker(root)) return null
        return root.walk().firstOrNull { it.editable && !it.hasAnyId(WhatsAppUi.COMPOSER_IDS) && it.className?.endsWith("EditText") == true }
    }

    fun scrollable(root: UiNode): UiNode? = root.walk().firstOrNull { it.scrollable }
}
