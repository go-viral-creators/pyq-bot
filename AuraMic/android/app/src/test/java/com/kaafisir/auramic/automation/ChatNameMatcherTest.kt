package com.kaafisir.auramic.automation

import com.kaafisir.auramic.automation.FakeTree.row
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatNameMatcherTest {
    private fun score(key: String, vararg rowTexts: String) = ChatNameMatcher.score(ChatKey.parse(key)!!, rowTexts.toList())

    @Test fun identicalNamesScoreHighest() = assertEquals(110, score("Aarav Sharma", "aarav  sharma"))

    @Test fun emojiAndSymbolsDoNotMatter() {
        assertEquals(100, score("Aano F❤️", "Aano F"))
        assertEquals(100, score("~ Aayan", "Aayan🤙"))
        assertEquals(100, score("Mehrban Ali Kairana", "Mehrban-Ali Kairana"))
    }

    @Test fun spacingDoesNotMatter() = assertEquals(98, score("Ali Khan", "AliKhan"))

    @Test fun phoneNumbersMatchWhateverTheirFormat() {
        assertEquals(95, score("+91 98707 16919", "098707-16919"))
        assertEquals(110, score("+91 98707 16919", "+91 98707 16919", "~ Aayan"))
    }

    @Test fun differentPeopleNeverMatch() {
        assertEquals(0, score("Ravi Kumar", "Ravi Kumar Singh"))       // no prefix guessing
        assertEquals(0, score("Mohammad Ali", "Mohammed Ali"))         // no fuzzy guessing
        assertEquals(0, score("+91 98707 16919", "+91 98707 16910"))
    }

    @Test fun anotherHeaderLineScoresLowerThanTheTitle() {
        val key = ChatKey.parse("Ravi Mourya\nZapUPI Gateway")!!
        assertEquals(110, ChatNameMatcher.score(key, listOf("Ravi Mourya")))
        assertEquals(100, ChatNameMatcher.score(key, listOf("ZapUPI Gateway")))   // 110 - 10
    }

    @Test fun theVerifiedBadgeIsNotPartOfTheName() {
        assertEquals(100, score("Amazon", "Amazon, verified business account"))
        assertEquals(100, score("Amazon Verified", "Amazon"))
        assertEquals("official store", ChatNameMatcher.clean("Official Store"))        // only a trailing badge is dropped
        assertEquals("verified", ChatNameMatcher.clean("Verified"))
    }

    @Test fun businessRowsWithOnlyADescriptionAreFound() {
        val r = FakeTree.root()
        val list = r.add(UiNode(className = "androidx.recyclerview.widget.RecyclerView", scrollable = true))
        val row = list.add(UiNode(className = "android.widget.LinearLayout", clickable = true, bounds = Bounds(0, 400, 1080, 560)))
        row.add(UiNode(viewId = "com.whatsapp:id/contactpicker_row_name", desc = "Amazon, verified", className = "android.widget.TextView"))
        row.add(UiNode(text = "BUSINESS ACCOUNT", className = "android.widget.TextView"))
        list.row("Mom")
        assertTrue(NodeFinder.chatRow(r, "Amazon") === row)
    }

    @Test fun aRowThatCannotTakeThisMessageTypeIsReportedAsBlocked() {
        val r = FakeTree.root()
        val list = r.add(UiNode(className = "androidx.recyclerview.widget.RecyclerView", scrollable = true))
        val row = list.row("Some Business")
        row.add(UiNode(text = "Can't send this message type", className = "android.widget.TextView"))
        list.row("Mom")
        assertTrue(NodeFinder.rowBlocked(r, "Some Business"))
        assertTrue(!NodeFinder.rowBlocked(r, "Mom"))
    }

    @Test fun shortNumbersAreNotPhoneNumbers() = assertNull(ChatNameMatcher.phoneKey("Room 12345"))

    @Test fun searchTextDropsEmojiButKeepsThePlusOfANumber() {
        assertEquals("Aano F", ChatKey.parse("Aano F❤️")!!.searchQueries.first())
        assertEquals("+91 98707 16919", ChatKey.parse("+91 98707 16919\n~ Aayan")!!.searchQueries.first())
    }

    @Test fun blankNamesGiveNoKey() {
        assertNull(ChatKey.parse(null)); assertNull(ChatKey.parse("  \n ~ ❤️ "))
    }

    // ---- through NodeFinder on fake screens ----------------------------------------------------------------

    @Test fun rowWithPushNameSubtitleIsFoundByNumber() {
        val r = FakeTree.root()
        val list = r.add(UiNode(className = "androidx.recyclerview.widget.RecyclerView", scrollable = true))
        val row = list.row("+91 98707 16919")
        row.add(UiNode(text = "~ Aayan", className = "android.widget.TextView"))
        list.row("Mom")
        val found = NodeFinder.chatRow(r, "+91 98707 16919")
        assertNotNull(found)
        assertTrue(found === row)
    }

    @Test fun pushNameOfAnUnsavedNumberFindsTheRow() {
        val r = FakeTree.root()
        val list = r.add(UiNode(className = "androidx.recyclerview.widget.RecyclerView", scrollable = true))
        val row = list.row("+91 98707 16919")
        row.add(UiNode(text = "~ Aayan🤙", className = "android.widget.TextView"))
        assertTrue(NodeFinder.chatRow(r, "Aayan") === row)
    }

    @Test fun exactRowBeatsALooserOne() {
        val r = FakeTree.picker("Mom ❤️", "Mom")
        val found = NodeFinder.chatRow(r, "Mom")!!
        assertEquals("Mom", found.children[0].text)                  // 110 vs 100: the exact one, not ambiguous
    }

    @Test fun chatTitleLinesIncludeTheSecondHeaderLine() {
        val r = FakeTree.root()
        r.add(UiNode(viewId = "com.whatsapp:id/conversation_contact_name", text = "Ravi Mourya", className = "android.widget.TextView", bounds = Bounds(180, 90, 700, 150)))
        r.add(UiNode(text = "ZapUPI Gateway", className = "android.widget.TextView", bounds = Bounds(180, 150, 700, 200)))
        r.add(UiNode(text = "online", className = "android.widget.TextView", bounds = Bounds(180, 200, 700, 240)))
        r.add(UiNode(viewId = "com.whatsapp:id/entry", editable = true, className = "android.widget.EditText", bounds = Bounds(40, 2230, 860, 2350)))
        assertEquals(listOf("Ravi Mourya", "ZapUPI Gateway"), NodeFinder.chatTitleLines(r, 2400))
        assertEquals("Ravi Mourya", NodeFinder.chatTitle(r, 2400))
    }

    @Test fun chatTitleLinesAreEmptyOnTheChatList() {
        assertTrue(NodeFinder.chatTitleLines(FakeTree.picker("Mom"), 2400).isEmpty())
    }
}
