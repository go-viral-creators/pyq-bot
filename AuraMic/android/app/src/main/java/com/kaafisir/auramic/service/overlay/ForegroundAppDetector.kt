package com.kaafisir.auramic.service.overlay

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emits the package currently in the foreground, using UsageStatsManager events (needs the
 * "Usage access" special permission). Polling pauses while the screen is off to save battery.
 */
@Singleton
class ForegroundAppDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    val foregroundPackage: Flow<String?> = flow {
        val tracker = ResumedActivityTracker()
        var lastQuery = System.currentTimeMillis() - INITIAL_LOOKBACK_MS
        while (true) {
            if (!power.isInteractive) {
                tracker.reset()
                emit(null)
                delay(SCREEN_OFF_POLL_MS)
                lastQuery = System.currentTimeMillis() - 2_000
                continue
            }
            val now = System.currentTimeMillis()
            drain(tracker, lastQuery, now)
            lastQuery = now - 500 // slight overlap so no event is missed; re-applying is idempotent
            emit(tracker.packageName)
            delay(POLL_MS)
        }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    /** Applies all resume/pause events in (from, to] to [tracker]. */
    private fun drain(tracker: ResumedActivityTracker, from: Long, to: Long) {
        val events = try {
            usage.queryEvents(from, to)
        } catch (_: SecurityException) {
            return
        } ?: return
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            tracker.onEvent(e.eventType, e.packageName, e.className)
        }
    }

    private companion object {
        const val POLL_MS = 450L
        const val SCREEN_OFF_POLL_MS = 2_000L
        const val INITIAL_LOOKBACK_MS = 10 * 60 * 1000L
    }
}

/**
 * Which activity is in front, from usage events. Tracked per activity (package + class), not per package: when
 * WhatsApp opens a chat, Android 10+ (and several OEM builds) report the new activity's RESUMED *before* the old
 * one's PAUSED - both are com.whatsapp, so a package-only check decided WhatsApp had left and hid the bubble.
 */
class ResumedActivityTracker {
    private var pkg: String? = null
    private var cls: String? = null

    val packageName: String? get() = pkg

    fun reset() { pkg = null; cls = null }

    fun onEvent(type: Int, packageName: String?, className: String?) {
        when (type) {
            RESUMED -> { pkg = packageName; cls = className }
            // Only the activity that is in front can take the app out of the front.
            PAUSED -> if (packageName == pkg && (className == null || cls == null || className == cls)) { pkg = null; cls = null }
        }
    }

    companion object {
        const val RESUMED = 1   // UsageEvents.Event.ACTIVITY_RESUMED (MOVE_TO_FOREGROUND)
        const val PAUSED = 2    // UsageEvents.Event.ACTIVITY_PAUSED (MOVE_TO_BACKGROUND)
    }
}
