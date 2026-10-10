package com.kaafisir.auramic.automation

/**
 * State machine that drives the chat app's share screen after we fire ACTION_SEND:
 *
 *   picker visible -> open the target chat (visible row -> scroll -> search) -> tap "Send".
 *
 * [step] is called repeatedly (on a timer) with a fresh snapshot of the active window. It is pure logic over
 * [UiNode]/[NodeActions] so it is covered by unit tests.
 *
 * Safety rules:
 *  - "Send" is only ever tapped after *we* selected a chat row; otherwise a user's half-typed message could be sent.
 *  - while the original conversation is still on screen we never type or tap anything.
 *  - [allowFirstRow] (user opt-in) picks the most recent chat when the open chat's name could not be read.
 */
class SendFlow(
    private val chatName: String?,
    private val now: () -> Long,
    private val timeoutMs: Long = 25_000,
    private val allowFirstRow: Boolean = false,
) {
    sealed interface Outcome {
        data object Continue : Outcome
        data object Done : Outcome
        data class Failed(val reason: String) : Outcome
    }

    private val startedAt = now()
    private var chatSelected = false
    private var rowClickedAt = 0L
    private var rowClicks = 0
    private var typedSearch = false
    private var openedSearch = false
    private var scrolls = 0
    private var stepsOnPicker = 0
    private var sendClickedAt = 0L
    private var sendClicks = 0

    fun step(root: UiNode?, actions: NodeActions): Outcome {
        val t = now()
        if (t - startedAt > timeoutMs) {
            return Outcome.Failed(if (chatSelected) "send button not found" else "chat not found")
        }
        if (root == null) return Outcome.Continue

        val send = NodeFinder.sendButton(root, allowPositional = chatSelected && t - rowClickedAt > 1_200)

        // ---- phase 3: Send tapped, wait for the button to go away ----------------------------------------------------
        if (sendClicks > 0) {
            if (send == null) return Outcome.Done
            if (t - sendClickedAt > 1_500) {
                if (sendClicks >= 3) return Outcome.Failed("send did not complete")
                // alternate between ACTION_CLICK and a real tap: some views only react to one of them
                if (sendClicks % 2 == 1) actions.tap(send) else actions.click(send)
                sendClicks++
                sendClickedAt = t
            }
            return Outcome.Continue
        }

        // ---- phase 2: chat chosen, tap Send as soon as it appears ----------------------------------------------------
        if (chatSelected) {
            if (send != null) {
                if (actions.click(send)) { sendClicks = 1; sendClickedAt = t }
                else if (actions.tap(send)) { sendClicks = 1; sendClickedAt = t }
            } else if (t - rowClickedAt > 1_600 && rowClicks < 3) {
                // The row click was swallowed (nothing changed): try again with a real tap on the row.
                val row = chatName?.let { NodeFinder.chatRow(root, it) } ?: if (allowFirstRow) NodeFinder.firstRow(root) else null
                if (row != null) { actions.tap(row); rowClicks++; rowClickedAt = t }
            }
            return Outcome.Continue
        }

        // ---- phase 1: find and open the target chat ------------------------------------------------------------------
        val assisted = chatName.isNullOrBlank() && !allowFirstRow
        if (assisted) return Outcome.Continue                      // the user picks, we never guess
        // Still looking at the conversation we came from: wait for the share screen.
        if (!NodeFinder.isPicker(root) && NodeFinder.hasComposer(root)) return Outcome.Continue
        stepsOnPicker++

        val row = if (chatName.isNullOrBlank()) NodeFinder.firstRow(root) else NodeFinder.chatRow(root, chatName)
        if (row != null) {
            if (actions.click(row) || actions.tap(row)) { chatSelected = true; rowClickedAt = t; rowClicks = 1 }
            return Outcome.Continue
        }
        if (chatName.isNullOrBlank()) return Outcome.Continue
        // Not visible: first nudge the list a couple of times (recent chats are near the top)...
        val list = NodeFinder.scrollable(root)
        if (!typedSearch && scrolls < 2 && list != null && stepsOnPicker > 1) {
            actions.scrollForward(list)
            scrolls++
            return Outcome.Continue
        }
        // ...then use the picker's search.
        NodeFinder.searchInput(root)?.let { input ->
            if (!typedSearch) {
                typedSearch = actions.setText(input, chatName)
            } else if (scrolls < 6 && list != null) {
                actions.scrollForward(list) // result list may be longer than the screen
                scrolls++
            }
            return Outcome.Continue
        }
        if (!openedSearch) {
            NodeFinder.searchButton(root)?.let { btn ->
                openedSearch = actions.click(btn) || actions.tap(btn)
            }
        }
        return Outcome.Continue
    }
}
