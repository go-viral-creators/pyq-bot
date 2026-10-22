package com.kaafisir.auramic.service.overlay

import android.content.Context
import android.os.Build
import com.kaafisir.auramic.BuildConfig
import com.kaafisir.auramic.automation.AutomationBridge
import com.kaafisir.auramic.data.permissions.PermissionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Text report for "the floating mic is not showing": phone, versions, permissions, what the services see right now and
 * the latest show/hide decisions. Contains no messages, contacts or screen content. Copied from Settings -> Troubleshoot.
 */
@Singleton
class MicDiagnostics @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissions: PermissionRepository,
    private val bridge: AutomationBridge,
) {
    fun report(): String {
        val p = permissions.refresh()
        fun version(pkg: String) = runCatching { context.packageManager.getPackageInfo(pkg, 0).versionName ?: "?" }.getOrElse { "not installed" }
        fun ok(v: Boolean) = if (v) "yes" else "NO"
        val anchor = bridge.micAnchor.value
        return buildString {
            appendLine("AuraMic ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("WhatsApp: ${version("com.whatsapp")}   Business: ${version("com.whatsapp.w4b")}")
            appendLine()
            appendLine("Permissions: overlay=${ok(p.overlay)} mic=${ok(p.microphone)} usageAccess=${ok(p.usageAccess)} " +
                "accessibility=${ok(p.accessibility)} notifications=${ok(p.notifications)} batteryUnrestricted=${ok(p.batteryExempt)}")
            appendLine("Floating mic service running: ${ok(OverlayService.isRunning.value)}")
            appendLine("Accessibility service connected: ${ok(bridge.connected.value)}")
            appendLine("Now: tracking=${bridge.anchorTracking.value} a11ySees=${bridge.a11yPackage.value ?: "none"} " +
                "chatOpen=${bridge.chatOpen.value} micFound=${anchor != null}" + (anchor?.let { " size=${it.size}px mic=${it.isMic}" } ?: ""))
            appendLine()
            appendLine("Latest decisions (oldest first):")
            appendLine(bridge.traceText().ifEmpty { "(nothing yet - open a WhatsApp chat first)" })
        }
    }
}
