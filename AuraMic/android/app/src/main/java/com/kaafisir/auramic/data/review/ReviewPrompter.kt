package com.kaafisir.auramic.data.review

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.play.core.review.ReviewManagerFactory
import com.kaafisir.auramic.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private val Context.reviewStore by preferencesDataStore(name = "auramic_review")

/**
 * Google Play In-App Review, asked at a good moment: when AuraMic opens after the user has sent at least
 * [MIN_NOTES] cleaned notes, and at most once every [INTERVAL_DAYS]. Play decides whether the sheet really appears
 * (quota), so it is never tied to a button. Builds not installed from Play simply do nothing.
 */
@Singleton
class ReviewPrompter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
) {
    private val askedAt = longPreferencesKey("asked_at")

    suspend fun maybeAsk(activity: Activity) {
        val notes = runCatching { settings.current().notesCleaned }.getOrDefault(0)
        val last = runCatching { context.reviewStore.data.first()[askedAt] ?: 0L }.getOrDefault(0L)
        if (!shouldAsk(notes, last, System.currentTimeMillis())) return
        runCatching {
            val manager = ReviewManagerFactory.create(context)
            val info = suspendCancellableCoroutine<com.google.android.play.core.review.ReviewInfo?> { c ->
                manager.requestReviewFlow().addOnCompleteListener { t -> c.resume(if (t.isSuccessful) t.result else null) }
            } ?: return
            context.reviewStore.edit { it[askedAt] = System.currentTimeMillis() }
            manager.launchReviewFlow(activity, info)
        }.onFailure { Log.w(TAG, "review flow unavailable", it) }
    }

    companion object {
        private const val TAG = "ReviewPrompter"
        const val MIN_NOTES = 5
        const val INTERVAL_DAYS = 120L

        fun shouldAsk(notesSent: Int, lastAskedAt: Long, now: Long): Boolean =
            notesSent >= MIN_NOTES && now - lastAskedAt > INTERVAL_DAYS * 86_400_000L
    }
}
