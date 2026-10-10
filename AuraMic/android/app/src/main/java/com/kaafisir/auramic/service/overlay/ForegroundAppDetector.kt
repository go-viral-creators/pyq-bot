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
        var current: String? = null
        var lastQuery = System.currentTimeMillis() - INITIAL_LOOKBACK_MS
        while (true) {
            if (!power.isInteractive) {
                current = null
                emit(null)
                delay(SCREEN_OFF_POLL_MS)
                lastQuery = System.currentTimeMillis() - 2_000
                continue
            }
            val now = System.currentTimeMillis()
            current = drain(current, lastQuery, now)
            lastQuery = now - 500 // slight overlap so no event is missed; re-applying is idempotent
            emit(current)
            delay(POLL_MS)
        }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    /** Applies all resume/pause events in (from, to] to [start] and returns the new foreground package. */
    private fun drain(start: String?, from: Long, to: Long): String? {
        var result = start
        val events = try {
            usage.queryEvents(from, to)
        } catch (_: SecurityException) {
            return result
        }
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                // 1 = ACTIVITY_RESUMED / MOVE_TO_FOREGROUND
                1 -> result = e.packageName
                // 2 = ACTIVITY_PAUSED / MOVE_TO_BACKGROUND
                2 -> if (result == e.packageName) result = null
            }
        }
        return result
    }

    private companion object {
        const val POLL_MS = 450L
        const val SCREEN_OFF_POLL_MS = 2_000L
        const val INITIAL_LOOKBACK_MS = 10 * 60 * 1000L
    }
}
