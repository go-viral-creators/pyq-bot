package com.kaafisir.auramic.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kaafisir.auramic.core.LocaleManager
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.kaafisir.auramic.domain.model.AppLanguage
import com.kaafisir.auramic.domain.model.ChatApp
import com.kaafisir.auramic.domain.model.EnhanceEngine
import com.kaafisir.auramic.domain.model.SilenceCut
import com.kaafisir.auramic.domain.model.VoicePreset
import com.kaafisir.auramic.domain.model.OutputFormat
import com.kaafisir.auramic.domain.model.ThemeMode
import com.kaafisir.auramic.domain.model.UserSettings
import com.kaafisir.auramic.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auramic_settings")

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private object K {
        val language = stringPreferencesKey("language")
        val tour = booleanPreferencesKey("tour_completed")
        val service = booleanPreferencesKey("service_enabled")
        val strength = floatPreferencesKey("noise_strength")
        val loudness = booleanPreferencesKey("loudness_boost")
        val format = stringPreferencesKey("output_format")
        val autoSend = booleanPreferencesKey("auto_send")
        val business = booleanPreferencesKey("include_business") // legacy, migrated into apps
        val apps = stringSetPreferencesKey("enabled_apps")
        val lufs = floatPreferencesKey("loudness_target")
        val engine = stringPreferencesKey("engine")
        val fast = booleanPreferencesKey("fast_mode")
        val keepBg = floatPreferencesKey("keep_background")
        val silence = stringPreferencesKey("silence_cut")
        val preset = stringPreferencesKey("voice_preset")
        val topRecent = booleanPreferencesKey("top_recent_fallback")
        val saveCopy = booleanPreferencesKey("save_copy")
        val haptics = booleanPreferencesKey("haptics")
        val bubbleSize = intPreferencesKey("bubble_size_dp")
        val bubbleX = intPreferencesKey("bubble_x")
        val bubbleY = intPreferencesKey("bubble_y")
        val theme = stringPreferencesKey("theme")
        val notes = intPreferencesKey("notes_cleaned")
        val seconds = longPreferencesKey("seconds_polished")
    }

    override val settings: Flow<UserSettings> = context.dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { p ->
            val d = UserSettings()
            UserSettings(
                language = AppLanguage.fromTag(p[K.language]),
                tourCompleted = p[K.tour] ?: d.tourCompleted,
                serviceEnabled = p[K.service] ?: d.serviceEnabled,
                noiseStrength = p[K.strength] ?: d.noiseStrength,
                loudnessBoost = p[K.loudness] ?: d.loudnessBoost,
                loudnessTarget = p[K.lufs] ?: d.loudnessTarget,
                engine = p[K.engine]?.let { runCatching { EnhanceEngine.valueOf(it) }.getOrNull() } ?: d.engine,
                fastMode = p[K.fast] ?: d.fastMode,
                keepBackground = p[K.keepBg] ?: d.keepBackground,
                silenceCut = p[K.silence]?.let { runCatching { SilenceCut.valueOf(it) }.getOrNull() } ?: d.silenceCut,
                voicePreset = p[K.preset]?.let { runCatching { VoicePreset.valueOf(it) }.getOrNull() } ?: d.voicePreset,
                topRecentFallback = p[K.topRecent] ?: d.topRecentFallback,
                saveCopy = p[K.saveCopy] ?: d.saveCopy,
                enabledApps = p[K.apps]?.mapNotNull { ChatApp.fromKey(it) }?.toSet()
                    ?: if (p[K.business] == false) ChatApp.defaults - ChatApp.WHATSAPP_BUSINESS else ChatApp.defaults,
                outputFormat = p[K.format]?.let { runCatching { OutputFormat.valueOf(it) }.getOrNull() } ?: d.outputFormat,
                autoSend = p[K.autoSend] ?: d.autoSend,
                haptics = p[K.haptics] ?: d.haptics,
                bubbleSizeDp = p[K.bubbleSize] ?: d.bubbleSizeDp,
                bubbleX = p[K.bubbleX] ?: d.bubbleX,
                bubbleY = p[K.bubbleY] ?: d.bubbleY,
                theme = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.theme,
                notesCleaned = p[K.notes] ?: d.notesCleaned,
                secondsPolished = p[K.seconds] ?: d.secondsPolished,
            )
        }

    override suspend fun current(): UserSettings = settings.first()

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        // Mirror synchronously for attachBaseContext(), which cannot wait for DataStore.
        LocaleManager.persist(context, language)
        edit { it[K.language] = language.tag }
    }

    override suspend fun setTourCompleted(done: Boolean) = edit { it[K.tour] = done }
    override suspend fun setServiceEnabled(enabled: Boolean) = edit { it[K.service] = enabled }
    override suspend fun setNoiseStrength(value: Float) = edit { it[K.strength] = value.coerceIn(0f, 1f) }
    override suspend fun setLoudnessBoost(enabled: Boolean) = edit { it[K.loudness] = enabled }
    override suspend fun setOutputFormat(format: OutputFormat) = edit { it[K.format] = format.name }
    override suspend fun setAutoSend(enabled: Boolean) = edit { it[K.autoSend] = enabled }
    override suspend fun setLoudnessTarget(lufs: Float) = edit { it[K.lufs] = lufs.coerceIn(-30f, -10f) }
    override suspend fun setEngine(engine: EnhanceEngine) = edit { it[K.engine] = engine.name }
    override suspend fun setFastMode(enabled: Boolean) = edit { it[K.fast] = enabled }
    override suspend fun setKeepBackground(value: Float) = edit { it[K.keepBg] = value.coerceIn(0f, 1f) }
    override suspend fun setSilenceCut(mode: SilenceCut) = edit { it[K.silence] = mode.name }
    override suspend fun setVoicePreset(preset: VoicePreset) = edit { it[K.preset] = preset.name }
    override suspend fun setTopRecentFallback(enabled: Boolean) = edit { it[K.topRecent] = enabled }
    override suspend fun setSaveCopy(enabled: Boolean) = edit { it[K.saveCopy] = enabled }
    override suspend fun setAppEnabled(app: ChatApp, enabled: Boolean) = edit {
        val cur = it[K.apps]?.toSet() ?: ChatApp.defaults.map { a -> a.key }.toSet()
        it[K.apps] = if (enabled) cur + app.key else cur - app.key
    }
    override suspend fun setHaptics(enabled: Boolean) = edit { it[K.haptics] = enabled }
    override suspend fun setBubbleSize(dp: Int) = edit { it[K.bubbleSize] = dp.coerceIn(48, 88) }
    override suspend fun setBubblePosition(x: Int, y: Int) = edit { it[K.bubbleX] = x; it[K.bubbleY] = y }
    override suspend fun setTheme(mode: ThemeMode) = edit { it[K.theme] = mode.name }
    override suspend fun recordCleanedNote(seconds: Long) = edit {
        it[K.notes] = (it[K.notes] ?: 0) + 1
        it[K.seconds] = (it[K.seconds] ?: 0L) + seconds
    }
}
