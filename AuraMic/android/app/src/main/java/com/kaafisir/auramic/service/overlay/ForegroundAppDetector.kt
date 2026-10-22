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
        val tracker = ForegroundTracker()
        var needsLookback = true
        while (true) {
            if (!power.isInteractive) {
                tracker.reset()
                emit(null)
                delay(SCREEN_OFF_POLL_MS)
                needsLookback = true      // after unlock, rebuild the picture from the recent history
                continue
            }
            val now = System.currentTimeMillis()
            // Every poll re-reads a window of recent events (not just "since last poll"): usage events can show up late
            // on some phones, and a missed RESUMED left the bubble missing until the next screen change.
            drain(tracker, now - if (needsLookback) INITIAL_LOOKBACK_MS else QUERY_WINDOW_MS, now + 1_000)
            needsLookback = false
            tracker.prune(now)
            emit(tracker.packageName)
            delay(POLL_MS)
        }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    /** Feeds all resume/pause events in [from, to] to [tracker] (it ignores the ones it already has). */
    private fun drain(tracker: ForegroundTracker, from: Long, to: Long) {
        val events = try {
            usage.queryEvents(from, to)
        } catch (_: SecurityException) {
            return
        } ?: return
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            tracker.onEvent(e.timeStamp, e.eventType, e.packageName, e.className)
        }
    }

    private companion object {
        const val POLL_MS = 450L
        const val QUERY_WINDOW_MS = 15_000L
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

    fun copy(): ResumedActivityTracker = ResumedActivityTracker().also { it.pkg = pkg; it.cls = cls }

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

/**
 * [ResumedActivityTracker] over a sliding window of events. Events are kept sorted by their own timestamp and
 * de-duplicated, so a window re-read on every poll - or an event that arrives late, out of order - still ends in the
 * right state. Events older than [keepMs] are folded into a base state, so a long stay in one app is not forgotten.
 */
class ForegroundTracker(private val keepMs: Long = 15_000L) {
    private class Ev(val t: Long, val type: Int, val pkg: String?, val cls: String?)
    private val window = ArrayList<Ev>()
    private val base = ResumedActivityTracker()

    fun reset() { window.clear(); base.reset() }

    fun onEvent(t: Long, type: Int, pkg: String?, cls: String?) {
        if (type != ResumedActivityTracker.RESUMED && type != ResumedActivityTracker.PAUSED) return
        if (window.any { it.t == t && it.type == type && it.pkg == pkg && it.cls == cls }) return
        var i = window.size
        while (i > 0 && window[i - 1].t > t) i--          // sorted by time; equal times keep their arrival order
        window.add(i, Ev(t, type, pkg, cls))
    }

    /** Folds events older than the window into the base state. */
    fun prune(now: Long) {
        while (window.isNotEmpty() && window[0].t < now - keepMs) {
            val e = window.removeAt(0)
            base.onEvent(e.type, e.pkg, e.cls)
        }
    }

    val packageName: String? get() {
        val t = base.copy()
        window.forEach { t.onEvent(it.type, it.pkg, it.cls) }
        return t.packageName
    }
}
