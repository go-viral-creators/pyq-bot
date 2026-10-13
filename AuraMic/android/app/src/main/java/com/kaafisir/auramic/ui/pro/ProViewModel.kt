package com.kaafisir.auramic.ui.pro

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaafisir.auramic.data.pro.Plan
import com.kaafisir.auramic.data.pro.ProAccess
import com.kaafisir.auramic.data.pro.ProState
import com.kaafisir.auramic.data.pro.resolveQuality
import com.kaafisir.auramic.data.update.UpdateManager
import com.kaafisir.auramic.data.update.UpdateState
import com.kaafisir.auramic.domain.model.Quality
import com.kaafisir.auramic.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProUi(
    val pro: ProState = ProState(),
    /** The quality that is actually in use right now. */
    val quality: Quality = Quality.MEDIUM,
    val update: UpdateState = UpdateState.NONE,
)

/** Shared by Home, Settings and the Pro screen: quality choice, subscription, free tries and in-app updates. */
@HiltViewModel
class ProViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val access: ProAccess,
    private val updates: UpdateManager,
) : ViewModel() {

    val ui: StateFlow<ProUi> = combine(repo.settings, access.state, updates.state) { s, p, u ->
        ProUi(p, resolveQuality(s.quality, p), u)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProUi())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { access.refresh() }
        updates.check()
    }

    /** Returns false if the choice needs Pro (the caller then shows the Pro screen). */
    fun choose(q: Quality): Boolean {
        if (q == Quality.HIGH && !ui.value.pro.highAvailable) return false
        viewModelScope.launch { repo.setQuality(q) }
        return true
    }

    /** Called when a purchase just completed: Pro members start on High Quality. */
    fun onSubscribed() { viewModelScope.launch { repo.setQuality(Quality.HIGH) } }

    fun buy(activity: Activity, plan: Plan): Boolean = access.purchase(activity, plan)

    /** This phone's request code for phone-bound grants (shown in the redeem dialog only). */
    suspend fun requestCode(): String = access.requestCode()

    fun redeem(code: String, onResult: (com.kaafisir.auramic.data.pro.GrantCheck) -> Unit) {
        viewModelScope.launch {
            val r = runCatching { access.redeem(code) }.getOrDefault(com.kaafisir.auramic.data.pro.GrantCheck.Malformed)
            if (r is com.kaafisir.auramic.data.pro.GrantCheck.Valid) repo.setQuality(Quality.HIGH)
            onResult(r)
        }
    }

    fun removeGrant() { viewModelScope.launch { access.removeGrant() } }

    fun checkUpdate(onResult: (Boolean) -> Unit) = updates.check(onResult)
    fun checkAndUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>, onResult: (UpdateState) -> Unit) = updates.checkAndAct(launcher, onResult)
    fun startUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) = updates.start(launcher)
    fun installUpdate() = updates.install()
}
