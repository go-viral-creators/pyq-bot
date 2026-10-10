package com.kaafisir.auramic.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaafisir.auramic.data.permissions.PermissionRepository
import com.kaafisir.auramic.domain.model.AppLanguage
import com.kaafisir.auramic.domain.model.PermissionState
import com.kaafisir.auramic.domain.model.ThemeMode
import com.kaafisir.auramic.domain.model.UserSettings
import com.kaafisir.auramic.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Activity-scoped state: settings (null until first load), permissions and top-level actions. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val permissionRepo: PermissionRepository,
) : ViewModel() {

    val settings: StateFlow<UserSettings?> = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val permissions: StateFlow<PermissionState> = permissionRepo.state

    fun refreshPermissions() { permissionRepo.refresh() }
    fun chooseLanguage(lang: AppLanguage, then: () -> Unit) {
        viewModelScope.launch { repo.setLanguage(lang); then() }
    }
    fun completeTour(done: Boolean = true) { viewModelScope.launch { repo.setTourCompleted(done) } }
    fun setTheme(mode: ThemeMode) { viewModelScope.launch { repo.setTheme(mode) } }
}
