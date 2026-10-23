package com.kaafisir.auramic.automation

/**
 * Chats that WhatsApp lists under a different name than its header shows (the same number saved twice in the phone's
 * contacts: "Ravi Mourya" in the chat, "Guru Mourya" in the share list). When the automatic search fails and the user
 * picks the chat by hand and sends, the name they picked is noted here against the chat's own name.
 *
 * A name is trusted only after the *same* pick was seen [threshold] times (default 2): one slip of the finger can never
 * redirect later voice notes to the wrong person. Pure logic (unit tested); [serialize] / constructor persist it.
 * Everything stays on the phone.
 */
class ChatAliasBook(serialized: String? = null, private val threshold: Int = 2, private val maxChats: Int = 100) {
    private class Entry(val alias: String, var count: Int, var active: Boolean)

    /** Cleaned header name of the chat -> names the picker showed for it. Insertion order = oldest first. */
    private val chats = LinkedHashMap<String, MutableList<Entry>>()

    init {
        serialized?.lineSequence()?.forEach { line ->
            val p = line.split('\t')
            if (p.size == 4) {
                val count = p[2].toIntOrNull() ?: return@forEach
                chats.getOrPut(p[0]) { mutableListOf() }.add(Entry(p[1], count, p[3] == "1"))
            }
        }
    }

    /** Names (as the picker shows them) trusted for [key]'s chat. */
    fun aliasesFor(key: ChatKey): List<String> = chats[key.clean.first()]?.filter { it.active }?.map { it.alias }.orEmpty()

    /** [key] with its trusted names added, ready for [SendFlow]. */
    fun augment(key: ChatKey): ChatKey = key.withAliases(aliasesFor(key))

    /**
     * The user sent a voice note to the picker row [pickedName] for [key]'s chat. Returns true when this made the name
     * trusted just now (so the caller can say "next time it is automatic").
     */
    fun observe(key: ChatKey, pickedName: String): Boolean {
        val picked = pickedName.replace('\t', ' ').replace('\n', ' ').trim()
        val cleanPicked = ChatNameMatcher.clean(picked)
        if (cleanPicked.isEmpty() || key.clean.contains(cleanPicked)) return false       // nothing new to learn
        val id = key.clean.first()
        val list = chats.remove(id) ?: mutableListOf()
        chats[id] = list                                                                  // most recent last
        val e = list.firstOrNull { ChatNameMatcher.clean(it.alias) == cleanPicked } ?: Entry(picked, 0, false).also { list += it }
        e.count++
        val becameActive = !e.active && e.count >= threshold
        if (becameActive) e.active = true
        while (chats.size > maxChats) chats.remove(chats.keys.first())
        return becameActive
    }

    /** Number of trusted names. */
    fun activeCount(): Int = chats.values.sumOf { l -> l.count { it.active } }

    fun clear() = chats.clear()

    fun serialize(): String = chats.entries.joinToString("\n") { (id, list) ->
        list.joinToString("\n") { "$id\t${it.alias}\t${it.count}\t${if (it.active) 1 else 0}" }
    }
}
