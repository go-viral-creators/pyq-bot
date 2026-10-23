package com.kaafisir.auramic.automation

/**
 * The name(s) a chat is known by. The first line is the title of the open conversation; further lines are the other
 * texts of its header (a business account's verified name, the "~ push name" of an unsaved number...). Encoded as
 * lines of one string so it travels through [SendRequest.chatName] unchanged.
 */
class ChatKey private constructor(val lines: List<String>, private val headerLines: Int) {
    /** [lines] after [ChatNameMatcher.clean], same order. */
    val clean: List<String> = lines.map { ChatNameMatcher.clean(it) }
    /** Last 10 digits of a phone-number line (null for names), same order. */
    val phones: List<String?> = lines.map { ChatNameMatcher.phoneKey(it) }

    /**
     * What to type into the picker's search box, best first: the names remembered for this chat (they are the ones the
     * picker is known to show), then the header lines; each without emoji and symbols.
     */
    val searchQueries: List<String> = (lines.drop(headerLines) + lines.take(headerLines))
        .mapIndexed { i, l -> queryOf(l).ifEmpty { clean.getOrElse(i) { "" } } }
        .filter { it.isNotEmpty() }.distinct().ifEmpty { listOf(clean.first()) }

    /** The same chat with extra names it is known by (see [ChatAliasBook]); they match like further header lines. */
    fun withAliases(extra: List<String>): ChatKey {
        val add = extra.filter { e -> ChatNameMatcher.clean(e).let { it.isNotEmpty() && it !in clean } }.distinct()
        return if (add.isEmpty()) this else ChatKey(lines + add, headerLines)
    }

    /** Lines joined for [SendRequest.chatName]; remembered names carry a marker so they survive the trip. */
    fun encode(): String = lines.mapIndexed { i, l -> if (i >= headerLines) MARK + l else l }.joinToString("\n")

    companion object {
        private const val MARK = "\u0001"
        private fun queryOf(line: String) = line.filter { it.isLetterOrDigit() || it == ' ' || it == '+' }.replace(Regex("\\s+"), " ").trim()

        fun parse(encoded: String?): ChatKey? {
            val raw = encoded?.split('\n')?.map { it.trim() }.orEmpty()
            val header = raw.filter { !it.startsWith(MARK) && ChatNameMatcher.clean(it).isNotEmpty() }.distinct()
            if (header.isEmpty()) return null
            val learned = raw.filter { it.startsWith(MARK) }.map { it.removePrefix(MARK).trim() }
                .filter { ChatNameMatcher.clean(it).isNotEmpty() && it !in header }.distinct()
            return ChatKey(header + learned, header.size)
        }
        fun of(vararg lines: String): ChatKey? = parse(lines.joinToString("\n"))
    }
}

/**
 * Decides whether a row of the share picker is the chat we came from. Deliberately strict: a wrong chat is far worse
 * than asking the user to pick, so there is no fuzzy / prefix matching, and two equally good rows mean "unknown".
 *
 * Score per row text (best wins), minus 10 for header lines after the title:
 *  110 identical after case / accent / bidi normalisation        (the old behaviour)
 *  100 identical once emoji, "~" and other symbols are dropped   ("Aano F❤️" = "Aano F", "~ Aayan" = "Aayan")
 *   98 identical ignoring spaces
 *   95 the same phone number, however it is formatted ("+91 98707 16919" = "098707-16919")
 */
object ChatNameMatcher {
    /** Lowest score taken as a match (a secondary header line needs an exact cleaned match at least). */
    const val ACCEPT = 85

    /** Lower case, accents / bidi marks / symbols / emoji / punctuation removed, one space between words. */
    fun clean(s: String?): String {
        val base = WhatsAppUi.normalize(s)
        val sb = StringBuilder(base.length)
        for (c in base) {
            val type = Character.getType(c)
            val keep = c.isLetterOrDigit() || type == Character.COMBINING_SPACING_MARK.toInt() || type == Character.ENCLOSING_MARK.toInt()
            sb.append(if (keep) c else ' ')
        }
        val words = sb.toString().trim().replace(Regex("\\s+"), " ")
        // A verified business is read out as "Amazon, verified business account": the badge is not part of the name.
        return BADGE_SUFFIX.matchEntire(words)?.groupValues?.get(1)?.trim() ?: words
    }

    private val BADGE_SUFFIX = Regex("^(.+?) (verified business account|verified business|verified account|official business account|official account|verified)$")

    /** The number as its last 10 digits when [s] has at least 7 digits, else null. */
    fun phoneKey(s: String?): String? {
        val digits = s?.filter { it.isDigit() } ?: return null
        return if (digits.length >= 7) digits.takeLast(10) else null
    }

    /** Best score of one picker row, given every text it shows (name, "~ push name", about line...). */
    fun score(key: ChatKey, rowTexts: List<String>): Int {
        var best = 0
        for (raw in rowTexts) {
            val rawNorm = WhatsAppUi.normalize(raw)
            val r = clean(raw)
            if (r.isEmpty()) continue
            val rp = phoneKey(raw)
            val rs = r.replace(" ", "")
            for (i in key.lines.indices) {
                val a = key.clean[i]
                if (a.isEmpty()) continue
                val s = when {
                    rawNorm == WhatsAppUi.normalize(key.lines[i]) -> 110
                    r == a -> 100
                    rs == a.replace(" ", "") -> 98
                    rp != null && rp == key.phones[i] -> 95
                    else -> 0
                }
                if (s > 0) best = maxOf(best, if (i == 0) s else s - 10)
            }
        }
        return best
    }
}
