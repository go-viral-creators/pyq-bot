package com.kaafisir.auramic.automation

import com.kaafisir.auramic.automation.FakeTree.row
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SendFlowTest {
    private var clock = 0L
    private fun flow(chat: String?) = SendFlow(chat, now = { clock })

    @Test fun clicksVisibleChatRowThenSend() {
        val f = flow("Aarav Sharma"); val a = RecordingActions()
        val picker = FakeTree.picker("Mom", "Aarav Sharma", "Work group")
        assertEquals(SendFlow.Outcome.Continue, f.step(picker, a))
        assertEquals(1, a.clicks.size)
        assertTrue(a.clicks[0].children[0].text == "Aarav Sharma")

        clock += 300
        assertEquals(SendFlow.Outcome.Continue, f.step(FakeTree.preview(), a))   // taps Send
        assertEquals(2, a.clicks.size)
        assertTrue(a.clicks[1].hasId("send"))

        clock += 400
        assertEquals(SendFlow.Outcome.Done, f.step(FakeTree.conversation("Aarav Sharma"), a)) // button gone
    }

    @Test fun neverTapsSendBeforeChatWasSelected() {
        // A conversation screen with a half-typed message exposes a Send button. We must not tap it.
        val f = flow("Mom"); val a = RecordingActions()
        repeat(5) { clock += 250; f.step(FakeTree.conversation("Mom", draftTyped = true), a) }
        assertTrue(a.clicks.isEmpty())
    }

    @Test fun assistedModeDoesNothingWithoutChatName() {
        val f = flow(null); val a = RecordingActions()
        repeat(4) { clock += 250; f.step(FakeTree.picker("Mom"), a); f.step(FakeTree.preview(), a) }
        assertTrue(a.clicks.isEmpty())
    }

    @Test fun usesPickerSearchWhenChatNotVisible() {
        val f = flow("Zed"); val a = RecordingActions()
        val list = FakeTree.picker("Mom", "Dad")
        f.step(list, a)                      // first step on picker: waits one tick
        clock += 250; f.step(list, a); clock += 250; f.step(list, a)  // nudges the list twice
        assertEquals(2, a.scrolls)
        clock += 250; f.step(list, a)        // opens search
        assertTrue(a.clicks.last().hasId("menuitem_search"))
        clock += 250
        f.step(FakeTree.pickerWithSearchField("Mom", "Dad"), a)       // types the name
        assertEquals(listOf("Zed"), a.typed)
        clock += 250
        f.step(FakeTree.pickerWithSearchField("Zed"), a)              // result appears -> click it
        assertTrue(a.clicks.last().children[0].text == "Zed")
    }

    @Test fun timesOutWhenChatIsNeverFound() {
        val f = flow("Nobody"); val a = RecordingActions()
        clock += 26_000
        val out = f.step(FakeTree.picker("Mom"), a)
        assertTrue(out is SendFlow.Outcome.Failed)
        assertTrue(a.clicks.isEmpty())
    }

    @Test fun retriesSendThenFails() {
        val f = flow("Mom"); val a = RecordingActions()
        f.step(FakeTree.picker("Mom"), a)
        var out: SendFlow.Outcome = SendFlow.Outcome.Continue
        repeat(12) { clock += 1_600; out = f.step(FakeTree.preview(), a) }  // Send never goes away
        assertTrue(out is SendFlow.Outcome.Failed)
        assertEquals(1 + 3, a.clicks.size) // row + 3 attempts
    }

    @Test fun neverTypesIntoTheMessageBoxOfAConversation() {
        // Right after ACTION_SEND the conversation is still on screen for a moment. Its only
        // EditText is the user's message box: the flow must wait, not type the chat name there.
        val f = flow("Mom"); val a = RecordingActions()
        repeat(6) { clock += 250; f.step(FakeTree.conversation("Mom"), a) }
        assertTrue(a.typed.isEmpty())
        assertTrue(a.clicks.isEmpty())
        assertEquals(0, a.scrolls)
    }
}
