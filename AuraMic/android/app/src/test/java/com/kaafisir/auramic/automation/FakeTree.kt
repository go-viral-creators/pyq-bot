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
        r.add(UiNode(viewId = P + "conversation_contact_name", text = title, className = "android.widget.TextView"))
        r.add(UiNode(viewId = P + "entry", editable = true, className = "android.widget.EditText"))
        r.add(UiNode(viewId = P + (if (draftTyped) "send" else "voice_note_btn"), desc = if (draftTyped) "Send" else "Voice message", clickable = true))
        return r
    }
}

class RecordingActions : NodeActions {
    val clicks = mutableListOf<UiNode>()
    val typed = mutableListOf<String>()
    var scrolls = 0
    override fun click(node: UiNode) = true.also { clicks += node }
    override fun setText(node: UiNode, text: String) = true.also { typed += text }
    override fun scrollForward(node: UiNode) = true.also { scrolls++ }
}
