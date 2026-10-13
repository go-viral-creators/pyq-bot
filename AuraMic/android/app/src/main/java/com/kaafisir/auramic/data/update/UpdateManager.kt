package com.kaafisir.auramic.data.update

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class UpdateState { NONE, AVAILABLE, DOWNLOADING, READY }

/** In-app updates through Google Play. Only does something for builds installed from Play; otherwise stays NONE. */
@Singleton
class UpdateManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val _state = MutableStateFlow(UpdateState.NONE)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var manager: AppUpdateManager? = null
    private var info: AppUpdateInfo? = null

    private val installListener = InstallStateUpdatedListener { s ->
        _state.value = when (s.installStatus()) {
            InstallStatus.DOWNLOADED -> UpdateState.READY
            InstallStatus.DOWNLOADING, InstallStatus.PENDING -> UpdateState.DOWNLOADING
            InstallStatus.FAILED, InstallStatus.CANCELED -> UpdateState.AVAILABLE
            else -> _state.value
        }
    }

    private fun mgr(): AppUpdateManager? = manager ?: runCatching {
        AppUpdateManagerFactory.create(context).also { it.registerListener(installListener); manager = it }
    }.getOrNull()

    /** Asks Google Play whether a newer version exists. [onResult] gets true if one is available. */
    fun check(onResult: ((Boolean) -> Unit)? = null) {
        val m = mgr() ?: run { onResult?.invoke(false); return }
        m.appUpdateInfo.addOnSuccessListener { i ->
            info = i
            _state.value = when {
                i.installStatus() == InstallStatus.DOWNLOADED -> UpdateState.READY
                i.installStatus() == InstallStatus.DOWNLOADING -> UpdateState.DOWNLOADING
                i.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && i.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> UpdateState.AVAILABLE
                else -> UpdateState.NONE
            }
            onResult?.invoke(_state.value != UpdateState.NONE)
        }.addOnFailureListener { onResult?.invoke(false) }
    }

    fun start(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val m = mgr() ?: return
        val i = info ?: return
        runCatching { m.startUpdateFlowForResult(i, launcher, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()) }
    }

    /** Installs a finished download and restarts the app. */
    fun install() { mgr()?.completeUpdate() }

    /**
     * "Check for update" in Settings: asks Play, then does the right thing for the state it finds - start the
     * download, or install one that already finished (previously a finished download was started again and never
     * installed). [onResult] gets the state found.
     */
    fun checkAndAct(launcher: ActivityResultLauncher<IntentSenderRequest>, onResult: (UpdateState) -> Unit) {
        check { _ ->
            when (_state.value) {
                UpdateState.AVAILABLE -> start(launcher)
                UpdateState.READY -> install()
                else -> Unit
            }
            onResult(_state.value)
        }
    }
}
