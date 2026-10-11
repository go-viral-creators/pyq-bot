package com.kaafisir.auramic.service.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.kaafisir.auramic.automation.AutomationBridge
import com.kaafisir.auramic.automation.Bounds
import com.kaafisir.auramic.automation.MicAnchor
import com.kaafisir.auramic.automation.MicAnchorFinder
import com.kaafisir.auramic.automation.NodeActions
import com.kaafisir.auramic.automation.NodeDump
import com.kaafisir.auramic.automation.NodeFinder
import com.kaafisir.auramic.automation.SendFlow
import com.kaafisir.auramic.automation.SendRequest
import com.kaafisir.auramic.automation.SendResult
import com.kaafisir.auramic.automation.UiNode
import com.kaafisir.auramic.automation.WhatsAppUi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The only job of this service: finish the hand-off inside WhatsApp.
 *
 *  - it is restricted (accessibility_service_config.xml) to com.whatsapp / com.whatsapp.w4b;
 *  - when the overlay posts a [SendRequest], it drives [SendFlow] against snapshots of the
 *    active WhatsApp window: choose the chat that was open, then tap "Send";
 *  - it can also tell the overlay which chat is currently open (so we know where to send).
 * It never reads, stores or transmits message content.
 */
@AndroidEntryPoint
class AuraAccessibilityService : AccessibilityService() {

    @Inject lateinit var bridge: AutomationBridge

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var runJob: Job? = null
    /** Wakes the mic-anchor tracker when WhatsApp's screen changes (keyboard, typing, new chat). */
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var lastFullScan = 0L
    private var lastAnchor: MicAnchor? = null
    private var lastAnchorAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        bridge.chatTitleProvider = { readChatTitle() }
        bridge.activityLauncher = { intent -> launch(intent) }
        bridge.setConnected(true)
        scope.launch { bridge.pending.collect { req -> if (req != null) start(req) else runJob?.cancel() } }
        scope.launch { bridge.anchorTracking.collectLatest { on -> if (on) trackMic() else bridge.setMicAnchor(null) } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events only wake the mic-anchor tracker (below); their content is never read.
        if (bridge.anchorTracking.value) wake.trySend(Unit)
    }

    override fun onInterrupt() { runJob?.cancel() }

    override fun onUnbind(intent: Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun teardown() {
        runJob?.cancel()
        bridge.setConnected(false)
        bridge.chatTitleProvider = null
        bridge.activityLauncher = null
        scope.cancel()
    }

    private fun launch(intent: Intent): Boolean = try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (t: Throwable) {
        Log.w(TAG, "launch failed", t)
        false
    }

    // ---------------------------------------------------------------------------------------
    // Mic anchor: where WhatsApp's own voice button is, so the bubble can sit exactly on top of it
    // ---------------------------------------------------------------------------------------

    private suspend fun trackMic() {
        while (true) {
            val now = SystemClock.elapsedRealtime()
            var seen = runCatching { readMicAnchor() }.getOrNull()
            // Text in the message box = WhatsApp's button is "Send" now, whatever its id / label says: get out of its way.
            if (composerHasText) {
                seen = (seen ?: lastAnchor)?.let { MicAnchor(it.bounds, false) }
            }
            if (seen != null) { lastAnchor = seen; lastAnchorAt = now }
            // Keep the last position for a moment when a read misses (screen transition): the bubble must not jump around.
            val anchor = seen ?: lastAnchor?.takeIf { now - lastAnchorAt < ANCHOR_HOLD_MS }
            if (anchor == null) lastAnchor = null
            bridge.setMicAnchor(anchor)
            bridge.setChatOpen(anchor != null || runCatching { conversationVisible }.getOrDefault(false))
            withTimeoutOrNull(TRACK_MS) { wake.receive() }
            delay(60) // coalesce bursts of events
        }
    }

    /** Set by [readMicAnchor]: a conversation screen (title or message box) is visible even if the voice button was not found. */
    @Volatile private var conversationVisible = false
    /** Set by [readMicAnchor]: the message box holds typed text (so WhatsApp shows its Send button, not the mic). */
    @Volatile private var composerHasText = false

    private fun readMicAnchor(): MicAnchor? {
        conversationVisible = false
        composerHasText = false
        val root = rootInActiveWindow ?: return null
        val pkg = root.packageName?.toString()
        try {
            if (pkg == null || pkg !in WhatsAppUi.PACKAGES) return null
            val dm = resources.displayMetrics
            run {
                val boxes = root.findAccessibilityNodeInfosByViewId("$pkg:id/entry")
                // An empty box exposes its hint ("Message") as text on some builds: hint, or no cursor in it (-1 / 0), means nothing typed.
                composerHasText = boxes.any {
                    val t = it.text?.toString()
                    it.isVisibleToUser && !t.isNullOrEmpty() && !it.isShowingHintText &&
                        t != it.hintText?.toString() && it.textSelectionEnd > 0
                }
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT < 33) boxes.forEach { runCatching { it.recycle() } }
            }
            // Fast path: look the button up by its resource id (no tree walk).
            for (id in MicAnchorFinder.SEND_ONLY_IDS + MicAnchorFinder.MIC_IDS) {
                val hits = root.findAccessibilityNodeInfosByViewId("$pkg:id/$id")
                try {
                    for (info in hits) {
                        if (!info.isVisibleToUser) continue
                        val r = Rect(); info.getBoundsInScreen(r)
                        val b = Bounds(r.left, r.top, r.right, r.bottom)
                        val kind = MicAnchorFinder.classify(info.viewIdResourceName, info.contentDescription?.toString(), info.text?.toString()) ?: continue
                        if (MicAnchorFinder.plausible(b, dm.widthPixels, dm.heightPixels)) {
                            return MicAnchor(b, kind == MicAnchorFinder.Kind.MIC)
                        }
                    }
                } finally {
                    @Suppress("DEPRECATION")
                    if (Build.VERSION.SDK_INT < 33) hits.forEach { runCatching { it.recycle() } }
                }
            }
            // Conversation screen without a visible voice button (e.g. WhatsApp changed its ids): title / message box present?
            for (id in listOf("conversation_contact_name")) {          // header of an open conversation; the chat list has none
                val hits = root.findAccessibilityNodeInfosByViewId("$pkg:id/$id")
                val found = hits.any { it.isVisibleToUser }
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT < 33) hits.forEach { runCatching { it.recycle() } }
                if (found) { conversationVisible = true; break }
            }
            // Slow path (unknown WhatsApp version): label-based search over the tree, at most once a second.
            val now = SystemClock.elapsedRealtime()
            if (now - lastFullScan < 1000) return null
            lastFullScan = now
            val snap = snapshot(null) ?: return null
            return try {
                snap.root?.let { r ->
                    MicAnchorFinder.find(r, dm.widthPixels, dm.heightPixels)
                }
            } finally { snap.release() }
        } finally {
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < 33) runCatching { root.recycle() }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Send automation
    // ---------------------------------------------------------------------------------------

    private fun start(request: SendRequest) {
        runJob?.cancel()
        runJob = scope.launch {
            val flow = SendFlow(request.chatName, now = { SystemClock.elapsedRealtime() }, allowFirstRow = request.allowFirstRow)
            val actions = LiveActions()
            var lastDump: String? = null
            var lastDumpAt = 0L
            while (isActive) {
                val snap = snapshot(request.targetPackage)
                val outcome = try {
                    // keep a cheap, throttled dump of what the flow is looking at: it is the evidence needed if it fails
                    val now = SystemClock.elapsedRealtime()
                    if (snap?.root != null && now - lastDumpAt > 700) { lastDump = NodeDump.format(snap.root); lastDumpAt = now }
                    flow.step(snap?.root, actions)
                } finally {
                    snap?.release()
                }
                when (outcome) {
                    SendFlow.Outcome.Continue -> delay(POLL_MS)
                    SendFlow.Outcome.Done -> { bridge.complete(request.id, SendResult.Sent); return@launch }
                    is SendFlow.Outcome.Failed -> {
                        Log.w(TAG, "send flow failed: ${outcome.reason}")
                        bridge.diagnostics = "reason=${outcome.reason} chat=${request.chatName?.take(3)}~ pkg=${request.targetPackage}\n" + (lastDump ?: "(no screen of the chat app was visible)")
                        bridge.complete(request.id, SendResult.Failed(outcome.reason)); return@launch
                    }
                }
            }
        }
    }

    private fun readChatTitle(): String? {
        val snap = snapshot(null) ?: return null
        return try {
            snap.root?.let { NodeFinder.chatTitle(it, resources.displayMetrics.heightPixels) }
        } finally {
            snap.release()
        }
    }

    private class Snapshot(val root: UiNode?, private val live: List<AccessibilityNodeInfo>) {
        @Suppress("DEPRECATION")
        fun release() {
            if (Build.VERSION.SDK_INT < 33) live.forEach { runCatching { it.recycle() } }
        }
    }

    private fun matches(pkg: String?, requiredPackage: String?) =
        pkg != null && pkg in WhatsAppUi.PACKAGES && (requiredPackage == null || pkg == requiredPackage)

    /** Finds the root of the chat app window: the active window first, then any window (share sheets can be dialogs). */
    private fun findRoot(requiredPackage: String?): AccessibilityNodeInfo? {
        val active = rootInActiveWindow
        if (active != null && matches(active.packageName?.toString(), requiredPackage)) return active
        @Suppress("DEPRECATION")
        if (active != null && Build.VERSION.SDK_INT < 33) active.recycle()
        return runCatching {
            windows.sortedByDescending { it.layer }.firstNotNullOfOrNull { w ->
                w.root?.takeIf { matches(it.packageName?.toString(), requiredPackage) }
            }
        }.getOrNull()
    }

    /** Converts the chat app window to [UiNode]s; null unless one of the supported chat apps (or the requested one) is showing. */
    private fun snapshot(requiredPackage: String?): Snapshot? {
        val rootInfo = findRoot(requiredPackage) ?: return null
        val live = mutableListOf(rootInfo)
        var root = build(rootInfo, 0, live, intArrayOf(MAX_NODES), true)
        if (root != null && root.walk().count() < 6) {
            // Some custom-drawn screens report almost nothing as "visible": retry without the visibility filter.
            root = build(rootInfo, 0, live, intArrayOf(MAX_NODES), false)
        }
        return Snapshot(root, live)
    }

    private fun build(info: AccessibilityNodeInfo, depth: Int, live: MutableList<AccessibilityNodeInfo>, budget: IntArray, requireVisible: Boolean): UiNode? {
        if ((requireVisible && !info.isVisibleToUser) || budget[0] <= 0) return null
        budget[0]--
        val r = Rect()
        info.getBoundsInScreen(r)
        val node = UiNode(
            viewId = info.viewIdResourceName,
            text = info.text?.toString(),
            desc = info.contentDescription?.toString(),
            className = info.className?.toString(),
            clickable = info.isClickable,
            enabled = info.isEnabled,
            scrollable = info.isScrollable,
            editable = info.isEditable,
            bounds = Bounds(r.left, r.top, r.right, r.bottom),
            handle = info,
        )
        if (depth < MAX_DEPTH) {
            for (i in 0 until info.childCount) {
                val child = info.getChild(i) ?: continue
                live += child
                build(child, depth + 1, live, budget, requireVisible)?.let { node.add(it) }
            }
        }
        return node
    }

    private inner class LiveActions : NodeActions {
        override fun click(node: UiNode): Boolean {
            val info = node.handle as? AccessibilityNodeInfo ?: return false
            if (info.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            return tap(node.bounds) // some WhatsApp views only react to real touches
        }

        override fun setText(node: UiNode, text: String): Boolean {
            val info = node.handle as? AccessibilityNodeInfo ?: return false
            info.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return info.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }

        override fun tap(node: UiNode): Boolean = tap(node.bounds)

        override fun scrollForward(node: UiNode): Boolean =
            (node.handle as? AccessibilityNodeInfo)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) == true
    }

    /** Fallback: dispatch a short synthetic tap at the centre of [b]. */
    private fun tap(b: Bounds): Boolean {
        if (b.width <= 0 || b.height <= 0) return false
        val path = Path().apply { moveTo((b.left + b.right) / 2f, (b.top + b.bottom) / 2f) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    companion object {
        private const val TAG = "AuraA11y"
        private const val POLL_MS = 220L
        private const val TRACK_MS = 450L
        private const val ANCHOR_HOLD_MS = 1_200L
        private const val MAX_DEPTH = 40
        private const val MAX_NODES = 900
    }
}
