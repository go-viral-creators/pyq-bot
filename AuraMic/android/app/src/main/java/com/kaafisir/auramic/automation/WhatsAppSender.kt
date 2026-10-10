package com.kaafisir.auramic.automation

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Builds and fires the share intent that hands the processed file to WhatsApp. */
@Singleton
class WhatsAppSender @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bridge: AutomationBridge,
) {
    fun isInstalled(pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0); true
    } catch (_: PackageManager.NameNotFoundException) { false }

    fun shareDir(): File = File(context.cacheDir, SHARE_DIR)

    /** Privacy housekeeping: processed notes are throw-away copies; drop anything older than [maxAgeMs]. */
    fun purgeOldFiles(maxAgeMs: Long = 60 * 60 * 1000L) {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        runCatching { shareDir().listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() } }
    }

    fun buildIntent(file: File, mime: String, targetPackage: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            setPackage(targetPackage)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    /** Opens WhatsApp's "Send to..." screen with the file attached. Returns false if nothing could start it. */
    fun share(file: File, mime: String, targetPackage: String): Boolean {
        val intent = buildIntent(file, mime, targetPackage)
        // Prefer the accessibility service as launcher: it may start activities from the background.
        val viaA11y = runCatching { bridge.activityLauncher?.invoke(intent) == true }.getOrDefault(false)
        if (viaA11y) return true
        return try {
            context.startActivity(intent)
            true
        } catch (t: Throwable) {
            Log.w(TAG, "startActivity failed", t)
            false
        }
    }

    companion object {
        const val SHARE_DIR = "shared"
        private const val TAG = "WhatsAppSender"
    }
}
