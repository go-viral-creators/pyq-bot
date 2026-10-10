package com.kaafisir.auramic

import android.app.Application
import com.kaafisir.auramic.audio.DfnEnhancer
import com.kaafisir.auramic.automation.WhatsAppSender
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class AuraMicApp : Application() {
    @Inject lateinit var dfn: DfnEnhancer
    @Inject lateinit var pro: com.kaafisir.auramic.data.pro.ProAccess

    override fun onCreate() {
        super.onCreate()
        kotlinx.coroutines.MainScope().launch { runCatching { pro.refresh() } } // subscription + free tries, works offline
        dfn.warmUp() // load the DeepFilterNet models in the background so the first voice note is fast
        // Housekeeping: processed voice notes are throw-away copies; drop anything older than an hour.
        Thread {
            runCatching {
                val dir = File(cacheDir, WhatsAppSender.SHARE_DIR)
                val cutoff = System.currentTimeMillis() - 60 * 60 * 1000L
                dir.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
            }
        }.start()
    }
}
