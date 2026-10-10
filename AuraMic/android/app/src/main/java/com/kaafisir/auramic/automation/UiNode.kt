package com.kaafisir.auramic.automation

data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/**
 * Immutable-ish snapshot of one accessibility node. The accessibility service converts the live
 * AccessibilityNodeInfo tree into these once per step, so all matching logic is plain Kotlin that
 * can be unit-tested on the JVM; [handle] points back at the live node for performing actions.
 */
class UiNode(
    val viewId: String? = null,
    val text: String? = null,
    val desc: String? = null,
    val className: String? = null,
    val clickable: Boolean = false,
    val enabled: Boolean = true,
    val scrollable: Boolean = false,
    val editable: Boolean = false,
    val bounds: Bounds = Bounds(0, 0, 0, 0),
    val handle: Any? = null,
) {
    var parent: UiNode? = null
        private set
    val children = mutableListOf<UiNode>()

    fun add(child: UiNode): UiNode {
        child.parent = this
        children += child
        return child
    }

    /** Depth-first walk including this node. */
    fun walk(): Sequence<UiNode> = sequence {
        yield(this@UiNode)
        for (c in children) yieldAll(c.walk())
    }

    /** This node, or the nearest ancestor, that can be clicked. */
    fun clickableSelfOrAncestor(): UiNode? {
        var n: UiNode? = this
        while (n != null) {
            if (n.clickable && n.enabled) return n
            n = n.parent
        }
        return null
    }

    fun hasId(name: String): Boolean = viewId?.endsWith(":id/$name") == true
}

/** Actions the matching logic can ask the service to perform on a snapshot node. */
interface NodeActions {
    fun click(node: UiNode): Boolean
    fun setText(node: UiNode, text: String): Boolean
    fun scrollForward(node: UiNode): Boolean
}
