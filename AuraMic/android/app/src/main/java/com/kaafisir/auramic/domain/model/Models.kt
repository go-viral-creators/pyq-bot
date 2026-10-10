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
enum class ChatApp(val key: String, val packages: Set<String>, val experimental: Boolean = false) {
    WHATSAPP("whatsapp", setOf("com.whatsapp")),
    WHATSAPP_BUSINESS("whatsapp_business", setOf("com.whatsapp.w4b")),
    TELEGRAM("telegram", setOf("org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram")),
    INSTAGRAM("instagram", setOf("com.instagram.android"), experimental = true);

    companion object {
        val allPackages: Set<String> = entries.flatMap { it.packages }.toSet()
        fun forPackage(pkg: String?): ChatApp? = entries.firstOrNull { pkg != null && pkg in it.packages }
        fun fromKey(key: String): ChatApp? = entries.firstOrNull { it.key == key }
        val defaults: Set<ChatApp> = setOf(WHATSAPP, WHATSAPP_BUSINESS, TELEGRAM)
    }
}

/** Everything the enhancement pipeline needs, derived from [UserSettings]. */
data class EnhanceOptions(
    val engine: EnhanceEngine = EnhanceEngine.AI,
    /** Maximum noise attenuation in dB (>= 100 means unlimited). */
    val attenuationDb: Float = 100f,
    /** 0..1 share of the removed background mixed back in ("keep ambience"). */
    val keepBackground: Float = 0f,
    val fastMode: Boolean = false,
    val silenceCut: SilenceCut = SilenceCut.OFF,
    val preset: VoicePreset = VoicePreset.PODCAST,
    val normalize: Boolean = true,
    val targetLufs: Float = -16f,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val language: AppLanguage? = null,
    val tourCompleted: Boolean = false,
    /** The user's intent: should the floating mic service be running? */
    val serviceEnabled: Boolean = false,
    /** 0.2 (gentle) .. 1.0 (maximum, unlimited attenuation). */
    val noiseStrength: Float = 1.0f,
    val loudnessBoost: Boolean = true,
    val loudnessTarget: Float = -16f,
    val engine: EnhanceEngine = EnhanceEngine.AI,
    val fastMode: Boolean = false,
    val keepBackground: Float = 0f,
    val silenceCut: SilenceCut = SilenceCut.OFF,
    val voicePreset: VoicePreset = VoicePreset.PODCAST,
    val enabledApps: Set<ChatApp> = ChatApp.defaults,
    /** If the open chat cannot be read, pick the most recent chat in the share list (off by default). */
    val topRecentFallback: Boolean = false,
    /** Also keep a copy of every processed note in Music/AuraMic. */
    val saveCopy: Boolean = false,
    val outputFormat: OutputFormat = OutputFormat.M4A,
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

/** Maps the 0.2..1.0 strength slider to a maximum attenuation: 0.2 -> 6 dB ... 0.99 -> 80 dB, 1.0 -> unlimited. */
fun UserSettings.attenuationDb(): Float =
    if (noiseStrength >= 0.99f) 100f else 6f + (noiseStrength.coerceIn(0.2f, 0.99f) - 0.2f) / 0.79f * 74f

fun UserSettings.toEnhanceOptions() = EnhanceOptions(
    engine = engine, attenuationDb = attenuationDb(), keepBackground = keepBackground, fastMode = fastMode,
    silenceCut = silenceCut, preset = voicePreset, normalize = loudnessBoost, targetLufs = loudnessTarget,
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
