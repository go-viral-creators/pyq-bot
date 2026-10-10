package com.kaafisir.auramic.ui.components

import android.content.Context
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Starts an activity from Compose. LocalContext may be a locale-wrapped (non-Activity) context, so prefer the real
 * Activity and otherwise add FLAG_ACTIVITY_NEW_TASK. Failures (no handler) are swallowed and reported as false.
 */
@Composable
fun rememberStartActivity(): (Intent) -> Boolean {
    val activity = LocalActivity.current
    val context: Context = LocalContext.current
    return remember(activity, context) {
        { intent ->
            runCatching {
                if (activity != null) activity.startActivity(intent)
                else context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
        }
    }
}
