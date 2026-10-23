package com.kaafisir.auramic.ui.permissions

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.kaafisir.auramic.data.permissions.PermissionRepository
import com.kaafisir.auramic.domain.model.PermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

enum class Perm { Overlay, Microphone, Usage, Accessibility, Notifications, Battery }

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val repo: PermissionRepository,
) : ViewModel() {
    val state: StateFlow<PermissionState> = repo.state
    fun refresh() { repo.refresh() }

    /** Intent that opens the right Android settings page, or null for runtime permissions. */
    fun settingsIntent(p: Perm): Intent? = when (p) {
        Perm.Overlay -> repo.overlayIntent()
        Perm.Usage -> repo.usageAccessIntent()
        Perm.Accessibility -> repo.accessibilityIntent()
        Perm.Battery -> repo.batteryIntent()
        Perm.Microphone, Perm.Notifications -> null
    }

    fun appDetailsIntent(): Intent = repo.appDetailsIntent()
    fun batteryIntent(): Intent = repo.batteryIntent()
    fun keepRunningGuide() = repo.keepRunningGuide()
    fun openAutostart(): Boolean = repo.openAutostart()

    fun granted(p: Perm, s: PermissionState) = when (p) {
        Perm.Overlay -> s.overlay
        Perm.Microphone -> s.microphone
        Perm.Usage -> s.usageAccess
        Perm.Accessibility -> s.accessibility
        Perm.Notifications -> s.notifications
        Perm.Battery -> s.batteryExempt
    }
}
