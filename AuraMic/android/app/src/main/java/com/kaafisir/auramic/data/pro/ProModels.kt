package com.kaafisir.auramic.data.pro

import com.kaafisir.auramic.domain.model.Quality

/** Number of High Quality voice notes a free user can try. */
const val FREE_HIGH_NOTES = 10

/** The two subscription plans; [basePlanId] matches the base plan configured in Google Play Console. */
enum class Plan(val basePlanId: String) { MONTHLY("monthly"), YEARLY("yearly") }

data class PlanOffer(val plan: Plan, val price: String)

data class ProState(
    /** Active Google Play subscription. */
    val subscribed: Boolean = false,
    /** End of a free-Pro grant (redeemed admin code), or null. Independent of [subscribed]: either one gives Pro. */
    val grantUntil: Long? = null,
    /** High Quality notes already used from the free trial (never goes down, even if app data is cleared). */
    val trialUsed: Int = 0,
    /** Plans loaded from Google Play (empty while offline / store not available). */
    val offers: List<PlanOffer> = emptyList(),
) {
    val trialLeft: Int get() = (FREE_HIGH_NOTES - trialUsed).coerceAtLeast(0)
    /** Pro from either source: a Play subscription or a valid free grant. */
    val pro: Boolean get() = subscribed || (grantUntil != null && grantUntil > System.currentTimeMillis())
    /** True when High Quality can be used right now. */
    val highAvailable: Boolean get() = pro || trialLeft > 0
}

/**
 * Which quality is actually used. Pro members default to High; everyone else defaults to Medium and can use High
 * only while free tries are left.
 */
fun resolveQuality(selected: Quality?, state: ProState): Quality = when {
    state.pro -> selected ?: Quality.HIGH
    selected == Quality.HIGH && state.trialLeft > 0 -> Quality.HIGH
    else -> Quality.MEDIUM
}
