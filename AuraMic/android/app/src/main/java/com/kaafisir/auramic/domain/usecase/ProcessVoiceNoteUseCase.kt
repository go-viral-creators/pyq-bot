package com.kaafisir.auramic.domain.usecase

import android.content.Context
import com.kaafisir.auramic.audio.AudioEncoder
import com.kaafisir.auramic.audio.EnhanceResult
import com.kaafisir.auramic.audio.EnhancementPipeline
import com.kaafisir.auramic.audio.PcmRecorder
import com.kaafisir.auramic.audio.Resampler
import com.kaafisir.auramic.automation.WhatsAppSender
import com.kaafisir.auramic.domain.model.EnhanceEngine
import com.kaafisir.auramic.domain.model.EnhanceOptions
import com.kaafisir.auramic.domain.model.OutputFormat
import com.kaafisir.auramic.domain.model.Quality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
        pipeline.process(rec.pcm, rec.length, rec.sampleRate, EnhanceOptions.forQuality(quality, cutPauses), stems, onProgress)
    }

    suspend operator fun invoke(rec: PcmRecorder.Recording, quality: Quality, cutPauses: Boolean): ProcessedNote {
        val r = enhance(rec, quality, cutPauses)
        return withContext(Dispatchers.IO) {
            val enc = AudioEncoder.encode(
                Resampler.toShort(r.voice, r.length), r.length, r.sampleRate, OutputFormat.M4A,
                File(context.cacheDir, WhatsAppSender.SHARE_DIR),
            )
            ProcessedNote(enc.file, enc.mimeType, r.seconds, r.engineUsed, r.processingMs, r.speedFactor, r.removedSilenceSeconds)
        }
    }
}
