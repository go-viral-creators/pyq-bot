package com.kaafisir.auramic.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.kaafisir.auramic.data.notes.SavedNote
import com.kaafisir.auramic.ui.lab.NowPlaying
import com.kaafisir.auramic.ui.lab.SavedNoteCard
import com.kaafisir.auramic.ui.lab.Take
import com.kaafisir.auramic.ui.theme.AuraTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class StudioLabCardTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val peaks = FloatArray(120) { i -> (0.2f + 0.7f * kotlin.math.abs(kotlin.math.sin(i / 6f))) * if (i % 17 < 3) 0.1f else 1f }
    private fun note(originalSaved: Boolean = true) = SavedNote(
        id = "1760000000000", createdAt = 1_760_000_000_000,
        originalFile = if (originalSaved) File("o.m4a") else null, enhancedFile = File("e.m4a"),
        originalMs = 83_000, enhancedMs = 71_000,
        originalPeaks = peaks, enhancedPeaks = FloatArray(120) { peaks[it] * if (it % 17 < 3) 0.05f else 0.9f }, bytes = 1_200_000,
    )

    private fun shot(name: String) {
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(android.graphics.Canvas(bmp))
        val out = File(System.getProperty("auramic.shots") ?: "build/shots").also { it.mkdirs() }
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun cardShowsBothLabelledTakesAndPlaysTheChosenOne() {
        val clicked = mutableListOf<Take>()
        var deleted = false
        rule.setContent {
            AuraTheme(darkTheme = true) {
                Column(Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
                    SavedNoteCard(note(), NowPlaying("1760000000000", Take.Clean, 30_000, 71_000, playing = true), null,
                        onToggle = { clicked += it }, onSeek = { _, _ -> }, onDelete = { deleted = true })
                }
            }
        }
        rule.onAllNodesWithText("Original voice").fetchSemanticsNodes().let { assertEquals(1, it.size) }
        rule.onAllNodesWithText("Enhanced voice").fetchSemanticsNodes().let { assertEquals(1, it.size) }
        rule.onAllNodesWithText("0:30 / 1:11").fetchSemanticsNodes().let { assertEquals(1, it.size) }   // enhanced is playing
        rule.onAllNodesWithText("1:23").fetchSemanticsNodes().let { assertEquals(1, it.size) }          // original idle: its length
        shot("lab_card_playing_dark")
        rule.onNodeWithContentDescription("Play Original voice").performClick()
        rule.onNodeWithContentDescription("Pause Enhanced voice").performClick()
        rule.onNodeWithContentDescription("Delete").performClick()
        assertEquals(listOf(Take.Original, Take.Clean), clicked)
        assertEquals(true, deleted)
    }

    @Test fun missingOriginalIsShownAsNotSaved() {
        rule.setContent {
            AuraTheme(darkTheme = false) {
                Column(Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
                    SavedNoteCard(note(originalSaved = false), null, null, {}, { _, _ -> }, {})
                }
            }
        }
        rule.onAllNodesWithText("Not saved").fetchSemanticsNodes().let { assertEquals(1, it.size) }
        shot("lab_card_missing_light")
    }
}
