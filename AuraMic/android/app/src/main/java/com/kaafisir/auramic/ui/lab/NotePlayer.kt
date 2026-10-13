package com.kaafisir.auramic.ui.lab

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/** Which saved take is loaded in the player and where it is. */
data class NowPlaying(val noteId: String, val take: Take, val positionMs: Long, val durationMs: Long, val playing: Boolean) {
    val fraction: Float get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

/**
 * Plays saved voice notes (m4a files) for Studio Lab with MediaPlayer, which streams from disk - a 5-minute note is
 * never decoded into memory. One take at a time; switching to the other take of the same note keeps the relative
 * position, so before / after can be compared at the same moment.
 */
class NotePlayer(private val scope: CoroutineScope) {
    private var player: MediaPlayer? = null
    private var ticker: Job? = null
    private val _now = MutableStateFlow<NowPlaying?>(null)
    val now: StateFlow<NowPlaying?> = _now.asStateFlow()
    /** "noteId/take" of the last take that could not be played. */
    private val _failed = MutableStateFlow<String?>(null)
    val failed: StateFlow<String?> = _failed.asStateFlow()

    fun toggle(noteId: String, take: Take, file: File) {
        val cur = _now.value
        val p = player
        if (cur != null && p != null && cur.noteId == noteId && cur.take == take) {
            if (cur.playing) { p.pause(); _now.value = cur.copy(playing = false, positionMs = p.currentPosition.toLong()); ticker?.cancel() }
            else { p.start(); _now.value = cur.copy(playing = true); tick() }
            return
        }
        // Other take of the same note while playing: continue from the same relative point.
        val startFraction = if (cur != null && cur.noteId == noteId && cur.playing) cur.fraction else 0f
        open(noteId, take, file, startFraction, play = true)
    }

    fun seek(noteId: String, take: Take, file: File, fraction: Float) {
        val cur = _now.value
        val p = player
        if (cur != null && p != null && cur.noteId == noteId && cur.take == take) {
            val ms = (fraction.coerceIn(0f, 1f) * cur.durationMs).toInt()
            p.seekTo(ms)
            _now.value = cur.copy(positionMs = ms.toLong())
        } else {
            open(noteId, take, file, fraction, play = false)
        }
    }

    private fun open(noteId: String, take: Take, file: File, fraction: Float, play: Boolean) {
        stop()
        _failed.value = null
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            mp.setDataSource(file.path)
            mp.prepare()                      // local file: quick, and simpler than prepareAsync for a few MB
            val dur = mp.duration.toLong().coerceAtLeast(0)
            if (fraction > 0f) mp.seekTo((fraction * dur).toInt())
            mp.setOnCompletionListener {
                ticker?.cancel()
                _now.value = _now.value?.copy(playing = false, positionMs = 0)
                runCatching { it.seekTo(0) }
            }
            mp.setOnErrorListener { _, what, extra ->
                Log.w(TAG, "playback error $what/$extra")
                _failed.value = "$noteId/${take.name}"
                stop(); true
            }
            player = mp
            _now.value = NowPlaying(noteId, take, (fraction * dur).toLong(), dur, play)
            if (play) { mp.start(); tick() }
        } catch (t: Throwable) {
            Log.w(TAG, "cannot play ${file.name}", t)
            runCatching { mp.release() }
            _failed.value = "$noteId/${take.name}"
            _now.value = null
        }
    }

    private fun tick() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                val p = player ?: break
                val cur = _now.value ?: break
                _now.value = cur.copy(positionMs = runCatching { p.currentPosition.toLong() }.getOrDefault(cur.positionMs))
                delay(80)
            }
        }
    }

    fun stop() {
        ticker?.cancel(); ticker = null
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        _now.value = null
    }

    private companion object { const val TAG = "NotePlayer" }
}
