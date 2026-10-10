package com.kaafisir.auramic.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.kaafisir.auramic.MainActivity
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Wipes app data (DataStore + prefs) so every test starts as a fresh install. */
private fun freshInstall() = object : ExternalResource() {
    override fun before() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ctx.dataDir.listFiles()?.forEach { if (it.name != "lib") it.deleteRecursively() }
    }
}

/** Launches the REAL MainActivity with the real Application + Hilt graph: catches start-up crashes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi", application = com.kaafisir.auramic.AuraMicApp::class)
class AppLaunchTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    val rule get() = compose
    @get:Rule val chain: RuleChain = RuleChain.outerRule(freshInstall()).around(compose)

    @Test fun firstLaunchShowsLanguageScreen() {
        rule.onNodeWithText("Choose your language").assertIsDisplayed()
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi", application = com.kaafisir.auramic.AuraMicApp::class)
class HomeNavigationTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    val rule get() = compose
    @get:Rule val chain: RuleChain = RuleChain.outerRule(freshInstall()).around(compose)

    @Test fun languageThenTourThenPermissionsThenHomeSettingsLab() {
        rule.onNodeWithText("Choose your language").assertIsDisplayed()
        rule.onNodeWithText("Español").performClick()
        rule.onNodeWithText("Continuar").performClick()
        waitFor("Conoce tu micrófono flotante")
        rule.onNodeWithText("Conoce tu micrófono flotante").assertIsDisplayed()    // tour, now in Spanish
        rule.onNodeWithText("Omitir").performClick()                                // skip the tour
        waitFor("Configuremos AuraMic")
        rule.onNodeWithText("Configuremos AuraMic").assertIsDisplayed()             // permissions
        rule.onNodeWithText("Omitir").performClick()                                // dismiss the permission showcase
        rule.onNodeWithText("Lo haré después").performClick()
        waitFor("Laboratorio de estudio")
        rule.onNodeWithText("Laboratorio de estudio").assertIsDisplayed()           // home
        rule.onNodeWithText("Laboratorio de estudio").performClick()
        waitFor("Mantén para grabar")
        rule.onNodeWithText("Mantén para grabar").assertIsDisplayed()               // lab

        // Back to Home, then switch through every language from Settings (the returning-user composition path)
        rule.onNodeWithContentDescription("Atrás").performClick()
        waitFor("Laboratorio de estudio")
        rule.onNodeWithContentDescription("Ajustes").performClick()
        // (native name, Settings title, label of the "Language" row in that language)
        val langs = listOf(
            Triple("English", "Settings", "Language"), Triple("हिन्दी", "सेटिंग्स", "भाषा"), Triple("Hinglish", "Settings", "Language"),
            Triple("Español", "Ajustes", "Idioma"), Triple("Português", "Configurações", "Idioma"),
            Triple("العربية", "الإعدادات", "اللغة"), Triple("Bahasa Indonesia", "Pengaturan", "Bahasa"),
        )
        var rowLabel = "Idioma"                                                       // currently Spanish
        for ((name, title, nextRow) in langs) {
            waitFor(rowLabel)
            rule.onAllNodesWithText(rowLabel).onFirst().performScrollTo().performClick()                // open the language dialog
            waitFor(name)
            rule.onAllNodesWithText(name).onLast().performClick()                     // pick a language
            waitFor(title)                                                            // Settings re-rendered in it
            rowLabel = nextRow
        }
    }

    private fun waitFor(text: String) {
        try {
            rule.waitUntil(10_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
            throw AssertionError("waiting for '$text'. Screen:\n" + rule.onRoot().printToString(), e)
        }
    }
}
