package com.kaafisir.auramic.ui.lab

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaafisir.auramic.audio.PcmPlayer
import com.kaafisir.auramic.audio.PcmRecorder
import com.kaafisir.auramic.audio.Resampler
import com.kaafisir.auramic.domain.repository.SettingsRepository
import com.kaafisir.auramic.domain.usecase.ProcessVoiceNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The two takes Studio Lab can play: the level-matched original and the cleaned voice. */
enum class Take { Original, Clean }

sealed interface LabState {
    data object Idle : LabState
    data object Recording : LabState
    data class Processing(val progress: Float) : LabState
    data object TooShort : LabState
    class Result(
        val takes: Map<Take, ShortArray>,
        val sampleRate: Int,
        val processingMs: Long,
    ) : LabState
}

/** Studio Lab: record in the app, then compare your original voice with the cleaned one. */
@HiltViewModel
class LabViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepo: SettingsRepository,
    private val process: ProcessVoiceNoteUseCase,
    private val pro: com.kaafisir.auramic.data.pro.ProAccess,
) : ViewModel() {

    private val _state = MutableStateFlow<LabState>(LabState.Idle)
    val state: StateFlow<LabState> = _state.asStateFlow()
    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()
    private val _playing = MutableStateFlow<Take?>(null)
    val playing: StateFlow<Take?> = _playing.asStateFlow()

    private var recorder: PcmRecorder? = null
    private var levelJob: Job? = null
    private val player = PcmPlayer()

    fun hasMic() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun start(): Boolean {
        stopPlayback()
        if (!hasMic()) return false
        val rec = PcmRecorder(maxSeconds = 60)
        if (!rec.start()) return false
        recorder = rec
        _state.value = LabState.Recording
        levelJob = viewModelScope.launch { rec.level.collect { _level.value = it } }
        return true
    }

    fun stop() {
        val rec = recorder?.stop() ?: run { _state.value = LabState.Idle; return }
        recorder = null
        levelJob?.cancel(); _level.value = 0f
        if (rec.seconds < 0.8f) { _state.value = LabState.TooShort; return }
        _state.value = LabState.Processing(0f)
        viewModelScope.launch {
            val settings = settingsRepo.current()
            val r = process.enhance(rec, pro.resolve(settings), stems = true) { p -> _state.value = LabState.Processing(p) }
            _state.value = LabState.Result(
                takes = mapOf(
                    Take.Original to Resampler.toShort(r.original!!, r.original.size),
                    Take.Clean to Resampler.toShort(r.voice, r.length),
                ),
                sampleRate = r.sampleRate, processingMs = r.processingMs,
            )
        }
    }

    fun play(take: Take) {
        val r = _state.value as? LabState.Result ?: return
        if (_playing.value == take) { stopPlayback(); return }
        val pcm = r.takes[take] ?: return
        _playing.value = take
        player.play(viewModelScope, pcm, pcm.size, r.sampleRate) {
            if (_playing.value == take) _playing.value = null
        }
    }

    fun stopPlayback() { player.stop(); _playing.value = null }
    fun reset() { stopPlayback(); _state.value = LabState.Idle }

    override fun onCleared() {
        recorder?.cancel(); player.stop()
        super.onCleared()
    }
}
