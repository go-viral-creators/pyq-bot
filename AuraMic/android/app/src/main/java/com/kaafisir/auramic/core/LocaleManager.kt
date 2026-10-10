package com.kaafisir.auramic.core

import android.content.Context
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
