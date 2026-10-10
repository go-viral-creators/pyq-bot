package com.kaafisir.auramic.domain

import com.kaafisir.auramic.domain.model.ChatApp
import com.kaafisir.auramic.domain.model.UserSettings
import com.kaafisir.auramic.domain.model.attenuationDb
import com.kaafisir.auramic.domain.model.toEnhanceOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsMappingTest {
    @Test fun strengthMapsToAttenuation() {
        assertEquals(100f, UserSettings(noiseStrength = 1.0f).attenuationDb(), 0f)
        assertEquals(6f, UserSettings(noiseStrength = 0.2f).attenuationDb(), 0.01f)
        assertTrue(UserSettings(noiseStrength = 0.6f).attenuationDb() in 30f..50f)
    }

    @Test fun defaultsAreStudioGrade() {
        val o = UserSettings().toEnhanceOptions()
        assertEquals(-16f, o.targetLufs, 0f); assertEquals(100f, o.attenuationDb, 0f); assertEquals(0f, o.keepBackground, 0f)
    }

    @Test fun chatAppsResolvePackages() {
        assertEquals(ChatApp.TELEGRAM, ChatApp.forPackage("org.telegram.messenger.web"))
        assertEquals(ChatApp.WHATSAPP_BUSINESS, ChatApp.forPackage("com.whatsapp.w4b"))
        assertNull(ChatApp.forPackage("com.example.other"))
        assertTrue(ChatApp.INSTAGRAM.experimental && ChatApp.INSTAGRAM !in ChatApp.defaults)
    }
}
