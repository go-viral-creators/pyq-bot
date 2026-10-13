package com.kaafisir.auramic.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/** Raw mono PCM16 capture. A fresh instance is used per recording. */
class PcmRecorder(
    private val preferredRate: Int = DEFAULT_RATE,
    private val maxSeconds: Int = MAX_SECONDS,
    /** Called once (on the recording thread) if [maxSeconds] is reached. */
    private val onLimit: () -> Unit = {},
) {
    class Recording(val pcm: ShortArray, val length: Int, val sampleRate: Int) {
        val seconds: Float get() = length / sampleRate.toFloat()
    }

    private val _level = MutableStateFlow(0f)
    /** Smoothed input level 0..1 for the waveform animation. */
    val level: StateFlow<Float> = _level.asStateFlow()

    @Volatile private var running = false
    private var record: AudioRecord? = null
    private var thread: Thread? = null
    private var buffer = ShortArray(preferredRate * 10)
    @Volatile private var length = 0
    private var rate = preferredRate

    val isRecording: Boolean get() = running

    /** Starts capturing. Returns false if the microphone could not be opened (busy / no permission). */
    @SuppressLint("MissingPermission") // checked by the caller before starting
    fun start(): Boolean {
        if (running) return true
        val ar = openRecord(preferredRate) ?: openRecord(44_100) ?: return false
        rate = ar.sampleRate
        record = ar
        length = 0
        running = true
        try {
            ar.startRecording()
            if (ar.recordingState != AudioRecord.RECORDSTATE_RECORDING) throw IllegalStateException("not recording")
        } catch (t: Throwable) {
            Log.w(TAG, "startRecording failed", t)
            running = false
            ar.release()
            record = null
            return false
        }
        thread = Thread({ readLoop(ar) }, "auramic-capture").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun openRecord(sampleRate: Int): AudioRecord? = try {
        val min = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) null else {
            val rec = AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, maxOf(min * 2, sampleRate / 5 * 2),
            )
            if (rec.state == AudioRecord.STATE_INITIALIZED) rec else { rec.release(); null }
        }
    } catch (t: Throwable) {
        Log.w(TAG, "AudioRecord($sampleRate) failed", t)
        null
    }

    private fun readLoop(ar: AudioRecord) {
        val chunk = ShortArray(rate / 50) // 20 ms
        val maxSamples = rate * maxSeconds
        var smooth = 0f
        while (running) {
            val n = ar.read(chunk, 0, chunk.size)
            if (n <= 0) {
                if (n < 0) break // error / device lost
                continue
            }
            if (length + n > buffer.size) buffer = buffer.copyOf(maxOf(buffer.size * 2, length + n))
            System.arraycopy(chunk, 0, buffer, length, n)
            length += n
            var e = 0.0
            for (i in 0 until n) e += (chunk[i] / 32768.0).let { it * it }
            val rms = sqrt(e / n).toFloat()
            // map -60..-6 dBFS onto 0..1, then smooth with fast attack / slow release
            val db = 20f * kotlin.math.log10(rms + 1e-6f)
            val target = ((db + 60f) / 54f).coerceIn(0f, 1f)
            smooth = if (target > smooth) 0.6f * target + 0.4f * smooth else 0.15f * target + 0.85f * smooth
            _level.value = smooth
            if (length >= maxSamples) {
                running = false
                onLimit()
            }
        }
    }

    /** Stops capture and returns the audio (null if nothing was recorded). */
    fun stop(): Recording? {
        running = false
        runCatching { thread?.join(500) }
        thread = null
        val ar = record
        record = null
        runCatching { ar?.stop() }
        ar?.release()
        _level.value = 0f
        return if (length > 0) Recording(buffer, length, rate) else null
    }

    fun cancel() {
        stop()
        length = 0
    }

    companion object {
        private const val TAG = "PcmRecorder"
        const val DEFAULT_RATE = 48_000
        /** Longest voice note (5 minutes); the recording stops and is sent when it is reached. */
        const val MAX_SECONDS = 300
    }
}
