package com.kaafisir.auramic.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.kaafisir.auramic.domain.model.AppLanguage
import com.kaafisir.auramic.service.overlay.OverlayState
import com.kaafisir.auramic.service.overlay.PillWindowContent
import com.kaafisir.auramic.R
import com.kaafisir.auramic.ui.components.AuraBubble
import com.kaafisir.auramic.ui.components.BubbleVisual
import com.kaafisir.auramic.ui.language.LanguageScreen
import com.kaafisir.auramic.ui.theme.AuraTheme
import com.kaafisir.auramic.ui.tour.TourScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ComposeSmokeTest {
    @get:Rule val rule = createComposeRule()

    @Test fun languageScreenPreviewsSelectedLanguageLive() {
        var chosen: AppLanguage? = null
        rule.setContent { AuraTheme(darkTheme = true) { LanguageScreen(AppLanguage.ENGLISH) { chosen = it } } }
        rule.onNodeWithText("Choose your language").assertIsDisplayed()
        rule.onNodeWithText("Español").performClick()
        rule.onNodeWithText("Elige tu idioma").assertIsDisplayed()          // title re-rendered in Spanish
        rule.onNodeWithText("Continuar").performClick()
        assertEquals(AppLanguage.SPANISH, chosen)
    }

    @Test fun hinglishCardPreviewsInLatinScript() {
        rule.setContent { AuraTheme(darkTheme = false) { LanguageScreen(AppLanguage.ENGLISH) {} } }
        rule.onNodeWithText("Hinglish").performClick()
        rule.onNodeWithText("Apni bhasha chunein").assertIsDisplayed()
    }

    @Test fun tourWalksThroughSteps() {
        var finished = false
        rule.setContent { AuraTheme(darkTheme = true) { TourScreen { finished = true } } }
        rule.onNodeWithText("Meet your floating mic").assertIsDisplayed()
        rule.onNodeWithText("Next").performClick()
        rule.onNodeWithText("Press and hold to record").assertIsDisplayed()
        rule.onNodeWithText("Skip").performClick()
        assertEquals(true, finished)
    }

    @Test fun bubbleRendersEveryVisualState() {
        var visual by mutableStateOf(BubbleVisual.Idle)
        rule.setContent { AuraTheme(darkTheme = true) { AuraBubble(visual, 0.5f, 64.dp) } }
        for (v in BubbleVisual.entries) {
            visual = v
            rule.waitForIdle()
        }
    }

    @Test fun statusPillShowsRecordingTimer() {
        rule.setContent { AuraTheme(darkTheme = true) { PillWindowContent(OverlayState.Recording(65_000, false), 0.4f, 300) } }
        rule.onNodeWithText("1:05").assertIsDisplayed()
    }

    @Test fun statusPillShowsResultBanner() {
        rule.setContent { AuraTheme(darkTheme = true) { PillWindowContent(OverlayState.Done(true, R.string.ov_sent), 0f, 300) } }
        rule.onNodeWithText("Sent!").assertIsDisplayed()
    }
}
