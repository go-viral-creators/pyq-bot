package com.kaafisir.auramic.ui

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.kaafisir.auramic.R
import com.kaafisir.auramic.domain.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/** Every language must resolve every string - including Hinglish (hi-Latn) - and keep format args. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalizationTest {
    private fun contextFor(lang: AppLanguage): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocale(Locale.forLanguageTag(lang.tag))
        return base.createConfigurationContext(cfg)
    }

    private val ids: List<Pair<String, Int>> = R.string::class.java.fields
        .filter { it.type == Int::class.javaPrimitiveType }
        .map { it.name to it.getInt(null) }
        .filter { (name, _) -> name != "app_name" && !name.startsWith("androidx_") && !name.startsWith("abc_") && !name.startsWith("m3c_") && !name.contains("_material") }
        .filter { (name, _) -> name.matches(Regex("^(common|lang|tour|perm|disclosure|home|lab|set|ov|cd|notif|a11y|app)_.*")) }

    @Test fun allStringsResolveInAllLanguages() {
        assertTrue("expected many strings, got ${ids.size}", ids.size > 120)
        for (lang in AppLanguage.entries) {
            val ctx = contextFor(lang)
            for ((name, id) in ids) {
                // build format args from the English template: %n$d -> Int, %n$f -> Double, %n$s -> String
                val spec = Regex("%(\\d)\\$[.\\d]*([sdf])").findAll(contextFor(AppLanguage.ENGLISH).resources.getString(id)).toList()
                val args = arrayOfNulls<Any>(spec.maxOfOrNull { it.groupValues[1].toInt() } ?: 0)
                spec.forEach { m -> args[m.groupValues[1].toInt() - 1] = when (m.groupValues[2]) { "d" -> 3; "f" -> 3.5; else -> "x" } }
                val raw = ctx.getString(id, *args).trim()
                assertTrue("${lang.tag}/$name empty", raw.isNotEmpty())
            }
        }
    }

    @Test fun formatPlaceholdersSurviveTranslation() {
        val en = contextFor(AppLanguage.ENGLISH)
        val formatted = ids.filter { (_, id) -> en.resources.getString(id).contains("%") }
        assertTrue(formatted.isNotEmpty())
        for (lang in AppLanguage.entries) {
            val ctx = contextFor(lang)
            for ((name, id) in formatted) {
                val raw = ctx.resources.getString(id)
                val expected = Regex("%\\d\\$[.\\d]*[sdf]").findAll(en.resources.getString(id)).map { it.value }.sorted().toList()
                val actual = Regex("%\\d\\$[.\\d]*[sdf]").findAll(raw).map { it.value }.sorted().toList()
                assertEquals("${lang.tag}/$name placeholders", expected, actual)
            }
        }
    }

    @Test fun languagesAreActuallyTranslated() {
        val en = contextFor(AppLanguage.ENGLISH).getString(R.string.lang_title)
        assertEquals("Choose your language", en)
        for (lang in AppLanguage.entries.filter { it != AppLanguage.ENGLISH }) {
            assertNotEquals("${lang.tag} falls back to English", en, contextFor(lang).getString(R.string.lang_title))
        }
    }

    @Test fun hinglishIsLatinScriptHindiAndDistinctFromDevanagari() {
        val hinglish = contextFor(AppLanguage.HINGLISH).getString(R.string.lang_title)
        val hindi = contextFor(AppLanguage.HINDI).getString(R.string.lang_title)
        assertEquals("Apni bhasha chunein", hinglish)
        assertEquals("अपनी भाषा चुनें", hindi)
    }
}
