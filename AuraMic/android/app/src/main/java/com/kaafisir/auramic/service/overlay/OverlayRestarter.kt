package com.kaafisir.auramic.service.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kaafisir.auramic.MainActivity
import com.kaafisir.auramic.R
import com.kaafisir.auramic.data.permissions.PermissionRepository
import com.kaafisir.auramic.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Brings the floating mic back after Android killed the process (app update, low memory, OEM task killers, reboot).
 *
 * The bubble runs in a microphone foreground service. Android only lets an app start one while the user is looking
 * at it, so a restart from the background (accessibility service reconnecting, boot) may be refused. Then - and when
 * a permission was revoked - we post a notification: tapping it opens AuraMic, which restarts the service from the
 * foreground. Restrictions are never worked around.
 */
@Singleton
class OverlayRestarter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val permissions: PermissionRepository,
) {
    /**
     * Starts the service if the user switched the floating mic on and it is not running.
     * [inForeground] = called while an AuraMic screen is visible (no notification needed then: the UI guides the user).
     */
    suspend fun restoreIfWanted(inForeground: Boolean) {
        if (OverlayService.isRunning.value) { cancelNotice(); return }
        val wanted = runCatching { settings.current().serviceEnabled }.getOrDefault(false)
        if (!wanted) { cancelNotice(); return }
        if (!permissions.refresh().requiredGranted) {
            if (!inForeground) notifyStopped(permissionMissing = true)
            return
        }
        val started = OverlayService.start(context)
        Log.i(TAG, "restore (foreground=$inForeground): start=$started")
        if (!started && !inForeground) notifyStopped(permissionMissing = false)
    }

    /** Called by the service when Android refused to make it a foreground service. */
    fun onStartRefused() {
        if (!OverlayService.isRunning.value) notifyStopped(permissionMissing = !permissions.refresh().requiredGranted)
    }

    fun cancelNotice() {
        runCatching { (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTICE_ID) }
    }

    private fun notifyStopped(permissionMissing: Boolean) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.areNotificationsEnabled()) return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, context.getString(R.string.notif_restore_channel), NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = PendingIntent.getActivity(
            context, 2, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mic)
            .setContentTitle(context.getString(R.string.notif_restore_title))
            .setContentText(context.getString(if (permissionMissing) R.string.notif_restore_perm else R.string.notif_restore_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        runCatching { nm.notify(NOTICE_ID, n) }.onFailure { Log.w(TAG, "notify failed", it) }
    }

    private companion object {
        const val TAG = "OverlayRestarter"
        const val CHANNEL_ID = "auramic_alerts"
        const val NOTICE_ID = 4202
    }
}
