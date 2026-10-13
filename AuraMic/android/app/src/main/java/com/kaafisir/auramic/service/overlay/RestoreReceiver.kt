package com.kaafisir.auramic.service.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** After a reboot or an AuraMic update the process is gone: restore the floating mic if the user had it on. */
@AndroidEntryPoint
class RestoreReceiver : BroadcastReceiver() {
    @Inject lateinit var restarter: OverlayRestarter

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            try { restarter.restoreIfWanted(inForeground = false) } finally { pending.finish() }
        }
    }
}
