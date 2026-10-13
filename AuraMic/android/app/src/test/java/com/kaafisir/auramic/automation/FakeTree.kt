package com.kaafisir.auramic.automation

/** Builders for fake WhatsApp screens used by the automation tests. */
object FakeTree {
    private const val P = "com.whatsapp:id/"

    fun root() = UiNode(className = "android.widget.FrameLayout", bounds = Bounds(0, 0, 1080, 2400))

    fun UiNode.row(name: String, clickable: Boolean = true): UiNode {
        val row = add(UiNode(className = "android.widget.LinearLayout", clickable = clickable, bounds = Bounds(0, 400, 1080, 560)))
        row.add(UiNode(viewId = P + "contactpicker_row_name", text = name, className = "android.widget.TextView"))
        return row
    }

    fun picker(vararg names: String): UiNode {
        val r = root()
        val list = r.add(UiNode(className = "androidx.recyclerview.widget.RecyclerView", scrollable = true))
        names.forEach { list.row(it) }
        r.add(UiNode(viewId = P + "menuitem_search", desc = "Search", clickable = true, className = "android.widget.ImageButton"))
        return r
    }

    fun pickerWithSearchField(vararg names: String): UiNode {
        val r = picker(*names)
        r.add(UiNode(viewId = P + "search_src_text", className = "android.widget.EditText", editable = true))
        return r
    }

    fun preview(): UiNode {
        val r = root()
        r.add(UiNode(viewId = P + "send", desc = "Send", clickable = true, className = "android.widget.ImageButton"))
        return r
    }

    fun conversation(title: String, draftTyped: Boolean = false): UiNode {
        val r = root()
        r.add(UiNode(viewId = P + "conversation_contact_name", text = title, className = "android.widget.TextView", bounds = Bounds(180, 90, 700, 150)))
        r.add(UiNode(viewId = P + "entry", editable = true, className = "android.widget.EditText", bounds = Bounds(40, 2230, 860, 2350)))
        // Real bounds: these round buttons sit in the lower-right corner, exactly where the positional fallback looks.
        r.add(UiNode(viewId = P + "camera_btn", desc = "Camera", clickable = true, className = "android.widget.ImageButton", bounds = Bounds(760, 2240, 850, 2340)))
        r.add(UiNode(viewId = P + (if (draftTyped) "send" else "voice_note_btn"), desc = if (draftTyped) "Send" else "Voice message",
            clickable = true, className = "android.widget.ImageButton", bounds = Bounds(900, 2220, 1040, 2360)))
        return r
    }

    /** A conversation whose ids are unknown to us (future WhatsApp): only labels and positions. */
    fun conversationWithoutIds(): UiNode {
        val r = root()
        r.add(UiNode(className = "android.widget.EditText", editable = true, bounds = Bounds(40, 2230, 760, 2350)))
        r.add(UiNode(desc = "Camera", clickable = true, className = "android.widget.ImageButton", bounds = Bounds(770, 2240, 860, 2340)))
        r.add(UiNode(desc = "Voice message", clickable = true, className = "android.widget.ImageButton", bounds = Bounds(900, 2220, 1040, 2360)))
        return r
    }

    /** WhatsApp's in-app camera: big shutter button low in the screen, no message box. */
    fun camera(): UiNode {
        val r = root()
        r.add(UiNode(viewId = P + "shutter", desc = "Take photo", clickable = true, className = "android.widget.ImageView", bounds = Bounds(440, 2050, 640, 2250)))
        r.add(UiNode(viewId = P + "switch_camera_btn", desc = "Switch camera", clickable = true, className = "android.widget.ImageView", bounds = Bounds(860, 2090, 980, 2210)))
        return r
    }

    /** Media preview whose Send button has no id or label (icon-only FAB) - only the positional fallback finds it. */
    fun previewIconOnly(): UiNode {
        val r = root()
        r.add(UiNode(clickable = true, className = "android.widget.ImageButton", bounds = Bounds(900, 2200, 1040, 2340)))
        return r
    }
}

class RecordingActions : NodeActions {
    val clicks = mutableListOf<UiNode>()
    val typed = mutableListOf<String>()
    val taps = mutableListOf<UiNode>()
    var scrolls = 0
    override fun click(node: UiNode) = true.also { clicks += node }
    override fun setText(node: UiNode, text: String) = true.also { typed += text }
    override fun scrollForward(node: UiNode) = true.also { scrolls++ }
    override fun tap(node: UiNode) = true.also { taps += node }
}
