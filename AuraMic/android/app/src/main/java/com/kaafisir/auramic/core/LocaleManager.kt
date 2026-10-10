package com.kaafisir.auramic.core

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.content.res.Configuration
import com.kaafisir.auramic.domain.model.AppLanguage
import java.util.Locale

/**
 * In-app language switching that works on every API level without AppCompat's per-app locales:
 * the chosen tag is mirrored into SharedPreferences (readable synchronously) and applied by
 * wrapping the base context of every Activity/Service that shows text.
 */
object LocaleManager {
    private const val PREFS = "auramic_locale"
    private const val KEY = "tag"

    fun persist(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, language.tag).apply()
    }

    fun saved(context: Context): AppLanguage? =
        AppLanguage.fromTag(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null))

    /** [base] with resources forced to [lang]; used for instant in-app language switching (no Activity recreation). */
    fun localized(base: Context, lang: AppLanguage): Context {
        val locale = Locale.forLanguageTag(lang.tag)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        val res = base.createConfigurationContext(config).resources
        // A ContextWrapper (not the bare configuration context!) so the Activity stays reachable through baseContext:
        // Hilt's hiltViewModel(), Navigation and startActivity() all walk that chain and crash on a plain ContextImpl.
        return object : ContextWrapper(base) {
            override fun getResources(): Resources = res
        }
    }

    /** Returns a context whose resources resolve in the user's chosen language (or [base] if unset). */
    fun wrap(base: Context): Context {
        val lang = saved(base) ?: return base
        val locale = Locale.forLanguageTag(lang.tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
