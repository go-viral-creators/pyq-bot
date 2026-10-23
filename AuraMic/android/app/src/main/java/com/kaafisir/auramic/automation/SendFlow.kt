package com.kaafisir.auramic.automation

/**
 * State machine that drives the chat app's share screen after we fire ACTION_SEND:
 *
 *   picker visible -> open the target chat (visible row -> the picker's search) -> tap "Send" -> confirm it went away.
 *
 * [step] is called repeatedly (on a timer) with a fresh snapshot of the active window. It is pure logic over
 * [UiNode]/[NodeActions] so it is covered by unit tests.
 *
 * Time budget (the user is waiting, so everything is short; whenever we give up, nothing has been sent and the user
 * simply finishes by hand):
 *  - the chat list must show up within [pickerWaitMs];
 *  - from the moment the list is on screen, [findMs] to find and tap the chat (the visible rows first, then the
 *    picker's search box);
 *  - [sendWaitMs] to find "Send" after the chat was tapped.
 *
 * Safety rules:
 *  - "Send" is only ever tapped after *we* selected a chat row; otherwise a user's half-typed message could be sent.
 *  - never on a conversation screen: there "Send" belongs to the user's own message box.
 *  - while the original conversation is still on screen we never type or tap anything.
 *  - without a readable chat name we never guess ([allowFirstRow] is a user opt-in): [Outcome.Failed] right away.
 *  - two equally good rows are "not found", never a coin toss ([NodeFinder.chatRow]).
 *  - once Send was tapped the result is never "failed": either the preview went away ([Outcome.Done]) or we cannot
 *    tell ([Outcome.Unknown]) - telling the user to send again then would create a duplicate.
 *  - Send is tapped at most twice, and the second time only when the very same button is still on screen.
 */
class SendFlow(
    private val chatName: String?,
    private val now: () -> Long,
    /** How long to wait for the chat list to appear after the share intent. */
    private val pickerWaitMs: Long = 6_000,
    /** How long to look for the chat once the list is on screen. */
    private val findMs: Long = 5_000,
    /** How long to wait for "Send" after the chat was tapped. */
    private val sendWaitMs: Long = 6_000,
    private val allowFirstRow: Boolean = false,
    /** How long to wait for the preview to close after Send was tapped. */
    private val confirmMs: Long = 6_000,
) {
    sealed interface Outcome {
        data object Continue : Outcome
        /** The send screen closed after we tapped Send: the note was sent. */
        data object Done : Outcome
        /** Send was never tapped: nothing was sent. */
        data class Failed(val reason: String) : Outcome
        /** Send was tapped but its effect could not be confirmed (screen left, timeout). The note may have been sent. */
        data class Unknown(val reason: String) : Outcome
    }

    private val startedAt = now()
    private var chatSelected = false
    private var rowClickedAt = 0L
    private var rowClicks = 0
    private var typedSearch = false
    private var openedSearch = false
    private var searchOpenedAt = 0L
    /** When the chat list was first seen: the clock for finding the chat starts here. */
    private var pickerSince = -1L
    private var sendClickedAt = 0L
    private var sendClicks = 0
    private var tappedSend: UiNode? = null
    private var pickerAfterSendSince = 0L

    /** True once Send was tapped: from then on the note may be on its way. */
    val sendTapped: Boolean get() = sendClicks > 0

    fun step(root: UiNode?, actions: NodeActions): Outcome {
        val t = now()

        // ---- phase 3: Send tapped, wait for the send screen to close ------------------------------------------------
        if (sendClicks > 0) return confirm(root, actions, t)

        val key = ChatKey.parse(chatName)
        // No readable chat name: we never guess. The user picks, so tell them at once instead of making them wait.
        if (key == null && !allowFirstRow && !chatSelected) return Outcome.Failed("chat name unknown")

        // ---- phase 2: chat chosen, tap Send as soon as it appears ----------------------------------------------------
        if (chatSelected) {
            // A conversation means WhatsApp jumped to the chat instead of showing a preview: its Send is the user's.
            val send = if (root == null || NodeFinder.isConversation(root)) null
            else NodeFinder.sendButton(root, allowPositional = t - rowClickedAt > 1_200)
            if (send != null) {
                if (actions.click(send) || actions.tap(send)) {
                    sendClicks = 1; sendClickedAt = t; tappedSend = send
                }
                return Outcome.Continue
            }
            if (t - rowClickedAt > sendWaitMs) return Outcome.Failed("send button not found")
            if (root != null && t - rowClickedAt > 1_600 && rowClicks < 3 && NodeFinder.isPicker(root)) {
                // The row click was swallowed (still on the picker): try again with a real tap on the row.
                val row = if (key != null) NodeFinder.chatRow(root, chatName!!) else if (allowFirstRow) NodeFinder.firstRow(root) else null
                if (row != null) { actions.tap(row); rowClicks++; rowClickedAt = t }
            }
            return Outcome.Continue
        }

        // ---- phase 1: find and open the target chat ------------------------------------------------------------------
        // Still looking at the conversation we came from (or nothing readable yet): wait for the share screen.
        if (root == null || (!NodeFinder.isPicker(root) && NodeFinder.hasComposer(root))) {
            return if (t - startedAt > pickerWaitMs) Outcome.Failed("share screen not shown") else Outcome.Continue
        }
        if (pickerSince < 0) pickerSince = t
        val elapsed = t - pickerSince

        val row = if (key != null) NodeFinder.chatRow(root, chatName!!) else NodeFinder.firstRow(root)
        if (row != null) {
            if (actions.click(row) || actions.tap(row)) { chatSelected = true; rowClickedAt = t; rowClicks = 1 }
            return Outcome.Continue
        }
        if (elapsed > findMs) return Outcome.Failed("chat not found")
        if (key == null) return Outcome.Continue                   // allowFirstRow and the list is still empty
        // Not among the visible rows: after a moment (the list fills in) use the picker's search, which finds any chat.
        if (elapsed < SEARCH_AFTER_MS) return Outcome.Continue
        val input = NodeFinder.searchInput(root)
        if (input != null) {
            if (!typedSearch) typedSearch = actions.setText(input, key.searchText)
            return Outcome.Continue
        }
        if (!openedSearch || t - searchOpenedAt > 1_200) {
            NodeFinder.searchButton(root)?.let { btn ->
                openedSearch = actions.click(btn) || actions.tap(btn)
                searchOpenedAt = t
            }
        }
        return Outcome.Continue
    }

    /**
     * After Send: the note is sent once the screen that held our Send button is gone. Only the button we tapped counts -
     * never a button found by position (that would be WhatsApp's mic / camera once the chat is back on screen).
     */
    private fun confirm(root: UiNode?, actions: NodeActions, t: Long): Outcome {
        val elapsed = t - sendClickedAt
        if (root == null) {
            // The chat app is not in front any more (user switched app, notification shade...): cannot see the result.
            return if (elapsed > confirmMs) Outcome.Unknown("chat app left after send") else Outcome.Continue
        }
        // Back on a conversation (or WhatsApp's camera / any other screen without our button): the preview closed.
        if (NodeFinder.isConversation(root)) return Outcome.Done
        val same = sameButton(root)
        if (same == null) {
            // The picker coming back without the preview means the send screen was dismissed - not proof of sending.
            if (NodeFinder.isPicker(root)) {
                if (pickerAfterSendSince == 0L) pickerAfterSendSince = t
                return if (t - pickerAfterSendSince > 2_000) Outcome.Unknown("picker visible after send") else Outcome.Continue
            }
            return Outcome.Done
        }
        pickerAfterSendSince = 0L
        // Our button is still there. One retry with the other input method, only for a button that is clearly idle.
        if (sendClicks == 1 && elapsed > RETRY_AFTER_MS && same.enabled) {
            actions.tap(same)
            sendClicks = 2
            return Outcome.Continue
        }
        return if (elapsed > confirmMs) Outcome.Unknown("send screen still open") else Outcome.Continue
    }

    /** The node we tapped as Send, if it is still on screen (same id, or for id-less buttons the same bounds and class). */
    private fun sameButton(root: UiNode): UiNode? {
        val prev = tappedSend ?: return null
        return root.walk().firstOrNull { n ->
            if (prev.viewId != null) n.viewId == prev.viewId && (n.clickable || n.clickableSelfOrAncestor() != null)
            else n.viewId == null && n.className == prev.className && n.bounds == prev.bounds && n.clickable
        }
    }

    private companion object {
        const val RETRY_AFTER_MS = 3_000L
        /** After the chat list appeared, how long the visible rows get before the search box is used. */
        const val SEARCH_AFTER_MS = 700L
    }
}
