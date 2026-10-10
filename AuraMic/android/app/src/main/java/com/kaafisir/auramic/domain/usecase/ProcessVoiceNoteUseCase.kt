package com.kaafisir.auramic.domain.usecase

import android.content.Context
import com.kaafisir.auramic.audio.AudioEncoder
import com.kaafisir.auramic.audio.DenoiseResult
import com.kaafisir.auramic.audio.NoiseSuppressionEngine
import com.kaafisir.auramic.audio.PcmRecorder
import com.kaafisir.auramic.automation.WhatsAppSender
import com.kaafisir.auramic.domain.model.UserSettings
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
    val usedModel: Boolean,
    val reductionDb: Float,
)

/** Recording -> AI denoise -> loudness -> encode. Runs entirely on-device. */
class ProcessVoiceNoteUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: NoiseSuppressionEngine,
) {
    /** Denoise only (Studio Lab keeps the PCM for A/B playback). */
    suspend fun denoise(rec: PcmRecorder.Recording, settings: UserSettings): DenoiseResult =
        withContext(Dispatchers.Default) {
            engine.process(rec.pcm, rec.length, rec.sampleRate, settings.noiseStrength, settings.loudnessBoost)
        }

    suspend operator fun invoke(rec: PcmRecorder.Recording, settings: UserSettings): ProcessedNote {
        val cleaned = denoise(rec, settings)
        return withContext(Dispatchers.IO) {
            val enc = AudioEncoder.encode(
                cleaned.pcm, rec.length, rec.sampleRate, settings.outputFormat,
                File(context.cacheDir, WhatsAppSender.SHARE_DIR),
            )
            ProcessedNote(enc.file, enc.mimeType, rec.seconds, cleaned.usedModel, cleaned.backgroundReductionDb)
        }
    }
}
