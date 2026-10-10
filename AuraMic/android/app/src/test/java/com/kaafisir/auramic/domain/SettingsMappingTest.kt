package com.kaafisir.auramic.domain

import com.kaafisir.auramic.domain.model.ChatApp
import com.kaafisir.auramic.domain.model.EnhanceEngine
import com.kaafisir.auramic.domain.model.EnhanceOptions
import com.kaafisir.auramic.domain.model.Quality
import com.kaafisir.auramic.domain.model.SilenceCut
import com.kaafisir.auramic.domain.model.VoicePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsMappingTest {
    @Test fun strengthIsInTheNinetyFiveToNinetyNineBand() {
        assertTrue(EnhanceOptions.STRENGTH in 0.95f..0.99f)
    }

    @Test fun highQualityUsesDeepFilterNetAndMediumTheLightGtcrn() {
        assertEquals(EnhanceEngine.AI, EnhanceOptions.forQuality(Quality.HIGH).engine)
        assertEquals(EnhanceEngine.LIGHT, EnhanceOptions.forQuality(Quality.MEDIUM).engine)
    }

    @Test fun bothQualitiesUsePodcastMasteringAndCutPausesOverThreeHundredMs() {
        for (q in Quality.entries) {
            val o = EnhanceOptions.forQuality(q)
            assertEquals(VoicePreset.PODCAST, o.preset)
            assertEquals(SilenceCut.PAUSES, o.silenceCut)
        }
    }

    @Test fun backgroundIsFullyRemovedForBothQualities() {
        for (q in Quality.entries) {
            val o = EnhanceOptions.forQuality(q)
            assertEquals(0f, o.keepBackground, 0f)
            assertEquals(-16f, o.targetLufs, 0f)
            assertEquals(EnhanceOptions.ATTENUATION_DB, o.attenuationDb, 0f)
        }
    }

    @Test fun onlyWhatsAppIsSupported() {
        assertEquals(setOf(ChatApp.WHATSAPP, ChatApp.WHATSAPP_BUSINESS), ChatApp.entries.toSet())
        assertEquals(ChatApp.WHATSAPP_BUSINESS, ChatApp.forPackage("com.whatsapp.w4b"))
        assertNull(ChatApp.forPackage("org.telegram.messenger"))
        assertNull(ChatApp.forPackage("com.instagram.android"))
    }
}
