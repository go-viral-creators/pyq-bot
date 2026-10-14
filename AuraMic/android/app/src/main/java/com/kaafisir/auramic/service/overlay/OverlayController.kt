package com.kaafisir.auramic.service.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.kaafisir.auramic.R
import com.kaafisir.auramic.core.Haptics
import com.kaafisir.auramic.ui.theme.AuraTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Owns the two overlay windows:
 *  - the *bubble* (touchable, draggable, holds the mic), and
 *  - a *pill* (full-width, never touchable) that shows timer / status above or below the bubble.
 * Both render Compose via [ComposeView] and are attached only while WhatsApp is in the foreground.
 */
class OverlayController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val orchestrator: VoiceNoteOrchestrator,
    private val haptics: Haptics,
    private val onPositionSaved: (x: Int, y: Int) -> Unit,
    private val onFreeModeChanged: (free: Boolean) -> Unit = {},
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density
    private fun dp(v: Float) = (v * density).roundToInt()

    private val owner = OverlayLifecycleOwner()
    private var bubbleRoot: TouchFrame? = null
    private var pillView: ComposeView? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var pillParams: WindowManager.LayoutParams
    private var stateJob: Job? = null
    private var snapAnimator: ValueAnimator? = null

    private var sizeDp = 64
    private var bx = -1 // bubble top-left, in collapsed coordinates
    private var by = -1
    private var holding = false
    private var lastSavedX = -2
    private var lastBubbleGeometry = IntArray(0)
    private var lastPillGeometry = IntArray(0)
    private var currentPackage = "com.whatsapp"
    private val pillAnchor = mutableIntStateOf(0)

    // --- sitting on top of WhatsApp's own voice button -------------------------------------------------------------
    private var anchor: com.kaafisir.auramic.automation.MicAnchor? = null
    private val bubbleDp = mutableFloatStateOf(64f)
    private val flatState = mutableStateOf(false)
    /**
     * Triple tap toggles this. false (default) = "fixed": the bubble covers WhatsApp's mic and cannot be dragged.
     * true = "free": the user drags it anywhere and it stays where it was dropped (no anchoring, no snapping).
     */
    private var freeMode = false
    /** Anchored = the bubble covers WhatsApp's mic; it then neither drags nor snaps and has the mic's exact size. */
    private val anchoredNow get() = if (freeMode) null else anchor?.takeIf { it.isMic }
    /** True while the bubble sits on top of WhatsApp's mic (fixed mode). */
    val isAnchored: Boolean get() = isAttached && anchoredNow != null
    private fun freePx() = dp(sizeDp.toFloat())
    private fun sizePx(): Int = anchoredNow?.let { it.size.coerceIn(dp(32f), dp(84f)) } ?: freePx()

    /** Called with the live position of WhatsApp's voice button (null when not in a chat). */
    fun setAnchor(a: com.kaafisir.auramic.automation.MicAnchor?) {
        if (a == anchor) return
        anchor = a
        if (isAttached) applyLayout()
    }

    val isAttached: Boolean get() = bubbleRoot != null

    /** Applies user settings (size / saved position) - cheap to call repeatedly. */
    fun configure(sizeDp: Int, savedX: Int, savedY: Int, free: Boolean = freeMode) {
        val resized = sizeDp != this.sizeDp
        this.sizeDp = sizeDp
        if (free != freeMode) { freeMode = free; if (isAttached) applyLayout() }
        if (bx == -1 && savedX >= 0) { bx = savedX; by = savedY }
        // "Reset position" in Settings stores -1: go back to the default spot.
        if (savedX == -1 && lastSavedX != -1) resetPosition()
        lastSavedX = savedX
        if (isAttached && resized) { rebuild() }
    }

    fun resetPosition() {
        bx = -1; by = -1
        if (isAttached) { ensurePosition(); applyLayout(); onPositionSaved(bx, by) }
    }

    fun setTargetPackage(pkg: String) { currentPackage = pkg }

    // ---------------------------------------------------------------------------------------
    // show / hide
    // ---------------------------------------------------------------------------------------

    fun show() {
        if (isAttached) return
        try {
            owner.resume()
            ensurePosition()
            val root = TouchFrame(context).also { bubbleRoot = it }
            val bubbleCompose = ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                setContent {
                    AuraTheme(darkTheme = true) {
                        val s by orchestrator.state.collectAsState()
                        val l by orchestrator.level.collectAsState()
                        BubbleWindowContent(s, l, androidx.compose.ui.unit.Dp(bubbleDp.floatValue), flat = flatState.value)
                    }
                }
            }
            root.addView(bubbleCompose, FrameLayout.LayoutParams(-1, -1))
            installOwners(root); installOwners(bubbleCompose)
            root.gesture = BubbleGesture(
                scheduler = { delay, action ->
                    val r = Runnable(action)
                    root.postDelayed(r, delay)
                    BubbleGesture.Cancelable { root.removeCallbacks(r) }
                },
                listener = gestureListener,
                slopPx = ViewConfiguration.get(context).scaledTouchSlop.toFloat() * 1.4f,
                cancelDistancePx = dp(72f).toFloat(),
            )

            val pill = ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                setContent {
                    AuraTheme(darkTheme = true) {
                        val s by orchestrator.state.collectAsState()
                        val l by orchestrator.level.collectAsState()
                        val p by orchestrator.progress.collectAsState()
                        PillWindowContent(s, l, pillAnchor.intValue, p)
                    }
                }
            }
            installOwners(pill)
            pillView = pill

            bubbleParams = baseParams(touchable = true).apply {
                width = sizePx(); height = sizePx()
            }
            pillParams = baseParams(touchable = false).apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
            applyLayout(updateViews = false)
            wm.addView(pill, pillParams)
            wm.addView(root, bubbleParams)

            stateJob = scope.launch { orchestrator.state.collect { applyLayout() } }
        } catch (t: Throwable) {
            // Overlay permission revoked / token problems: fail soft, the service decides what to do next.
            Log.w(TAG, "show() failed", t)
            hide()
            throw t
        }
    }

    fun hide() {
        stateJob?.cancel(); stateJob = null
        snapAnimator?.cancel()
        bubbleRoot?.let { runCatching { wm.removeViewImmediate(it) } }
        pillView?.let { runCatching { wm.removeViewImmediate(it) } }
        bubbleRoot = null; pillView = null
        holding = false
        lastBubbleGeometry = IntArray(0); lastPillGeometry = IntArray(0)
    }

    fun destroy() {
        hide()
        owner.destroy()
    }

    private fun rebuild() { hide(); show() }

    // ---------------------------------------------------------------------------------------
    // layout
    // ---------------------------------------------------------------------------------------

    private fun baseFlags(touchable: Boolean) =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            (if (touchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

    private fun baseParams(touchable: Boolean) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        baseFlags(touchable),
        PixelFormat.TRANSLUCENT,
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private fun screen(): Pair<Int, Int> = try {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            wm.currentWindowMetrics.bounds.let { it.width() to it.height() }
        } else {
            val m = DisplayMetrics()
            @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(m)
            m.widthPixels to m.heightPixels
        }
    } catch (_: Throwable) {
        context.resources.displayMetrics.let { it.widthPixels to it.heightPixels }
    }

    private val margin get() = dp(6f)

    private fun clampX(x: Int, w: Int) = x.coerceIn(margin, (w - freePx() - margin).coerceAtLeast(margin))
    private fun clampY(y: Int, h: Int) = y.coerceIn(dp(32f), (h - freePx() - dp(64f)).coerceAtLeast(dp(32f)))

    private fun ensurePosition() {
        val (w, h) = screen()
        if (bx < 0 || by < 0) {
            bx = w - freePx() - margin
            by = (h * 0.58f).roundToInt()
        }
        bx = clampX(bx, w); by = clampY(by, h)
    }

    /** Positions both windows for the current state (the bubble window grows around its centre while busy). */
    private fun applyLayout(updateViews: Boolean = true) {
        val anchored = anchoredNow
        val size = sizePx()
        val busy = orchestrator.state.value.isBusy || orchestrator.state.value is OverlayState.Done
        // Free-floating: the window is larger than the bubble so breathing / pressed-down scale-up and the glow are never clipped.
        // Anchored and idle: exactly the mic's size, so WhatsApp's neighbouring buttons stay tappable.
        val window = when {
            busy -> size * 2
            anchored != null -> size
            else -> (size * 1.5f).toInt()
        }
        val pad = (window - size) / 2
        val left = if (anchored != null) anchored.centerX - size / 2 else bx
        val top = if (anchored != null) anchored.centerY - size / 2 else by
        bubbleParams.width = window; bubbleParams.height = window
        bubbleParams.x = left - pad; bubbleParams.y = top - pad
        bubbleDp.floatValue = size / density
        flatState.value = anchored != null
        // Dragging only in "free" mode: in fixed mode a touch that moves simply records, like WhatsApp's own button.
        bubbleRoot?.gesture?.allowDrag = freeMode

        // While WhatsApp shows "Send" (text typed) its own button must be usable: hide and ignore touches.
        // (A bubble the user moved elsewhere does not cover it, so it stays.)
        val hideForSend = !freeMode && anchor != null && !anchor!!.isMic && !busy
        val flags = baseFlags(touchable = !hideForSend)
        bubbleParams.flags = flags
        bubbleRoot?.alpha = if (hideForSend) 0f else 1f

        val (w, h) = screen()
        val pillH = dp(48f)
        val above = top - pad - pillH - dp(6f)
        pillParams.y = if (above > dp(28f)) above else top + size + pad + dp(6f)
        pillParams.x = 0
        pillAnchor.intValue = left + size / 2
        if (updateViews) {
            // The recording ticker re-emits state every 100 ms; only touch WindowManager when geometry changed.
            val b = intArrayOf(bubbleParams.x, bubbleParams.y, bubbleParams.width, bubbleParams.height, flags)
            val p = intArrayOf(pillParams.x, pillParams.y)
            if (!b.contentEquals(lastBubbleGeometry)) {
                lastBubbleGeometry = b
                bubbleRoot?.takeIf { it.isAttachedToWindow }?.let { runCatching { wm.updateViewLayout(it, bubbleParams) } }
            }
            if (!p.contentEquals(lastPillGeometry)) {
                lastPillGeometry = p
                pillView?.takeIf { it.isAttachedToWindow }?.let { runCatching { wm.updateViewLayout(it, pillParams) } }
            }
        }
        if (w <= 0 || h <= 0) Log.w(TAG, "bad screen size")
    }

    /** Call after rotation / display size changes. */
    fun onConfigurationChanged() {
        if (!isAttached) return
        ensurePosition(); applyLayout()
    }

    private fun snapToEdge() {
        val (w, _) = screen()
        val size = freePx()
        val target = if (bx + size / 2 < w / 2) margin else w - size - margin
        snapAnimator?.cancel()
        snapAnimator = ValueAnimator.ofInt(bx, target).apply {
            duration = 220
            interpolator = DecelerateInterpolator()
            addUpdateListener { bx = it.animatedValue as Int; applyLayout() }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) { onPositionSaved(bx, by) }
            })
            start()
        }
    }

    // ---------------------------------------------------------------------------------------
    // gestures
    // ---------------------------------------------------------------------------------------

    private val gestureListener = object : BubbleGesture.Listener {
        override fun onHoldStart() {
            snapAnimator?.cancel()
            holding = orchestrator.press(currentPackage)
        }

        override fun onCancelArmed(armed: Boolean) { if (holding) orchestrator.setCancelArmed(armed) }

        override fun onRelease(cancelled: Boolean) {
            if (holding) orchestrator.release(cancelled)
            holding = false
        }

        override fun onDrag(dx: Float, dy: Float) {
            if (orchestrator.state.value.isBusy || anchoredNow != null) return
            val (w, h) = screen()
            bx = clampX(bx + dx.roundToInt(), w)
            by = clampY(by + dy.roundToInt(), h)
            applyLayout()
        }

        override fun onDragEnd() {
            haptics.tick()
            // Free mode: stays exactly where it was dropped (clamped on screen); saved for next time.
            if (freeMode) onPositionSaved(bx, by) else snapToEdge()
        }

        override fun onTap() { orchestrator.showHint(if (freeMode) R.string.ov_hint_free else R.string.ov_hint) }

        override fun onTripleTap() {
            if (orchestrator.state.value.isBusy) return
            val wasAnchored = anchoredNow
            freeMode = !freeMode
            if (freeMode) {
                // Start moving from where the bubble is now (on WhatsApp's mic) instead of jumping to the old spot.
                wasAnchored?.let { a -> val (w, h) = screen(); bx = clampX(a.centerX - freePx() / 2, w); by = clampY(a.centerY - freePx() / 2, h) }
                onPositionSaved(bx, by)
            }
            haptics.success()
            applyLayout()
            onFreeModeChanged(freeMode)
            orchestrator.showHint(if (freeMode) R.string.ov_free_on else R.string.ov_free_off)
        }
    }

    // ---------------------------------------------------------------------------------------

    /** Swallows every touch for itself and feeds raw coordinates to the gesture machine. */
    private class TouchFrame(context: Context) : FrameLayout(context) {
        var gesture: BubbleGesture? = null
        override fun onInterceptTouchEvent(ev: MotionEvent) = true
        override fun onTouchEvent(ev: MotionEvent): Boolean {
            val g = gesture ?: return false
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> g.down(ev.rawX, ev.rawY)
                MotionEvent.ACTION_MOVE -> g.move(ev.rawX, ev.rawY)
                MotionEvent.ACTION_UP -> g.up()
                MotionEvent.ACTION_CANCEL -> g.cancelEvent()
            }
            return true
        }
    }

    private fun installOwners(view: View) {
        view.setViewTreeLifecycleOwner(owner)
        view.setViewTreeSavedStateRegistryOwner(owner)
        view.setViewTreeViewModelStoreOwner(owner)
    }

    /** Minimal lifecycle host so Compose can run inside a Service-owned window. */
    private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry(this)
        private val savedState = SavedStateRegistryController.create(this)
        private val store = ViewModelStore()

        init {
            savedState.performAttach()
            savedState.performRestore(null)
            registry.currentState = Lifecycle.State.CREATED
        }

        fun resume() { if (registry.currentState != Lifecycle.State.DESTROYED) registry.currentState = Lifecycle.State.RESUMED }
        fun destroy() { registry.currentState = Lifecycle.State.DESTROYED; store.clear() }

        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry
        override val viewModelStore: ViewModelStore get() = store
    }

    private companion object {
        const val TAG = "OverlayController"
    }
}
