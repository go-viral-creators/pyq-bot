package com.kaafisir.auramic.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NodeFinderTest {
    @Test fun readsChatTitleById() {
        assertEquals("Mom ❤", NodeFinder.chatTitle(FakeTree.conversation("Mom ❤"), 2400))
    }

    @Test fun chatTitleFallsBackToLargestTopText() {
        val r = FakeTree.root()
        r.add(UiNode(viewId = "com.whatsapp:id/entry", editable = true, className = "android.widget.EditText"))
        r.add(UiNode(text = "online", bounds = Bounds(200, 90, 400, 120), className = "android.widget.TextView"))
        r.add(UiNode(text = "Work Group", bounds = Bounds(200, 40, 600, 100), className = "android.widget.TextView"))
        assertEquals("Work Group", NodeFinder.chatTitle(r, 2400))
    }

    @Test fun noTitleOutsideChat() { assertNull(NodeFinder.chatTitle(FakeTree.picker("A"), 2400)) }

    @Test fun matchesNamesIgnoringCaseSpacingAndBidiMarks() {
        val picker = FakeTree.picker("‎aarav  SHARMA")
        assertNotNull(NodeFinder.chatRow(picker, "Aarav Sharma"))
    }

    @Test fun neverGuessesFromAPrefixOrWhenTwoRowsAreEquallyGood() {
        // A longer name that merely starts the same way is another person: ask the user instead.
        assertNull(NodeFinder.chatRow(FakeTree.picker("Aarav Sharma", "Mom"), "Aarav"))
        assertNull(NodeFinder.chatRow(FakeTree.picker("Mom", "Mom"), "Mom"))
        // exact match beats a longer name that merely starts the same way
        val p = FakeTree.picker("Mom 2", "Mom")
        assertEquals("Mom", NodeFinder.chatRow(p, "Mom")!!.children[0].text)
    }

    @Test fun sendButtonRecognisedByLabelInOtherLanguages() {
        for (word in listOf("Enviar", "भेजें", "إرسال", "Kirim")) {
            val r = FakeTree.root()
            r.add(UiNode(desc = word, clickable = true, className = "android.widget.ImageButton"))
            assertNotNull("label $word", NodeFinder.sendButton(r))
        }
    }

    @Test fun sendLabelInsideTextFieldIsIgnored() {
        val r = FakeTree.root()
        r.add(UiNode(text = "Send", editable = true, clickable = true, className = "android.widget.EditText"))
        assertNull(NodeFinder.sendButton(r))
    }

    @Test fun clickableAncestorIsUsed() {
        val r = FakeTree.root()
        val box = r.add(UiNode(clickable = true))
        box.add(UiNode(text = "SEND", className = "android.widget.TextView"))
        assertEquals(box, NodeFinder.sendButton(r))
    }
}
