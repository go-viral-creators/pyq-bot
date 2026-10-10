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

/** Denoising engine. AI = DeepFilterNet3 (ONNX Runtime); CLASSIC = built-in spectral DSP (fastest, lighter). */
enum class EnhanceEngine { AI, CLASSIC }

/** Auto-cut of silent parts: shortens long pauses and trims the start/end. */
enum class SilenceCut { OFF, GENTLE, TIGHT }

/** Mastering preset applied after denoising (EQ + de-esser + compressor). */
enum class VoicePreset { OFF, NATURAL, PODCAST }

/** Chat apps the floating mic works in. Package names cover the common variants of each app. */
enum class ChatApp(val key: String, val packages: Set<String>) {
    WHATSAPP("whatsapp", setOf("com.whatsapp")),
    WHATSAPP_BUSINESS("whatsapp_business", setOf("com.whatsapp.w4b"));

    companion object {
        val allPackages: Set<String> = entries.flatMap { it.packages }.toSet()
        fun forPackage(pkg: String?): ChatApp? = entries.firstOrNull { pkg != null && pkg in it.packages }
    }
}

/**
 * The only choice the user sees. MEDIUM is the fast, light cleaner (free). HIGH is the studio-grade cleaner
 * (Pro: subscription, or a few free tries).
 */
enum class Quality { MEDIUM, HIGH }

/** Everything the enhancement pipeline needs. Derived from [Quality]; there are no other user-facing knobs. */
data class EnhanceOptions(
    val engine: EnhanceEngine = EnhanceEngine.AI,
    /** Maximum noise attenuation in dB (>= 100 means unlimited). */
    val attenuationDb: Float = ATTENUATION_DB,
    /** 0..1 share of the removed background mixed back in. Always 0: the background is fully removed. */
    val keepBackground: Float = 0f,
    val fastMode: Boolean = false,
    val silenceCut: SilenceCut = SilenceCut.OFF,
    val preset: VoicePreset = VoicePreset.PODCAST,
    val normalize: Boolean = true,
    val targetLufs: Float = -16f,
) {
    companion object {
        /** Internal noise-removal strength: 97% of the range (the 95-99% band), i.e. very strong but not robotic. */
        const val STRENGTH = 0.97f
        const val ATTENUATION_DB = 6f + (STRENGTH - 0.2f) / 0.79f * 74f

        fun forQuality(q: Quality) = when (q) {
            Quality.HIGH -> EnhanceOptions(engine = EnhanceEngine.AI, preset = VoicePreset.PODCAST)
            Quality.MEDIUM -> EnhanceOptions(engine = EnhanceEngine.CLASSIC, preset = VoicePreset.NATURAL)
        }
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val language: AppLanguage? = null,
    val tourCompleted: Boolean = false,
    /** The user's intent: should the floating mic service be running? */
    val serviceEnabled: Boolean = false,
    /** The quality the user picked. Null = never chosen: Pro users get HIGH, everyone else MEDIUM. */
    val quality: Quality? = null,
    val autoSend: Boolean = true,
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
