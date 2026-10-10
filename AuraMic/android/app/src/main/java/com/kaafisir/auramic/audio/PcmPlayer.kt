package com.kaafisir.auramic.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Tiny PCM player used by Studio Lab for the before/after comparison. */
class PcmPlayer {
    private var job: Job? = null
    private var track: AudioTrack? = null

    fun play(scope: CoroutineScope, pcm: ShortArray, length: Int, sampleRate: Int, onFinished: () -> Unit) {
        stop()
        job = scope.launch(Dispatchers.IO) {
            val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val t = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, sampleRate / 5 * 2))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            track = t
            try {
                t.play()
                var pos = 0
                while (isActive && pos < length) {
                    val n = t.write(pcm, pos, minOf(4096, length - pos))
                    if (n <= 0) break
                    pos += n
                }
                // wait for the buffered tail to finish playing
                while (isActive && t.playbackHeadPosition < pos) delay(30)
            } finally {
                runCatching { t.stop() }
                t.release()
                if (track === t) track = null
                onFinished()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        track?.let { runCatching { it.pause(); it.flush() } }
    }
}
