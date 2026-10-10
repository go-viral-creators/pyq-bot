package com.kaafisir.auramic.service.tile

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.app.PendingIntent
import com.kaafisir.auramic.MainActivity
import com.kaafisir.auramic.R
import com.kaafisir.auramic.data.permissions.PermissionRepository
import com.kaafisir.auramic.domain.repository.SettingsRepository
import com.kaafisir.auramic.service.overlay.OverlayService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quick Settings tile: one-tap on/off for the floating mic. A tile tap counts as a user
 * interaction, which is what Android requires to (re)start a microphone foreground service.
 */
@AndroidEntryPoint
class AuraTileService : TileService() {
    @Inject lateinit var permissions: PermissionRepository
    @Inject lateinit var settings: SettingsRepository
    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { scope = it }
        OverlayService.isRunning.onEach { render(it) }.launchIn(s)
    }

    override fun onStopListening() {
        scope?.cancel(); scope = null
        super.onStopListening()
    }

    private fun render(running: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.app_name)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_stat_mic)
        if (Build.VERSION.SDK_INT >= 29) tile.subtitle = getString(if (running) R.string.common_on else R.string.common_off)
        tile.updateTile()
    }

    @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated") // Intent overload only used below API 34
    override fun onClick() {
        super.onClick()
        if (OverlayService.isRunning.value) {
            OverlayService.stop(this)
            CoroutineScope(Dispatchers.Main.immediate).launch { settings.setServiceEnabled(false) }
            return
        }
        if (permissions.refresh().requiredGranted && OverlayService.start(this)) {
            CoroutineScope(Dispatchers.Main.immediate).launch { settings.setServiceEnabled(true) }
        } else {
            // Not set up (or Android refused): open the app.
            val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
            } else {
                @Suppress("DEPRECATION") startActivityAndCollapse(intent)
            }
        }
    }
}
