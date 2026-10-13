package com.kaafisir.auramic.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaafisir.auramic.data.permissions.PermissionRepository
import com.kaafisir.auramic.domain.model.PermissionState
import com.kaafisir.auramic.domain.model.UserSettings
import com.kaafisir.auramic.domain.repository.SettingsRepository
import com.kaafisir.auramic.service.overlay.OverlayService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUi(
    val settings: UserSettings = UserSettings(),
    val permissions: PermissionState = PermissionState(),
    val running: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: SettingsRepository,
    private val permissionRepo: PermissionRepository,
    private val restarter: com.kaafisir.auramic.service.overlay.OverlayRestarter,
) : ViewModel() {

    val ui: StateFlow<HomeUi> = combine(repo.settings, permissionRepo.state, OverlayService.isRunning) { s, p, r ->
        HomeUi(s, p, r)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUi())

    fun refresh() { permissionRepo.refresh() }

    /** Turns the floating mic on/off, or sends the user to setup if permissions are missing. */
    fun toggle(onNeedSetup: () -> Unit) {
        viewModelScope.launch {
            when {
                // Live state, not [ui] (which starts from defaults until DataStore has answered).
                OverlayService.isRunning.value -> { OverlayService.stop(context); repo.setServiceEnabled(false) }
                !permissionRepo.refresh().requiredGranted -> onNeedSetup()
                else -> if (OverlayService.start(context)) repo.setServiceEnabled(true)
            }
        }
    }

    /**
     * If the user wants it on but the process was killed (update, OEM task killer, low memory), bring it back.
     * Reads the stored setting directly: [ui] still holds its defaults right after a process restart.
     */
    fun ensureRunning() {
        viewModelScope.launch { restarter.restoreIfWanted(inForeground = true) }
    }
}
