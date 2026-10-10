package com.kaafisir.auramic.data.pro

import android.app.Activity
import com.kaafisir.auramic.domain.model.Quality
import com.kaafisir.auramic.domain.model.UserSettings
import kotlinx.coroutines.flow.StateFlow

/** Everything about "Pro": subscription state, free High Quality tries, and buying. */
interface ProAccess {
    val state: StateFlow<ProState>

    /** Re-reads the subscription from Google Play (works offline from the Play cache) and the free-try counter. */
    suspend fun refresh()

    /** Opens the Google Play purchase sheet. Returns false if the store is not available. */
    fun purchase(activity: Activity, plan: Plan): Boolean

    /** Counts one High Quality note against the free tries (no-op for subscribers). */
    suspend fun consumeFreeHighNote()

    fun resolve(settings: UserSettings): Quality = resolveQuality(settings.quality, state.value)
}
