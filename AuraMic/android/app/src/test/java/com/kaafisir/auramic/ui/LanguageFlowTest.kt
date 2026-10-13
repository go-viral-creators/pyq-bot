package com.kaafisir.auramic.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.kaafisir.auramic.data.permissions.PermissionRepository
import com.kaafisir.auramic.domain.model.AppLanguage
import com.kaafisir.auramic.domain.model.Quality
import com.kaafisir.auramic.domain.model.ThemeMode
import com.kaafisir.auramic.domain.model.UserSettings
import com.kaafisir.auramic.domain.repository.SettingsRepository
import com.kaafisir.auramic.ui.navigation.AppNavHost
import com.kaafisir.auramic.ui.theme.AuraTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** In-memory settings so the navigation test can run without DataStore / Hilt. */
private class FakeSettings : SettingsRepository {
    val state = MutableStateFlow(UserSettings())
    override val settings: Flow<UserSettings> = state
    override suspend fun current() = state.first()
    override suspend fun setLanguage(language: AppLanguage) { state.value = state.value.copy(language = language) }
    override suspend fun setTourCompleted(done: Boolean) { state.value = state.value.copy(tourCompleted = done) }
    override suspend fun setServiceEnabled(enabled: Boolean) {}
    override suspend fun setQuality(quality: Quality) {}
    override suspend fun setAutoSend(enabled: Boolean) {}
    override suspend fun setCutPauses(enabled: Boolean) {}
    override suspend fun setHaptics(enabled: Boolean) {}
    override suspend fun setBubbleSize(dp: Int) {}
    override suspend fun setBubblePosition(x: Int, y: Int) {}
    override suspend fun setBubbleFree(free: Boolean) {}
    override suspend fun setSaveNotes(enabled: Boolean) {}
    override suspend fun setTheme(mode: ThemeMode) {}
    override suspend fun recordCleanedNote(seconds: Long) {}
}

/** Regression: after choosing a language and pressing Continue the app must move on to the tutorial. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class LanguageFlowTest {
    @get:Rule val rule = createComposeRule()

    @Test fun continueLeavesTheLanguageScreen() {
        val repo = FakeSettings()
        val vm = AppViewModel(repo, PermissionRepository(ApplicationProvider.getApplicationContext()))
        rule.setContent {
            val s by repo.state.collectAsState()
            AuraTheme(darkTheme = true) { AppNavHost(settings = s, vm = vm) }
        }
        rule.onNodeWithText("Choose your language").assertIsDisplayed()
        rule.onNodeWithText("Continue").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Meet your floating mic").assertIsDisplayed()   // tutorial step 1
    }

    @Test fun languageScreenIsNeverShownOnceLanguageIsSet() {
        val repo = FakeSettings().apply { state.value = UserSettings(language = AppLanguage.ENGLISH, tourCompleted = false) }
        val vm = AppViewModel(repo, PermissionRepository(ApplicationProvider.getApplicationContext()))
        rule.setContent {
            val s by repo.state.collectAsState()
            AuraTheme(darkTheme = true) { AppNavHost(settings = s, vm = vm) }
        }
        rule.onNodeWithText("Meet your floating mic").assertIsDisplayed()
    }
}
