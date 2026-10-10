package com.kaafisir.auramic.automation

/**
 * Compact, privacy-conscious text dump of a screen structure, used by "Copy diagnostics" so a failing auto-send can be
 * debugged from the real WhatsApp / Telegram layout. Texts are cut to a few characters.
 */
object NodeDump {
    fun format(root: UiNode, maxNodes: Int = 160, maxText: Int = 6): String {
        val sb = StringBuilder()
        var count = 0
        fun cut(s: String?) = s?.replace('\n', ' ')?.let { if (it.length > maxText) it.take(maxText) + "~${it.length}" else it } ?: ""
        fun walk(n: UiNode, depth: Int) {
            if (count >= maxNodes) return
            count++
            val flags = buildString {
                if (n.clickable) append('C'); if (n.editable) append('E'); if (n.scrollable) append('S'); if (!n.enabled) append('d')
            }
            sb.append("  ".repeat(depth.coerceAtMost(12)))
                .append(n.className?.substringAfterLast('.') ?: "?")
                .append(" id=").append(n.viewId?.substringAfter(":id/") ?: "-")
                .append(" t=\"").append(cut(n.text)).append("\" d=\"").append(cut(n.desc)).append("\"")
                .append(" [").append(flags).append("] ")
                .append(n.bounds.left).append(',').append(n.bounds.top).append(',').append(n.bounds.right).append(',').append(n.bounds.bottom)
                .append('\n')
            n.children.forEach { walk(it, depth + 1) }
        }
        walk(root, 0)
        if (count >= maxNodes) sb.append("... truncated at ").append(maxNodes).append(" nodes\n")
        return sb.toString()
    }
}
