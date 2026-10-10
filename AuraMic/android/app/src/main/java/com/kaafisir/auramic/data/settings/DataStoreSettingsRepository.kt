package com.kaafisir.auramic.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kaafisir.auramic.core.LocaleManager
import com.kaafisir.auramic.domain.model.AppLanguage
import com.kaafisir.auramic.domain.model.Quality
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
        val quality = stringPreferencesKey("quality")
        val autoSend = booleanPreferencesKey("auto_send")
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
                quality = p[K.quality]?.let { runCatching { Quality.valueOf(it) }.getOrNull() },
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
    override suspend fun setQuality(quality: Quality) = edit { it[K.quality] = quality.name }
    override suspend fun setAutoSend(enabled: Boolean) = edit { it[K.autoSend] = enabled }
    override suspend fun setHaptics(enabled: Boolean) = edit { it[K.haptics] = enabled }
    override suspend fun setBubbleSize(dp: Int) = edit { it[K.bubbleSize] = dp.coerceIn(48, 88) }
    override suspend fun setBubblePosition(x: Int, y: Int) = edit { it[K.bubbleX] = x; it[K.bubbleY] = y }
    override suspend fun setTheme(mode: ThemeMode) = edit { it[K.theme] = mode.name }
    override suspend fun recordCleanedNote(seconds: Long) = edit {
        it[K.notes] = (it[K.notes] ?: 0) + 1
        it[K.seconds] = (it[K.seconds] ?: 0L) + seconds
    }
}
