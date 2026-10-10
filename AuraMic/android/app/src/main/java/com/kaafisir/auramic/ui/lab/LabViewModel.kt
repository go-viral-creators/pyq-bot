package com.kaafisir.auramic.ui.lab

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaafisir.auramic.audio.NoiseSuppressionEngine
import com.kaafisir.auramic.audio.PcmPlayer
import com.kaafisir.auramic.audio.PcmRecorder
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
import kotlin.math.roundToInt

enum class Take { Original, Clean }

sealed interface LabState {
    data object Idle : LabState
    data object Recording : LabState
    data object Processing : LabState
    data object TooShort : LabState
    class Result(
        val original: ShortArray, val clean: ShortArray, val length: Int, val sampleRate: Int,
        val reductionDb: Int, val usedModel: Boolean,
    ) : LabState
}

/** Studio Lab: record in the app, then A/B the original against the AuraMic-processed take. */
@HiltViewModel
class LabViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepo: SettingsRepository,
    private val process: ProcessVoiceNoteUseCase,
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
        val rec = PcmRecorder(maxSeconds = 30)
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
        _state.value = LabState.Processing
        viewModelScope.launch {
            val settings = settingsRepo.current()
            val cleaned = process.denoise(rec, settings)
            // Level-match the original so the comparison is about noise, not loudness.
            val gain = (NoiseSuppressionEngine.rms(cleaned.pcm, rec.length) / (NoiseSuppressionEngine.rms(rec.pcm, rec.length) + 1e-9))
                .coerceIn(0.25, 8.0).toFloat()
            val matched = ShortArray(rec.length) { i -> (rec.pcm[i] * gain).roundToInt().coerceIn(-32768, 32767).toShort() }
            _state.value = LabState.Result(matched, cleaned.pcm, rec.length, rec.sampleRate, cleaned.backgroundReductionDb.roundToInt(), cleaned.usedModel)
        }
    }

    fun play(take: Take) {
        val r = _state.value as? LabState.Result ?: return
        if (_playing.value == take) { stopPlayback(); return }
        _playing.value = take
        player.play(viewModelScope, if (take == Take.Original) r.original else r.clean, r.length, r.sampleRate) {
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
