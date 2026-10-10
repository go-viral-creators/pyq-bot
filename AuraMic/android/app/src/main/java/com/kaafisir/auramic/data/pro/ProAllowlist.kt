package com.kaafisir.auramic.data.pro

import android.content.Context
import android.provider.Settings

/**
 * Devices that always get Pro for free (the developer's own phones). Matching uses this app's Android ID, which Android
 * derives per device AND per app-signing key: a build signed with another key (e.g. the Play App Signing key) sees a
 * different ID, so Settings -> About shows the ID this build sees; add it here if it differs.
 */
object ProAllowlist {
    private val IDS = setOf("92ae6fd1c66039c6")

    fun deviceId(context: Context): String =
        runCatching { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) }.getOrNull().orEmpty()

    fun isAllowedId(id: String): Boolean = id.trim().lowercase() in IDS

    fun isAllowed(context: Context): Boolean = isAllowedId(deviceId(context))
}
