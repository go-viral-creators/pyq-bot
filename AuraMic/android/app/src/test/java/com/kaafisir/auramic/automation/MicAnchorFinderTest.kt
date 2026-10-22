package com.kaafisir.auramic.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MicAnchorFinderTest {
    private val w = 1080
    private val h = 2400
    private val P = "com.whatsapp:id/"
    private val micBounds = Bounds(940, 2180, 1050, 2290)   // 110 px round button, lower right

    private fun chat(mic: Boolean, desc: String? = null): UiNode {
        val r = FakeTree.root()
        r.add(UiNode(viewId = P + "conversation_contact_name", text = "Aarav", bounds = Bounds(150, 80, 600, 160)))
        r.add(UiNode(viewId = P + "entry", editable = true, bounds = Bounds(20, 2190, 900, 2280)))
        r.add(
            UiNode(
                viewId = P + (if (mic) "voice_note_btn" else "send"), desc = desc ?: if (mic) "Voice message" else "Send",
                clickable = true, bounds = micBounds,
            ),
        )
        return r
    }

    @Test fun findsTheMicByItsId() {
        val a = MicAnchorFinder.find(chat(mic = true), w, h)!!
        assertTrue(a.isMic); assertEquals(micBounds, a.bounds)
        assertEquals(995, a.centerX); assertEquals(2235, a.centerY); assertEquals(110, a.size)
    }

    @Test fun sendButtonMeansTextWasTypedSoTheMicIsNotAvailable() {
        val a = MicAnchorFinder.find(chat(mic = false), w, h)!!
        assertFalse(a.isMic)
    }

    @Test fun micIdWithSendLabelIsTheSendState() {
        // newer WhatsApp builds reuse one button and only change its label
        val a = MicAnchorFinder.find(chat(mic = true, desc = "Send"), w, h)!!
        assertFalse(a.isMic)
    }

    @Test fun chatListAndPickerHaveNoMic() {
        assertNull(MicAnchorFinder.find(FakeTree.picker("Mom", "Aarav"), w, h))
    }

    @Test fun unknownIdsAreFoundByTheirLabel() {
        val r = FakeTree.root()
        r.add(UiNode(viewId = P + "entry", editable = true, bounds = Bounds(20, 2190, 900, 2280)))
        r.add(UiNode(viewId = P + "some_new_id", desc = "Voice message", clickable = true, bounds = micBounds))
        assertTrue(MicAnchorFinder.find(r, w, h)!!.isMic)
        val es = FakeTree.root()
        es.add(UiNode(className = "android.widget.EditText", editable = true, bounds = Bounds(20, 2190, 900, 2280)))
        es.add(UiNode(desc = "Mensaje de voz", clickable = true, bounds = micBounds))
        assertTrue(MicAnchorFinder.find(es, w, h)!!.isMic)
    }

    @Test fun screensWithoutAMessageBoxNeverGetTheBubble() {
        // chat list: FAB-like buttons, an "Audio call" button, a bare label without composer
        val list = FakeTree.root()
        list.add(UiNode(viewId = P + "fab", desc = "New chat", clickable = true, bounds = micBounds))
        list.add(UiNode(desc = "Audio call", clickable = true, bounds = Bounds(800, 2000, 910, 2110)))
        list.add(UiNode(desc = "Voice message", clickable = true, bounds = Bounds(700, 2100, 810, 2210)))      // label alone, no composer
        list.add(UiNode(desc = "Microphone access", clickable = true, bounds = Bounds(500, 2100, 610, 2210)))
        assertNull(MicAnchorFinder.find(list, w, h))
        assertNull(MicAnchorFinder.classify(null, "Audio call", null))
        assertNull(MicAnchorFinder.classify(null, "Microphone access", null))
    }

    @Test fun implausibleShapesAndPlacesAreIgnored() {
        val top = FakeTree.root()
        top.add(UiNode(viewId = P + "voice_note_btn", desc = "Voice message", clickable = true, bounds = Bounds(900, 100, 1010, 210)))   // top of screen
        assertNull(MicAnchorFinder.find(top, w, h))
        val wide = FakeTree.root()
        wide.add(UiNode(viewId = P + "voice_note_btn", desc = "Voice message", clickable = true, bounds = Bounds(0, 2180, 1080, 2290)))
        assertNull(MicAnchorFinder.find(wide, w, h))
    }

    @Test fun clickableParentOfAnInnerIconSuppliesTheBounds() {
        val r = FakeTree.root()
        val box = r.add(UiNode(clickable = true, bounds = micBounds))
        box.add(UiNode(viewId = P + "voice_note_btn", desc = "Voice message", bounds = Bounds(965, 2205, 1025, 2265)))
        assertEquals(micBounds, MicAnchorFinder.find(r, w, h)!!.bounds)
        assertNotNull(MicAnchorFinder.classify(P + "voice_note_btn", null, null))
    }
}

class AnchorHolderTest {
    private val a = MicAnchor(Bounds(900, 2200, 1040, 2340), true)

    @Test fun keepsThePositionThroughAnUnreadableTransition() {
        val h = AnchorHolder()
        assertEquals(a, h.update(a, true, 0))
        assertEquals(a, h.update(null, false, 800))                 // tree not available: hold
        org.junit.Assert.assertNull(h.update(null, false, 1_100))   // ...but not forever
    }

    @Test fun leavingTheChatHidesAlmostImmediately() {
        val h = AnchorHolder()
        h.update(a, true, 0)
        org.junit.Assert.assertNull(h.update(null, true, 10))       // window read, no conversation: gone on this very read
        org.junit.Assert.assertFalse(h.holding)
    }

    @Test fun aConversationWithoutAMicKeepsTheBubbleForAWhile() {
        val h = AnchorHolder()
        h.update(a, true, 0)
        assertEquals(a, h.update(null, true, 300, conversationShown = true))
        org.junit.Assert.assertNull(h.update(null, true, 1_100, conversationShown = true))
    }

    @Test fun resetForgetsAtOnce() {
        val h = AnchorHolder()
        h.update(a, false, 0)
        h.reset()
        org.junit.Assert.assertNull(h.update(null, false, 10))
    }

    @Test fun aFreshAnchorAlwaysWins() {
        val h = AnchorHolder()
        val b = MicAnchor(Bounds(10, 20, 110, 120), true)
        h.update(a, true, 0)
        assertEquals(b, h.update(b, true, 50))
    }
}
