package com.kaafisir.auramic.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatAliasBookTest {
    private val key = ChatKey.parse("Ravi Mourya")!!

    @Test fun aNameIsTrustedOnlyAfterTheSamePickTwice() {
        val b = ChatAliasBook()
        assertFalse(b.observe(key, "Guru Mourya"))
        assertTrue(b.aliasesFor(key).isEmpty())            // one pick (could be a slip): not used yet
        assertTrue(b.observe(key, "Guru Mourya"))          // second time: trusted now
        assertEquals(listOf("Guru Mourya"), b.aliasesFor(key))
        assertFalse(b.observe(key, "Guru Mourya"))         // already trusted: not "just became"
    }

    @Test fun twoDifferentPicksDoNotAddUp() {
        val b = ChatAliasBook()
        b.observe(key, "Guru Mourya"); b.observe(key, "Someone Else")
        assertTrue(b.aliasesFor(key).isEmpty())
    }

    @Test fun nothingIsLearnedWhenThePickedNameIsTheSameChatName() {
        val b = ChatAliasBook()
        repeat(3) { assertFalse(b.observe(key, "ravi  mourya")) }
        assertFalse(b.observe(key, "Ravi Mourya 🙂")); assertTrue(b.aliasesFor(key).isEmpty())
        assertEquals(0, b.activeCount())
    }

    @Test fun survivesSaveAndLoad() {
        val b = ChatAliasBook()
        b.observe(key, "Guru Mourya"); b.observe(key, "Guru Mourya"); b.observe(key, "Half Seen")
        val again = ChatAliasBook(b.serialize())
        assertEquals(listOf("Guru Mourya"), again.aliasesFor(key))
        assertEquals(1, again.activeCount())
        assertTrue(again.observe(key, "Half Seen"))        // its first sighting was kept too
    }

    @Test fun clearForgetsEverything() {
        val b = ChatAliasBook()
        repeat(2) { b.observe(key, "Guru Mourya") }
        b.clear()
        assertTrue(b.aliasesFor(key).isEmpty()); assertEquals("", b.serialize())
    }

    @Test fun keepsOnlyTheMostRecentChats() {
        val b = ChatAliasBook(maxChats = 2)
        for (n in listOf("A one", "B two", "C three")) { val k = ChatKey.parse(n)!!; repeat(2) { b.observe(k, "X $n") } }
        assertEquals(2, b.activeCount())
        assertTrue(b.aliasesFor(ChatKey.parse("A one")!!).isEmpty())
    }

    @Test fun anAugmentedKeyFindsTheRowTheOldNameCouldNot() {
        val b = ChatAliasBook()
        repeat(2) { b.observe(key, "Guru Mourya") }
        val augmented = b.augment(key)
        val picker = FakeTree.picker("Mom", "Guru Mourya")
        assertEquals("Guru Mourya", NodeFinder.chatRow(picker, augmented.encode())!!.children[0].text)
        assertTrue(NodeFinder.chatRow(picker, key.encode()) == null)       // without the lesson it is "not found"
        // the learned name is searched first (the picker is known to show it); the header name is the fallback
        assertEquals(listOf("Guru Mourya", "Ravi Mourya"), augmented.searchQueries)
    }

    @Test fun learnedNamesSurviveEncodingButStayMarkedAsLearned() {
        val b = ChatAliasBook()
        repeat(2) { b.observe(key, "Guru Mourya") }
        val round = ChatKey.parse(b.augment(key).encode())!!
        assertEquals(listOf("Ravi Mourya", "Guru Mourya"), round.lines)
        assertEquals("Guru Mourya", round.searchQueries.first())
    }
}
