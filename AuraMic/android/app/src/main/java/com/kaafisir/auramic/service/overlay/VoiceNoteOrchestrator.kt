package com.kaafisir.auramic.service.overlay

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.kaafisir.auramic.R
import com.kaafisir.auramic.audio.PcmRecorder
import com.kaafisir.auramic.automation.AutomationBridge
import com.kaafisir.auramic.automation.SendResult
import com.kaafisir.auramic.automation.WhatsAppSender
import com.kaafisir.auramic.core.Haptics
import com.kaafisir.auramic.domain.repository.SettingsRepository
import com.kaafisir.auramic.domain.usecase.ProcessVoiceNoteUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The brain of the floating mic: press -> record -> (release) -> denoise -> encode -> share to
 * WhatsApp -> let the accessibility service tap Send. Exposes [state]/[level] for the UI.
 * All public functions must be called on the main thread; [attach] must be called first.
 */
class VoiceNoteOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepo: SettingsRepository,
    private val processNote: ProcessVoiceNoteUseCase,
    private val sender: WhatsAppSender,
    private val mediaSaver: com.kaafisir.auramic.automation.MediaSaver,
    private val bridge: AutomationBridge,
    private val haptics: Haptics,
    private val pro: com.kaafisir.auramic.data.pro.ProAccess,
    private val archive: com.kaafisir.auramic.data.notes.NoteArchive,
) {
    private lateinit var scope: CoroutineScope

    private val _state = MutableStateFlow<OverlayState>(OverlayState.Idle)
    val state: StateFlow<OverlayState> = _state.asStateFlow()
    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()
    /** 0..1 while a note is being cleaned (shown for long notes). */
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private var recorder: PcmRecorder? = null
    private var tickerJob: Job? = null
    private var levelJob: Job? = null
    private var pipelineJob: Job? = null
    private var transientJob: Job? = null
    private var startedAt = 0L
    private var cancelArmed = false
    private var chatName: String? = null
    private var targetPackage = "com.whatsapp"

    fun attach(scope: CoroutineScope) { this.scope = scope }

    fun release() {
        tickerJob?.cancel(); levelJob?.cancel(); pipelineJob?.cancel(); transientJob?.cancel()
        recorder?.cancel(); recorder = null
        _state.value = OverlayState.Idle
    }

    /** Starts recording for a chat in [targetPackage]. Returns false if it could not start. */
    fun press(targetPackage: String): Boolean {
        if (_state.value.isBusy) return false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            showTransient(OverlayState.Done(false, R.string.lab_mic_needed)); haptics.error(); return false
        }
        this.targetPackage = targetPackage
        // Remember which chat is open *now*; the share sheet will need it later.
        chatName = bridge.currentChatTitle()
        val rec = PcmRecorder(onLimit = { scope.launch { release(cancelled = false) } })
        if (!rec.start()) {
            showTransient(OverlayState.Done(false, R.string.ov_error_mic)); haptics.error(); return false
        }
        transientJob?.cancel()
        recorder = rec
        cancelArmed = false
        startedAt = SystemClock.elapsedRealtime()
        haptics.press()
        _state.value = OverlayState.Recording(0, false)
        levelJob = scope.launch { rec.level.collect { _level.value = it } }
        tickerJob = scope.launch {
            while (isActive) {
                _state.value = OverlayState.Recording(SystemClock.elapsedRealtime() - startedAt, cancelArmed)
                delay(100)
            }
        }
        return true
    }

    fun setCancelArmed(armed: Boolean) {
        if (_state.value !is OverlayState.Recording) return
        cancelArmed = armed
        haptics.tick()
        _state.value = OverlayState.Recording(SystemClock.elapsedRealtime() - startedAt, armed)
    }

    /** Finger lifted (or max length hit). [cancelled] discards the recording. */
    fun release(cancelled: Boolean) {
        if (_state.value !is OverlayState.Recording) return
        tickerJob?.cancel(); levelJob?.cancel()
        _level.value = 0f
        val rec = recorder?.stop()
        recorder = null
        if (cancelled || rec == null) {
            haptics.tick()
            _state.value = OverlayState.Idle
            return
        }
        if (rec.seconds < MIN_SECONDS) {
            haptics.error()
            showTransient(OverlayState.Done(false, R.string.ov_too_short))
            return
        }
        pipelineJob = scope.launch { runPipeline(rec) }
    }

    fun showHint(message: Int) {
        if (!_state.value.isBusy) showTransient(OverlayState.Hint(message), 1_600)
    }

    private suspend fun runPipeline(rec: PcmRecorder.Recording) {
        try {
            _state.value = OverlayState.Processing
            val settings = settingsRepo.current()
            val quality = pro.resolve(settings)
            _progress.value = 0f
            val note = processNote(rec, quality, settings.cutPauses) { p -> if (p > _progress.value) _progress.value = p }
            Log.i(TAG, "processed ${"%.1f".format(note.seconds)}s engine=${note.engine} ${"%.1f".format(note.speedFactor)}x realtime -> ${note.file.name}")

            _state.value = OverlayState.Sending
            // WhatsApp copies the file when the user (or we) tap Send; our temporary copy is then useless.
            scope.launch { delay(TEMP_FILE_TTL_MS); runCatching { note.file.delete() } }
            // Studio Lab keeps both versions (setting, on by default) - saved only after the hand-off below, so
            // encoding the original never delays sending. A failed save is logged and changes nothing else.
            val archiveJob = if (settings.saveNotes) {
                scope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
                    archive.save(rec, note.file, (note.seconds * 1000).toLong(), note.peaks)
                }
            } else null
            try {
                handOff(note, settings)
            } finally {
                archiveJob?.start()
                // Counted after the hand-off: it touches DataStore + a MediaStore file and must not delay sending.
                if (quality == com.kaafisir.auramic.domain.model.Quality.HIGH) pro.consumeFreeHighNote()   // no-op for Pro
            }
        } catch (t: kotlinx.coroutines.CancellationException) {
            throw t
        } catch (t: Throwable) {
            Log.e(TAG, "pipeline failed", t)
            haptics.error()
            showTransient(OverlayState.Done(false, R.string.ov_failed), 2_800)
        }
    }

    /** Shares the cleaned note to WhatsApp and lets the accessibility service send it. */
    private suspend fun handOff(note: com.kaafisir.auramic.domain.usecase.ProcessedNote, settings: com.kaafisir.auramic.domain.model.UserSettings) {
        if (!sender.share(note.file, note.mimeType, targetPackage)) {
            // The chat app cannot receive audio files (e.g. Instagram) or refused the intent: keep the note for manual use.
            val saved = mediaSaver.saveToMusic(note.file, note.mimeType)
            haptics.error()
            showTransient(OverlayState.Done(saved, if (saved) R.string.ov_saved else R.string.ov_failed), 3_200)
            if (saved) settingsRepo.recordCleanedNote(note.seconds.toLong())
            return
        }
        settingsRepo.recordCleanedNote(note.seconds.toLong())
        if (!settings.autoSend || !bridge.connected.value) {
            // Assisted mode: the share sheet is open, the user picks the chat and taps Send.
            showTransient(OverlayState.Done(true, R.string.ov_manual), 3_000)
            return
        }
        when (val r = bridge.sendAndAwait(chatName, targetPackage, allowFirstRow = false)) {
            SendResult.Sent -> { haptics.success(); showTransient(OverlayState.Done(true, R.string.ov_sent)) }
            // Nothing was sent. The chat list / preview is still open, so tell the user what to do (a toast stays readable
            // over WhatsApp) and get out of the way: no bubble on top of the list they now have to tap.
            is SendResult.Failed -> { haptics.error(); toastThenIdle(r.reason) }
            // Send was tapped but the screen changed before we could see the result: never say "not sent" (a resend would duplicate).
            is SendResult.Unknown -> { haptics.tick(); showTransient(OverlayState.Done(true, R.string.ov_unconfirmed), 2_600) }
        }
    }

    /** The automatic send gave up ([reason] is from SendFlow): ask the user to finish by hand, with a toast, and clear the bubble state. */
    private fun toastThenIdle(reason: String) {
        val message = when (reason) {
            "send button not found" -> R.string.ov_tap_send
            "chat blocked" -> R.string.ov_chat_blocked
            else -> R.string.ov_pick_chat
        }
        runCatching {
            android.widget.Toast.makeText(context, com.kaafisir.auramic.core.LocaleManager.wrap(context).getString(message), android.widget.Toast.LENGTH_LONG).show()
        }
        transientJob?.cancel()
        _state.value = OverlayState.Idle
    }

    /** Shows a banner state, then returns to Idle unless something else took over. */
    private fun showTransient(s: OverlayState, ms: Long = 1_700) {
        transientJob?.cancel()
        _state.value = s
        transientJob = scope.launch {
            delay(ms)
            if (_state.value === s) _state.value = OverlayState.Idle
        }
    }

    private companion object {
        const val TAG = "Orchestrator"
        const val MIN_SECONDS = 0.7f
        const val TEMP_FILE_TTL_MS = 10 * 60 * 1000L
    }
}
