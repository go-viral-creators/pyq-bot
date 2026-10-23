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

    @Test fun withoutAChatNameTheUserIsToldAtOnceAndNothingIsTapped() {
        val f = flow(null); val a = RecordingActions()
        val out = f.step(FakeTree.picker("Mom"), a)
        assertEquals(SendFlow.Outcome.Failed("chat name unknown"), out)
        repeat(3) { clock += 250; f.step(FakeTree.preview(), a) }
        assertTrue(a.clicks.isEmpty() && a.taps.isEmpty() && a.typed.isEmpty())
    }

    @Test fun usesPickerSearchWhenChatNotVisible() {
        val f = flow("Zed"); val a = RecordingActions()
        val list = FakeTree.picker("Mom", "Dad")
        f.step(list, a)                       // list just appeared: give the visible rows a moment
        assertTrue(a.clicks.isEmpty() && a.typed.isEmpty())
        clock += 800; f.step(list, a)         // still no Zed: opens the search box
        assertTrue(a.clicks.last().hasId("menuitem_search"))
        clock += 250
        f.step(FakeTree.pickerWithSearchField("Mom", "Dad"), a)       // types the name
        assertEquals(listOf("Zed"), a.typed)
        clock += 250
        f.step(FakeTree.pickerWithSearchField("Zed"), a)              // result appears -> click it
        assertTrue(a.clicks.last().children[0].text == "Zed")
    }

    @Test fun neverScrollsTheListAnyMore() {
        val f = flow("Zed"); val a = RecordingActions()
        repeat(15) { clock += 300; f.step(FakeTree.picker("Mom", "Dad"), a) }
        assertEquals(0, a.scrolls)
    }

    @Test fun givesUpFiveSecondsAfterTheListAppearedNotFromTheStart() {
        val f = flow("Nobody"); val a = RecordingActions()
        clock += 3_000                                         // the list took 3 s to show up: that time does not count
        assertEquals(SendFlow.Outcome.Continue, f.step(FakeTree.conversation("Nobody"), a))
        assertEquals(SendFlow.Outcome.Continue, f.step(FakeTree.picker("Mom"), a))
        clock += 4_900
        assertEquals(SendFlow.Outcome.Continue, f.step(FakeTree.picker("Mom"), a))
        clock += 200
        assertEquals(SendFlow.Outcome.Failed("chat not found"), f.step(FakeTree.picker("Mom"), a))
        assertTrue(a.clicks.none { it.children.firstOrNull()?.text == "Mom" })
    }

    @Test fun gaveUpWhenTheShareScreenNeverAppears() {
        val f = flow("Mom"); val a = RecordingActions()
        clock += 5_900
        assertEquals(SendFlow.Outcome.Continue, f.step(FakeTree.conversation("Mom"), a))
        clock += 200
        assertEquals(SendFlow.Outcome.Failed("share screen not shown"), f.step(FakeTree.conversation("Mom"), a))
        assertTrue(a.clicks.isEmpty() && a.typed.isEmpty())
    }

    @Test fun twoEquallyGoodRowsAreNotGuessed() {
        val f = flow("Mom"); val a = RecordingActions()
        val list = FakeTree.picker("Mom", "Mom")
        f.step(list, a)
        assertTrue(a.clicks.isEmpty())
        clock += 5_100
        assertEquals(SendFlow.Outcome.Failed("chat not found"), f.step(list, a))
    }

    @Test fun findsTheChatThroughItsOtherHeaderLine() {
        // Business account: the picker shows the verified name, the header's first line is the person.
        val f = flow("Ravi Mourya\nZapUPI Gateway"); val a = RecordingActions()
        f.step(FakeTree.picker("Amazon", "ZapUPI Gateway"), a)
        assertEquals(1, a.clicks.size)
        assertEquals("ZapUPI Gateway", a.clicks[0].children[0].text)
    }

    @Test fun aChatThatCannotReceiveAudioFailsAtOnceWithoutTapping() {
        val f = flow("Some Business"); val a = RecordingActions()
        val r = FakeTree.root()
        val list = r.add(UiNode(className = "androidx.recyclerview.widget.RecyclerView", scrollable = true))
        val row = list.row("Some Business")
        row.add(UiNode(text = "Can't send this message type", className = "android.widget.TextView"))
        assertEquals(SendFlow.Outcome.Failed("chat blocked"), f.step(r, a))
        assertTrue(a.clicks.isEmpty() && a.taps.isEmpty())
    }

    @Test fun triesTheRememberedNameInTheSearchBoxFirstThenTheHeaderName() {
        val key = ChatKey.parse("Ravi Mourya")!!.withAliases(listOf("Guru Mourya"))
        val f = flow(key.encode()); val a = RecordingActions()
        val list = FakeTree.picker("Mom", "Dad")
        f.step(list, a)
        clock += 800; f.step(list, a)                                   // opens the search box
        clock += 250; f.step(FakeTree.pickerWithSearchField("Mom"), a)  // types the remembered name
        assertEquals(listOf("Guru Mourya"), a.typed)
        clock += 1_200; f.step(FakeTree.pickerWithSearchField("Mom"), a) // no usable result: the header name
        assertEquals(listOf("Guru Mourya", "Ravi Mourya"), a.typed)
    }

    @Test fun gaveUpWhenTheSendButtonNeverAppears() {
        val f = flow("Mom"); val a = RecordingActions()
        f.step(FakeTree.picker("Mom"), a)
        clock += 6_100
        assertEquals(SendFlow.Outcome.Failed("send button not found"), f.step(FakeTree.picker("Mom"), a))
        assertTrue(!f.sendTapped)
    }

    /** Selects "Mom" on the picker and taps Send on the preview; returns the number of actions so far. */
    private fun sendTo(f: SendFlow, a: RecordingActions, preview: UiNode = FakeTree.preview()) {
        f.step(FakeTree.picker("Mom"), a)
        clock += 1_300
        assertEquals(SendFlow.Outcome.Continue, f.step(preview, a))
        assertTrue(f.sendTapped)
    }

    @Test fun sendButtonThatNeverGoesAwayIsRetriedOnceThenUnknownNeverFailed() {
        val f = flow("Mom"); val a = RecordingActions()
        sendTo(f, a)
        var out: SendFlow.Outcome = SendFlow.Outcome.Continue
        repeat(12) { clock += 1_600; if (out == SendFlow.Outcome.Continue) out = f.step(FakeTree.preview(), a) }
        assertTrue("was $out", out is SendFlow.Outcome.Unknown)
        assertEquals(1 + 1, a.clicks.size)   // row + first Send
        assertEquals(1, a.taps.size)         // exactly one retry: no flood of duplicate sends
    }

    @Test fun backInTheChatAfterSendIsSuccessAndNothingElseIsTapped() {
        // The bug behind "audio not sent": WhatsApp returns to the chat, whose mic / camera buttons sit in the lower-right
        // corner. They must not be taken for "Send" and tapped again.
        val f = flow("Mom"); val a = RecordingActions()
        sendTo(f, a)
        val before = a.clicks.size + a.taps.size
        clock += 400
        assertEquals(SendFlow.Outcome.Done, f.step(FakeTree.conversation("Mom"), a))
        assertEquals(before, a.clicks.size + a.taps.size)
    }

    @Test fun openingWhatsAppCameraAfterSendIsSuccessNotFailure() {
        val f = flow("Mom"); val a = RecordingActions()
        sendTo(f, a)
        val before = a.clicks.size + a.taps.size
        repeat(10) { clock += 900; assertEquals(SendFlow.Outcome.Done, f.step(FakeTree.camera(), a)) }
        assertEquals(before, a.clicks.size + a.taps.size)   // never taps the shutter
    }

    @Test fun draftInTheChatAfterSendIsNeverSent() {
        // Back in the conversation the user had typed text: its Send button has the same id as the preview's.
        val f = flow("Mom"); val a = RecordingActions()
        sendTo(f, a)
        val before = a.clicks.size + a.taps.size
        repeat(6) { clock += 1_600; assertEquals(SendFlow.Outcome.Done, f.step(FakeTree.conversation("Mom", draftTyped = true), a)) }
        assertEquals(before, a.clicks.size + a.taps.size)
    }

    @Test fun overallTimeoutAfterSendIsNotReportedAsFailure() {
        val f = flow("Mom"); val a = RecordingActions()
        f.step(FakeTree.picker("Mom"), a)
        clock += 5_900                                      // slow device: Send tapped late
        f.step(FakeTree.preview(), a)
        assertTrue(f.sendTapped)
        clock += 300
        val out = f.step(FakeTree.preview(), a)             // past the "find Send" limit
        assertTrue("was $out", out !is SendFlow.Outcome.Failed)
    }

    @Test fun leavingWhatsAppAfterSendIsUnknown() {
        val f = flow("Mom"); val a = RecordingActions()
        sendTo(f, a)
        var out: SendFlow.Outcome = SendFlow.Outcome.Continue
        repeat(8) { clock += 2_000; if (out == SendFlow.Outcome.Continue) out = f.step(null, a) }
        assertTrue("was $out", out is SendFlow.Outcome.Unknown)
    }

    @Test fun pickerComingBackWithoutPreviewIsUnknown() {
        val f = flow("Mom"); val a = RecordingActions()
        sendTo(f, a)
        var out: SendFlow.Outcome = SendFlow.Outcome.Continue
        repeat(5) { clock += 800; if (out == SendFlow.Outcome.Continue) out = f.step(FakeTree.picker("Mom"), a) }
        assertTrue("was $out", out is SendFlow.Outcome.Unknown)
    }

    @Test fun iconOnlySendFoundByPositionIsConfirmedByItsOwnDisappearance() {
        val f = flow("Mom"); val a = RecordingActions()
        sendTo(f, a, FakeTree.previewIconOnly())
        clock += 500
        assertEquals(SendFlow.Outcome.Continue, f.step(FakeTree.previewIconOnly(), a))  // same button still there
        clock += 500
        assertEquals(SendFlow.Outcome.Done, f.step(FakeTree.conversationWithoutIds(), a))
    }

    @Test fun neverTapsSendWhenWhatsAppOpensTheChatInsteadOfAPreview() {
        val f = flow("Mom"); val a = RecordingActions()
        f.step(FakeTree.picker("Mom"), a)
        repeat(6) { clock += 1_000; f.step(FakeTree.conversation("Mom", draftTyped = true), a) }
        assertEquals(1, a.clicks.size + a.taps.size)        // only the row
        assertTrue(!f.sendTapped)
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
