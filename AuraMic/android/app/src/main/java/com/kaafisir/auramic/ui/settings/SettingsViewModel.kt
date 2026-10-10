package com.kaafisir.auramic.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaafisir.auramic.automation.AutomationBridge
import com.kaafisir.auramic.data.permissions.PermissionRepository
import com.kaafisir.auramic.domain.model.AppLanguage
import com.kaafisir.auramic.domain.model.ThemeMode
import com.kaafisir.auramic.domain.model.UserSettings
import com.kaafisir.auramic.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: SettingsRepository,
    private val permissions: PermissionRepository,
    private val bridge: AutomationBridge,
) : ViewModel() {
    val settings: StateFlow<UserSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    private fun launch(block: suspend () -> Unit) { viewModelScope.launch { block() } }

    fun setAutoSend(v: Boolean) = launch { repo.setAutoSend(v) }
    fun setHaptics(v: Boolean) = launch { repo.setHaptics(v) }
    fun setBubbleSize(v: Int) = launch { repo.setBubbleSize(v) }
    fun resetPosition() = launch { repo.setBubblePosition(-1, -1) }
    fun setTheme(v: ThemeMode) = launch { repo.setTheme(v) }
    fun setLanguage(lang: AppLanguage) = launch { repo.setLanguage(lang) }
    fun batteryIntent(): Intent = permissions.batteryIntent()

    /** Copies the last chat-screen structure captured by the automation to the clipboard. Returns false if there is none. */
    fun copyDiagnostics(): Boolean {
        val text = bridge.diagnostics ?: return false
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("AuraMic diagnostics", text))
        return true
    }
}
