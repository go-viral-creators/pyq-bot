package com.kaafisir.auramic.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import com.kaafisir.auramic.domain.model.OutputFormat
import java.io.File
import java.nio.ByteOrder

/** Encodes mono PCM16 into a compact, WhatsApp-friendly file using the hardware/software MediaCodec. */
object AudioEncoder {
    private const val TAG = "AudioEncoder"

    class Encoded(val file: File, val mimeType: String, val format: OutputFormat)

    /**
     * Encodes to [wanted]; if Opus is requested but unsupported (needs API 29+, 48 kHz and an Opus
     * encoder) or fails, transparently falls back to AAC in M4A.
     */
    fun encode(pcm: ShortArray, length: Int, sampleRate: Int, wanted: OutputFormat, outDir: File): Encoded =
        encode(length, sampleRate, wanted, outDir) { buf, pos, n -> buf.put(pcm, pos, n) }

    /** Same as above, straight from float samples (converted per codec buffer, no clip-sized PCM16 copy). */
    fun encode(pcm: FloatArray, length: Int, sampleRate: Int, wanted: OutputFormat, outDir: File): Encoded =
        encode(length, sampleRate, wanted, outDir) { buf, pos, n ->
            for (i in pos until pos + n) {
                val v = (pcm[i] * 32767f).let { if (it >= 0) it + 0.5f else it - 0.5f }   // same rounding as Resampler.toShort
                buf.put(v.coerceIn(-32768f, 32767f).toInt().toShort())
            }
        }

    private fun encode(length: Int, sampleRate: Int, wanted: OutputFormat, outDir: File, fill: Fill): Encoded {
        outDir.mkdirs()
        val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
        if (wanted == OutputFormat.OPUS && Build.VERSION.SDK_INT >= 29 && sampleRate == 48_000) {
            val f = File(outDir, "AuraMic_$stamp.opus")
            try {
                run(fill, length, sampleRate, MediaFormat.MIMETYPE_AUDIO_OPUS, 32_000, MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG, f)
                return Encoded(f, "audio/ogg", OutputFormat.OPUS)
            } catch (t: Throwable) {
                Log.w(TAG, "Opus encode failed, falling back to AAC", t)
                f.delete()
            }
        }
        val f = File(outDir, "AuraMic_$stamp.m4a")
        run(fill, length, sampleRate, MediaFormat.MIMETYPE_AUDIO_AAC, 64_000, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4, f)
        return Encoded(f, "audio/mp4", OutputFormat.M4A)
    }

    /** Writes samples [pos, pos + n) into the codec's input buffer. */
    private fun interface Fill { fun into(buf: java.nio.ShortBuffer, pos: Int, n: Int) }

    private fun run(fill: Fill, length: Int, sampleRate: Int, mime: String, bitrate: Int, muxerFormat: Int, out: File) {
        val format = MediaFormat.createAudioFormat(mime, sampleRate, 1).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024)
            if (mime == MediaFormat.MIMETYPE_AUDIO_AAC) {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            }
        }
        val codec = MediaCodec.createEncoderByType(mime)
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            muxer = MediaMuxer(out.absolutePath, muxerFormat)
            var track = -1
            val info = MediaCodec.BufferInfo()
            var pos = 0 // sample position
            var inputDone = false
            var outputDone = false
            var idleSpins = 0
            while (!outputDone) {
                if (!inputDone) {
                    val idx = codec.dequeueInputBuffer(10_000)
                    if (idx >= 0) {
                        val buf = codec.getInputBuffer(idx)!!
                        buf.clear()
                        val n = minOf(buf.capacity() / 2, length - pos)
                        if (n <= 0) {
                            codec.queueInputBuffer(idx, 0, 0, pos * 1_000_000L / sampleRate, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            fill.into(buf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer(), pos, n)
                            codec.queueInputBuffer(idx, 0, n * 2, pos * 1_000_000L / sampleRate, 0)
                            pos += n
                        }
                    }
                }
                val o = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        track = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    o >= 0 -> {
                        idleSpins = 0
                        val ob = codec.getOutputBuffer(o)!!
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                        if (info.size > 0 && muxerStarted) {
                            ob.position(info.offset)
                            ob.limit(info.offset + info.size)
                            muxer.writeSampleData(track, ob, info)
                        }
                        codec.releaseOutputBuffer(o, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                    o == MediaCodec.INFO_TRY_AGAIN_LATER -> if (inputDone && ++idleSpins > 500) error("encoder stalled")
                }
            }
            if (!muxerStarted) error("encoder produced no output")
        } finally {
            runCatching { codec.stop() }
            codec.release()
            if (muxer != null) {
                runCatching { if (muxerStarted) muxer.stop() }
                runCatching { muxer.release() }
            }
        }
    }
}
