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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The two takes Studio Lab can play: the level-matched original and the cleaned voice. */
enum class Take { Original, Clean }

sealed interface LabState {
    data object Idle : LabState
    data object Recording : LabState
    data class Processing(val progress: Float) : LabState
    data object TooShort : LabState
    data object Failed : LabState
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
    private val archive: com.kaafisir.auramic.data.notes.NoteArchive,
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

    // ---- saved voice notes (from the floating mic) -------------------------------------------------------------
    val notes: StateFlow<com.kaafisir.auramic.data.notes.NoteArchive.State> = archive.state
    val savingEnabled: StateFlow<Boolean> = settingsRepo.settings.map { it.saveNotes }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    private val notePlayer = NotePlayer(viewModelScope)
    val nowPlaying: StateFlow<NowPlaying?> = notePlayer.now
    val playFailed: StateFlow<String?> = notePlayer.failed

    init { viewModelScope.launch { archive.cleanUpInterrupted(); archive.refresh() } }

    fun togglePlay(note: com.kaafisir.auramic.data.notes.SavedNote, take: Take) {
        stopPlayback()
        val file = (if (take == Take.Original) note.originalFile else note.enhancedFile) ?: return
        notePlayer.toggle(note.id, take, file)
    }

    fun seek(note: com.kaafisir.auramic.data.notes.SavedNote, take: Take, fraction: Float) {
        val file = (if (take == Take.Original) note.originalFile else note.enhancedFile) ?: return
        notePlayer.seek(note.id, take, file, fraction)
    }

    fun delete(note: com.kaafisir.auramic.data.notes.SavedNote) {
        if (notePlayer.now.value?.noteId == note.id) notePlayer.stop()
        viewModelScope.launch { archive.delete(note.id) }
    }

    fun deleteAll() { notePlayer.stop(); viewModelScope.launch { archive.deleteAll() } }

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
            try {
                val settings = settingsRepo.current()
                val r = process.enhance(rec, pro.resolve(settings), settings.cutPauses, stems = true) { p -> _state.value = LabState.Processing((p / 0.95f).coerceAtMost(1f)) }   // pipeline ends at 95 % (no encoding in Lab)
                val original = r.original ?: error("no original stem")
                _state.value = LabState.Result(
                    takes = mapOf(
                        Take.Original to Resampler.toShort(original, original.size),
                        Take.Clean to Resampler.toShort(r.voice, r.length),
                    ),
                    sampleRate = r.sampleRate, processingMs = r.processingMs,
                )
            } catch (t: kotlinx.coroutines.CancellationException) {
                throw t
            } catch (t: Throwable) {
                // Native / encoder failure or out of memory: show it instead of crashing or spinning forever.
                android.util.Log.e("LabViewModel", "processing failed", t)
                _state.value = LabState.Failed
            }
        }
    }

    fun play(take: Take) {
        val r = _state.value as? LabState.Result ?: return
        notePlayer.stop()
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
        recorder?.cancel(); player.stop(); notePlayer.stop()
        super.onCleared()
    }
}
