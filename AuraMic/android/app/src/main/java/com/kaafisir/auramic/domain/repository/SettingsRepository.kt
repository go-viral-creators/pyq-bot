package com.kaafisir.auramic.domain.repository

import com.kaafisir.auramic.domain.model.AppLanguage
import com.kaafisir.auramic.domain.model.Quality
import com.kaafisir.auramic.domain.model.ThemeMode
import com.kaafisir.auramic.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun current(): UserSettings
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setTourCompleted(done: Boolean)
    suspend fun setServiceEnabled(enabled: Boolean)
    suspend fun setQuality(quality: Quality)
    suspend fun setAutoSend(enabled: Boolean)
    suspend fun setCutPauses(enabled: Boolean)
    suspend fun setHaptics(enabled: Boolean)
    suspend fun setBubbleSize(dp: Int)
    suspend fun setBubblePosition(x: Int, y: Int)
    suspend fun setBubbleFree(free: Boolean)
    suspend fun setTheme(mode: ThemeMode)
    suspend fun recordCleanedNote(seconds: Long)
}
