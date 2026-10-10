package com.kaafisir.auramic.domain.model

/** UI languages. [tag] is a BCP-47 tag; Hinglish is Hindi written in Latin script (hi-Latn). */
enum class AppLanguage(
    val tag: String,
    val nativeName: String,
    val englishName: String,
    val badge: String,
) {
    ENGLISH("en", "English", "English", "EN"),
    HINDI("hi", "हिन्दी", "Hindi", "हि"),
    HINGLISH("hi-Latn", "Hinglish", "Hindi in English letters", "Hi·En"),
    SPANISH("es", "Español", "Spanish", "ES"),
    PORTUGUESE("pt", "Português", "Portuguese", "PT"),
    ARABIC("ar", "العربية", "Arabic", "ع"),
    INDONESIAN("id", "Bahasa Indonesia", "Indonesian", "ID");

    companion object {
        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }

        /** Best default for the device locale (used to pre-select a card on the first screen). */
        fun suggestedFor(languageCode: String): AppLanguage =
            entries.firstOrNull { it.tag.substringBefore('-') == languageCode && it != HINGLISH }
                ?: if (languageCode == "in") INDONESIAN else ENGLISH
    }
}

enum class OutputFormat { M4A, OPUS }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val language: AppLanguage? = null,
    val tourCompleted: Boolean = false,
    /** The user's intent: should the floating mic service be running? */
    val serviceEnabled: Boolean = false,
    val noiseStrength: Float = 0.7f,
    val loudnessBoost: Boolean = true,
    val outputFormat: OutputFormat = OutputFormat.M4A,
    val autoSend: Boolean = true,
    val includeBusiness: Boolean = true,
    val haptics: Boolean = true,
    val bubbleSizeDp: Int = 64,
    /** Bubble window position in px, or -1 for "default". */
    val bubbleX: Int = -1,
    val bubbleY: Int = -1,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val notesCleaned: Int = 0,
    val secondsPolished: Long = 0,
)

/** Snapshot of everything AuraMic needs from the OS. */
data class PermissionState(
    val overlay: Boolean = false,
    val microphone: Boolean = false,
    val usageAccess: Boolean = false,
    val accessibility: Boolean = false,
    val notifications: Boolean = false,
    val batteryExempt: Boolean = false,
) {
    val requiredGranted: Boolean get() = overlay && microphone && usageAccess && accessibility && notifications
    val requiredCount: Int get() = listOf(overlay, microphone, usageAccess, accessibility, notifications).count { it }
    companion object { const val REQUIRED_TOTAL = 5 }
}
