package com.kaafisir.auramic.domain.repository

import com.kaafisir.auramic.domain.model.AppLanguage
import com.kaafisir.auramic.domain.model.ChatApp
import com.kaafisir.auramic.domain.model.EnhanceEngine
import com.kaafisir.auramic.domain.model.SilenceCut
import com.kaafisir.auramic.domain.model.VoicePreset
import com.kaafisir.auramic.domain.model.OutputFormat
import com.kaafisir.auramic.domain.model.ThemeMode
import com.kaafisir.auramic.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun current(): UserSettings
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setTourCompleted(done: Boolean)
    suspend fun setServiceEnabled(enabled: Boolean)
    suspend fun setNoiseStrength(value: Float)
    suspend fun setLoudnessBoost(enabled: Boolean)
    suspend fun setOutputFormat(format: OutputFormat)
    suspend fun setAutoSend(enabled: Boolean)
    suspend fun setLoudnessTarget(lufs: Float)
    suspend fun setEngine(engine: EnhanceEngine)
    suspend fun setFastMode(enabled: Boolean)
    suspend fun setKeepBackground(value: Float)
    suspend fun setSilenceCut(mode: SilenceCut)
    suspend fun setVoicePreset(preset: VoicePreset)
    suspend fun setAppEnabled(app: ChatApp, enabled: Boolean)
    suspend fun setTopRecentFallback(enabled: Boolean)
    suspend fun setSaveCopy(enabled: Boolean)
    suspend fun setHaptics(enabled: Boolean)
    suspend fun setBubbleSize(dp: Int)
    suspend fun setBubblePosition(x: Int, y: Int)
    suspend fun setTheme(mode: ThemeMode)
    suspend fun recordCleanedNote(seconds: Long)
}
