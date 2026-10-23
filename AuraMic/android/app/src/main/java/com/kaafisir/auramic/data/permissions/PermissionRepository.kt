package com.kaafisir.auramic.data.permissions

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import androidx.core.content.ContextCompat
import com.kaafisir.auramic.domain.model.PermissionState
import com.kaafisir.auramic.service.accessibility.AuraAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Reads the state of all permissions and builds the intents that grant them. */
@Singleton
class PermissionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _state = MutableStateFlow(read())
    val state: StateFlow<PermissionState> = _state.asStateFlow()

    /** Re-reads everything; call from onResume and after returning from a settings screen. */
    fun refresh(): PermissionState = read().also { _state.value = it }

    private fun read() = PermissionState(
        overlay = Settings.canDrawOverlays(context),
        microphone = granted(Manifest.permission.RECORD_AUDIO),
        usageAccess = hasUsageAccess(),
        accessibility = isAccessibilityEnabled(),
        notifications = Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS),
        batteryExempt = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(context.packageName),
    )

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    private fun hasUsageAccess(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isAccessibilityEnabled(): Boolean {
        val component = ComponentName(context, AuraAccessibilityService::class.java)
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val viaManager = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo?.serviceInfo?.let { s -> s.packageName == component.packageName && s.name == component.className } == true }
        if (viaManager) return true
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(':').any {
            it.equals(component.flattenToString(), true) || it.equals(component.flattenToShortString(), true)
        }
    }

    // ---- intents ---------------------------------------------------------------------

    private fun newTask(i: Intent) = i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun overlayIntent(): Intent = newTask(
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
    )

    fun usageAccessIntent(): Intent = newTask(
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            if (Build.VERSION.SDK_INT >= 29) data = Uri.parse("package:${context.packageName}")
        }
    )

    fun accessibilityIntent(): Intent {
        val component = ComponentName(context, AuraAccessibilityService::class.java).flattenToString()
        return newTask(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            // Many AOSP-based settings apps scroll to / highlight this entry.
            putExtra(":settings:fragment_args_key", component)
            putExtra(":settings:show_fragment_args", android.os.Bundle().apply { putString(":settings:fragment_args_key", component) })
        })
    }

    fun appDetailsIntent(): Intent = newTask(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    )

    fun batteryIntent(): Intent = newTask(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

    /** Help for keeping AuraMic alive on this phone (see [KeepRunningGuide]). */
    fun keepRunningGuide(): KeepRunningGuide.Guide = KeepRunningGuide.forDevice(Build.MANUFACTURER, Build.BRAND)

    /**
     * Opens the maker's Autostart / background page if this phone has one that we know; otherwise AuraMic's own info
     * page (Battery is one tap away there). Returns false only when nothing could be opened.
     */
    fun openAutostart(): Boolean {
        for ((pkg, cls) in keepRunningGuide().autostartTargets) {
            val i = newTask(Intent().setClassName(pkg, cls))
            if (runCatching { context.startActivity(i) }.isSuccess) return true
        }
        return runCatching { context.startActivity(appDetailsIntent()) }.isSuccess
    }
}
