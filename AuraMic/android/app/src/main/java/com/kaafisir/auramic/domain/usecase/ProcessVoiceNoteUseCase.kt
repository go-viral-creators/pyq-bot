package com.kaafisir.auramic.domain.usecase

import android.content.Context
import com.kaafisir.auramic.audio.AudioEncoder
import com.kaafisir.auramic.audio.EnhanceResult
import com.kaafisir.auramic.audio.EnhancementPipeline
import com.kaafisir.auramic.audio.PcmRecorder
import com.kaafisir.auramic.automation.WhatsAppSender
import com.kaafisir.auramic.domain.model.EnhanceEngine
import com.kaafisir.auramic.domain.model.EnhanceOptions
import com.kaafisir.auramic.domain.model.OutputFormat
import com.kaafisir.auramic.domain.model.Quality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** A cleaned recording ready to share. */
class ProcessedNote(
    val file: File,
    val mimeType: String,
    val seconds: Float,
    val engine: EnhanceEngine,
    val processingMs: Long,
    val speedFactor: Float,
    val removedSilenceSeconds: Float,
    /** Waveform of the cleaned voice (for Studio Lab). */
    val peaks: FloatArray = FloatArray(0),
)

/** Recording -> AI denoise -> (cut silence) -> master -> encode. Runs entirely on-device. */
class ProcessVoiceNoteUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pipeline: EnhancementPipeline,
) {
    /** Enhancement only; Studio Lab keeps the signals for A/B playback (with stems). */
    suspend fun enhance(
        rec: PcmRecorder.Recording, quality: Quality, cutPauses: Boolean, stems: Boolean = false, onProgress: ((Float) -> Unit)? = null,
    ): EnhanceResult = withContext(Dispatchers.Default) {
        val job = coroutineContext[Job]
        // Progress is reported per network chunk (8 s of audio): also the point where a cancelled note stops early.
        val progress: (Float) -> Unit = { p ->
            if (job?.isActive == false) throw CancellationException("voice note cancelled")
            onProgress?.invoke(p)
        }
        pipeline.process(rec.pcm, rec.length, rec.sampleRate, EnhanceOptions.forQuality(quality, cutPauses), stems, progress)
    }

    suspend operator fun invoke(
        rec: PcmRecorder.Recording, quality: Quality, cutPauses: Boolean, onProgress: ((Float) -> Unit)? = null,
    ): ProcessedNote {
        val r = enhance(rec, quality, cutPauses, onProgress = onProgress)
        return withContext(Dispatchers.IO) {
            // Encoding is the last 5 %: 95 -> 100 %, so 100 % really means "done".
            val enc = AudioEncoder.encode(
                r.voice, r.length, r.sampleRate, OutputFormat.M4A,
                File(context.cacheDir, WhatsAppSender.SHARE_DIR),
            ) { f -> onProgress?.invoke(0.95f + 0.05f * f) }
            val peaks = com.kaafisir.auramic.data.notes.peaksOf(r.length) { r.voice[it] }
            ProcessedNote(enc.file, enc.mimeType, r.seconds, r.engineUsed, r.processingMs, r.speedFactor, r.removedSilenceSeconds, peaks)
        }
    }
}
