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
import com.kaafisir.auramic.automation.NodeActions
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
import kotlinx.coroutines.flow.collect
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

    override fun onServiceConnected() {
        super.onServiceConnected()
        bridge.chatTitleProvider = { readChatTitle() }
        bridge.activityLauncher = { intent -> launch(intent) }
        bridge.setConnected(true)
        scope.launch { bridge.pending.collect { req -> if (req != null) start(req) else runJob?.cancel() } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally empty: all work is driven by SendRequests and polled snapshots, so we
        // do not touch (or even look at) events from the user's normal WhatsApp usage.
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
    // Send automation
    // ---------------------------------------------------------------------------------------

    private fun start(request: SendRequest) {
        runJob?.cancel()
        runJob = scope.launch {
            val flow = SendFlow(request.chatName, now = { SystemClock.elapsedRealtime() })
            val actions = LiveActions()
            while (isActive) {
                val snap = snapshot(request.targetPackage)
                val outcome = try {
                    flow.step(snap?.root, actions)
                } finally {
                    snap?.release()
                }
                when (outcome) {
                    SendFlow.Outcome.Continue -> delay(POLL_MS)
                    SendFlow.Outcome.Done -> { bridge.complete(request.id, SendResult.Sent); return@launch }
                    is SendFlow.Outcome.Failed -> { bridge.complete(request.id, SendResult.Failed(outcome.reason)); return@launch }
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

    /** Converts the active window to [UiNode]s; null unless it belongs to (the requested) WhatsApp. */
    private fun snapshot(requiredPackage: String?): Snapshot? {
        val rootInfo = rootInActiveWindow ?: return null
        val pkg = rootInfo.packageName?.toString()
        if (pkg == null || pkg !in WhatsAppUi.PACKAGES || (requiredPackage != null && pkg != requiredPackage)) {
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < 33) rootInfo.recycle()
            return null
        }
        val live = mutableListOf(rootInfo)
        val budget = intArrayOf(MAX_NODES)
        val root = build(rootInfo, 0, live, budget)
        return Snapshot(root, live)
    }

    private fun build(info: AccessibilityNodeInfo, depth: Int, live: MutableList<AccessibilityNodeInfo>, budget: IntArray): UiNode? {
        if (!info.isVisibleToUser || budget[0] <= 0) return null
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
                build(child, depth + 1, live, budget)?.let { node.add(it) }
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
        private const val MAX_DEPTH = 40
        private const val MAX_NODES = 900
    }
}
