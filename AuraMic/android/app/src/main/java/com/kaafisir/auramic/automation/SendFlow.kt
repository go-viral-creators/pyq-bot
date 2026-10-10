package com.kaafisir.auramic.automation

/**
 * State machine that drives WhatsApp's share screen after we fire ACTION_SEND:
 *
 *   picker visible -> open the target chat (visible row -> scroll -> search) -> tap "Send".
 *
 * [step] is called repeatedly (on accessibility events and on a timer) with a fresh snapshot of
 * the active window. It is pure logic over [UiNode]/[NodeActions] so it is covered by unit tests.
 *
 * Safety rule: "Send" is only ever tapped after *we* selected the chat row. Without that, a
 * user's half-typed text message in a different chat could be sent by mistake.
 */
class SendFlow(
    private val chatName: String?,
    private val now: () -> Long,
    private val timeoutMs: Long = 25_000,
) {
    sealed interface Outcome {
        data object Continue : Outcome
        data object Done : Outcome
        data class Failed(val reason: String) : Outcome
    }

    private val startedAt = now()
    private var chatSelected = false
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

        val send = NodeFinder.sendButton(root)

        // ---- phase 3: Send tapped, wait for the button to go away --------------------------------
        if (sendClicks > 0) {
            if (send == null) return Outcome.Done
            if (t - sendClickedAt > 1_500) {
                if (sendClicks >= 3) return Outcome.Failed("send did not complete")
                actions.click(send)
                sendClicks++
                sendClickedAt = t
            }
            return Outcome.Continue
        }

        // ---- phase 2: chat chosen, tap Send as soon as it appears ----------------------------------
        if (chatSelected) {
            if (send != null && actions.click(send)) {
                sendClicks = 1
                sendClickedAt = t
            }
            return Outcome.Continue
        }

        // ---- phase 1: find and open the target chat ------------------------------------------------
        if (chatName.isNullOrBlank()) return Outcome.Continue // assisted mode: the user picks, we never guess
        if (!NodeFinder.isPicker(root) && NodeFinder.searchInput(root) == null) return Outcome.Continue // still on the chat screen
        stepsOnPicker++

        NodeFinder.chatRow(root, chatName)?.let { row ->
            if (actions.click(row)) chatSelected = true
            return Outcome.Continue
        }
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
                openedSearch = actions.click(btn)
            }
        }
        return Outcome.Continue
    }
}
