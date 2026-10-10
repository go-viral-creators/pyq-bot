package com.kaafisir.auramic.automation

import com.kaafisir.auramic.automation.FakeTree.row
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fallbacks for apps / versions whose resource ids we do not know (Telegram, Instagram, future WhatsApp builds). */
class NodeFinderFallbackTest {
    private var clock = 0L

    /** Telegram-like share sheet: names are plain, non-clickable text nodes; no resource ids at all. */
    private fun telegramShare(vararg names: String): UiNode {
        val r = UiNode(className = "android.widget.FrameLayout", bounds = Bounds(0, 0, 1080, 2400))
        val grid = r.add(UiNode(className = "android.view.ViewGroup", bounds = Bounds(0, 800, 1080, 2000)))
        names.forEachIndexed { i, n ->
            grid.add(UiNode(text = n, className = "android.widget.TextView", bounds = Bounds(40 + i * 260, 1000, 280 + i * 260, 1100)))
        }
        return r
    }

    @Test fun findsPlainTextRowWithoutIdsAndTapsIt() {
        val f = SendFlow("Aarav", { clock }); val a = RecordingActions()
        f.step(telegramShare("Mom", "Aarav", "Work"), a)
        // no clickable ancestor: the node itself is handed to click(), which (in the service) falls back to a real tap
        assertEquals(1, a.clicks.size)
        assertEquals("Aarav", a.clicks[0].text)
    }

    @Test fun sendButtonByShortLabelAndByPosition() {
        val withLabel = UiNode(bounds = Bounds(0, 0, 1080, 2400))
        withLabel.add(UiNode(text = "Send (1)", clickable = true, className = "android.widget.TextView", bounds = Bounds(700, 2200, 1000, 2350)))
        assertNotNull(NodeFinder.sendButton(withLabel))

        val iconOnly = UiNode(bounds = Bounds(0, 0, 1080, 2400))
        iconOnly.add(UiNode(clickable = true, className = "android.widget.ImageButton", bounds = Bounds(900, 2150, 1040, 2290)))
        assertNull(NodeFinder.sendButton(iconOnly, allowPositional = false))      // never guess before a chat was chosen
        assertNotNull(NodeFinder.sendButton(iconOnly, allowPositional = true))
    }

    @Test fun swallowedRowClickIsRetriedWithATap() {
        val f = SendFlow("Mom", { clock }); val a = RecordingActions()
        val picker = FakeTree.picker("Mom", "Dad")
        f.step(picker, a)                                  // row clicked, screen does not change
        clock += 1_700
        f.step(picker, a)
        assertEquals(1, a.taps.size)
        assertEquals("Mom", a.taps[0].children[0].text)
    }

    @Test fun mostRecentChatOnlyWhenOptedIn() {
        val picker = FakeTree.picker("Top chat", "Other")
        val off = RecordingActions(); SendFlow(null, { clock }).step(picker, off)
        assertTrue(off.clicks.isEmpty() && off.taps.isEmpty())
        val on = RecordingActions(); SendFlow(null, { clock }, allowFirstRow = true).step(picker, on)
        assertEquals("Top chat", on.clicks[0].children[0].text)
    }

    @Test fun genericComposerDetectionAndTitleFallback() {
        val conv = UiNode(bounds = Bounds(0, 0, 1080, 2400))
        conv.add(UiNode(text = "Priya", className = "android.widget.TextView", bounds = Bounds(150, 60, 500, 130)))
        conv.add(UiNode(text = "last seen", className = "android.widget.TextView", bounds = Bounds(150, 130, 500, 170)))
        conv.add(UiNode(editable = true, className = "android.widget.EditText", bounds = Bounds(100, 2200, 900, 2340)))
        assertTrue(NodeFinder.hasComposer(conv))
        assertEquals("Priya", NodeFinder.chatTitle(conv, 2400))
    }

    @Test fun diagnosticsDumpIsCompactAndShortensText() {
        val r = FakeTree.picker("Aarav Sharma")
        val dump = NodeDump.format(r, maxNodes = 50)
        assertTrue(dump.contains("contactpicker_row_name"))
        assertTrue("names must be shortened", !dump.contains("Aarav Sharma"))
        assertTrue(dump.contains("Aarav ~12"))
    }
}
