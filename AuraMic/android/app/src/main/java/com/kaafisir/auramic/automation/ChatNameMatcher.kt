package com.kaafisir.auramic.automation

/**
 * The name(s) a chat is known by. The first line is the title of the open conversation; further lines are the other
 * texts of its header (a business account's verified name, the "~ push name" of an unsaved number...). Encoded as
 * lines of one string so it travels through [SendRequest.chatName] unchanged.
 */
class ChatKey private constructor(val lines: List<String>) {
    /** [lines] after [ChatNameMatcher.clean], same order. */
    val clean: List<String> = lines.map { ChatNameMatcher.clean(it) }
    /** Last 10 digits of a phone-number line (null for names), same order. */
    val phones: List<String?> = lines.map { ChatNameMatcher.phoneKey(it) }

    /** What to type into the picker's search box: the title without emoji and symbols. */
    val searchText: String get() = lines.first().filter { it.isLetterOrDigit() || it == ' ' || it == '+' }
        .replace(Regex("\\s+"), " ").trim().ifEmpty { clean.first() }

    fun encode(): String = lines.joinToString("\n")

    companion object {
        fun parse(encoded: String?): ChatKey? {
            val lines = encoded?.split('\n')?.map { it.trim() }?.filter { ChatNameMatcher.clean(it).isNotEmpty() }.orEmpty()
            return if (lines.isEmpty()) null else ChatKey(lines.distinct())
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
        return sb.toString().trim().replace(Regex("\\s+"), " ")
    }

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
