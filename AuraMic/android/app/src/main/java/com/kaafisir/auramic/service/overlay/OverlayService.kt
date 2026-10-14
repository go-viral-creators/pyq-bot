package com.kaafisir.auramic.service.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.kaafisir.auramic.core.Haptics
import com.kaafisir.auramic.core.LocaleManager
import com.kaafisir.auramic.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service (type "microphone") that keeps the floating mic alive. It shows the bubble
 * only while WhatsApp / WhatsApp Business is the foreground app.
 */
@AndroidEntryPoint
class OverlayService : Service() {

    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var detector: ForegroundAppDetector
    @Inject lateinit var orchestrator: VoiceNoteOrchestrator
    @Inject lateinit var haptics: Haptics
    @Inject lateinit var sender: com.kaafisir.auramic.automation.WhatsAppSender
    @Inject lateinit var dfn: com.kaafisir.auramic.audio.DfnEnhancer
    @Inject lateinit var bridge: com.kaafisir.auramic.automation.AutomationBridge
    @Inject lateinit var restarter: OverlayRestarter

    private data class Quad(val pkg: String?, val chatOpen: Boolean, val anchored: Boolean, val connected: Boolean)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: OverlayController? = null
    /** True while WhatsApp is the foreground app (the bubble should be on screen). */
    private var wantVisible = false
    private val visibility = BubbleVisibility()
    /** onCreate could not become a foreground service: nothing else was set up. */
    private var refused = false

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(LocaleManager.wrap(newBase))

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val notification = ServiceNotification.build(this)
            if (Build.VERSION.SDK_INT >= 30) {
                ServiceCompat.startForeground(this, ServiceNotification.ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(ServiceNotification.ID, notification)
            }
        } catch (t: Throwable) {
            // Android 14+: the mic foreground type needs RECORD_AUDIO and a start while AuraMic is visible.
            // Tell the user instead of silently showing "on" with no bubble.
            Log.w(TAG, "cannot start foreground", t)
            refused = true
            restarter.onStartRefused()
            stopSelf()
            return
        }
        running.value = true
        restarter.cancelNotice()
        orchestrator.attach(scope)
        sender.purgeOldFiles()
        dfn.warmUp()

        val ctl = OverlayController(
            context = this,
            scope = scope,
            orchestrator = orchestrator,
            haptics = haptics,
            onPositionSaved = { x, y -> scope.launch { settingsRepo.setBubblePosition(x, y) } },
            onFreeModeChanged = { free -> scope.launch { settingsRepo.setBubbleFree(free) } },
        )
        controller = ctl

        scope.launch {
            settingsRepo.settings.collect { s ->
                haptics.enabled = s.haptics
                ctl.configure(s.bubbleSizeDp, s.bubbleX, s.bubbleY, s.bubbleFree)
            }
        }
        // Sit exactly on top of WhatsApp's own voice button whenever the accessibility service can see it.
        scope.launch { bridge.micAnchor.collect { ctl.setAnchor(it) } }
        // Track WhatsApp's screen (mic position / is a chat open?) whenever WhatsApp is in front.
        scope.launch {
            detector.foregroundPackage.map { it?.takeIf { p -> p in com.kaafisir.auramic.domain.model.ChatApp.allPackages } }
                .distinctUntilChanged()
                .collect { pkg ->
                    bridge.setAnchorTracking(pkg != null)
                    if (pkg != null) ctl.setTargetPackage(pkg)
                }
        }
        // The bubble is only for conversations: it is shown while a chat is open and hidden on the chat list, status,
        // settings, calls... (if accessibility is not enabled there is no way to tell, so then it follows WhatsApp).
        scope.launch {
            combine(detector.foregroundPackage, bridge.chatOpen, bridge.micAnchor, bridge.connected) { pkg, open, anchor, connected ->
                Quad(pkg?.takeIf { it in com.kaafisir.auramic.domain.model.ChatApp.allPackages }, open, anchor != null, connected)
            }
                .transformLatest { q ->
                    when (visibility.decide(q.pkg != null, q.chatOpen, q.anchored, q.connected)) {
                        BubbleVisibility.Decision.Hide -> emit(null)
                        BubbleVisibility.Decision.Show -> emit(q.pkg)
                        BubbleVisibility.Decision.ShowAfterGrace -> { delay(2_500); emit(q.pkg) }
                    }
                }
                .distinctUntilChanged()
                .let { f -> @OptIn(kotlinx.coroutines.FlowPreview::class) f.debounce { if (it == null) 60L else 0L } }
                .collect { pkg ->
                    wantVisible = pkg != null
                    if (pkg != null) {
                        try {
                            ctl.show()
                        } catch (t: Throwable) {
                            // "Display over other apps" was revoked: stop and point the user at the fix.
                            Log.e(TAG, "overlay permission lost?", t)
                            running.value = false
                            restarter.onStartRefused()
                            stopSelf()
                        }
                    } else if (!orchestrator.state.value.isBusy) {
                        ctl.hide()
                    }
                }
        }
        // Keep the bubble hidden when a recording finishes after the user already left WhatsApp.
        scope.launch {
            orchestrator.state.collect { s ->
                if (s === OverlayState.Idle && !wantVisible) ctl.hide()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            scope.launch { settingsRepo.setServiceEnabled(false) }
            stopSelf()
        }
        // Never restart on its own: Android 14+ forbids starting a microphone service from the background.
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        controller?.onConfigurationChanged()
    }

    override fun onDestroy() {
        running.value = false
        if (refused) { scope.cancel(); super.onDestroy(); return }
        bridge.setAnchorTracking(false)
        orchestrator.release()
        controller?.destroy()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "OverlayService"
        const val ACTION_STOP = "com.kaafisir.auramic.action.STOP"

        private val running = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = running.asStateFlow()

        fun start(context: Context): Boolean = try {
            androidx.core.content.ContextCompat.startForegroundService(context, Intent(context, OverlayService::class.java))
            true
        } catch (t: Throwable) {
            Log.w(TAG, "start failed", t)
            false
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
