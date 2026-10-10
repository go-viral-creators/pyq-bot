# AuraMic AI — complete product ecosystem

Floating-mic voice notes for WhatsApp with **on-device AI noise removal** and **auto-send**.

```
AuraMic/
├── android/            Kotlin + Compose app (MVVM / clean layers, Hilt, Coroutines/Flow), NDK C++ + ONNX Runtime (DeepFilterNet3)
├── play_store_assets/  ASO listing (7 locales), icon, feature graphic, 6 screenshots (+ generator scripts), Play Console guide
├── website/            Static Tailwind site: landing page, Privacy, Terms, Accessibility disclaimer, support@fireclashpro.com
├── tools/              DSP + DeepFilterNet regression tests, numpy reference, string generator
├── android/hosttest/   desktop-JVM test of the real Kotlin audio pipeline
└── apk/                Debug + release APKs and the release AAB
```

## Technical approach
### Audio
1. `AudioRecord` 48 kHz mono PCM16 (44.1 kHz devices are resampled) -> in-memory buffer (<= 3 min).
2. **DeepFilterNet3** (full-band, ~2.1 M parameters, MIT/Apache-2.0) via **ONNX Runtime**: the C++ front/back end (`cpp/dfn.cpp`,
   a port of libDF: 960-pt STFT, ERB + complex features, ERB mask, 5-tap deep filtering, iSTFT) wraps the three ONNX models
   (`assets/dfn3/enc|erb_dec|df_dec.onnx`) which run in 8 s chunks with warm-up context (`DfnRuntime.kt`). ~25x real time on a laptop CPU.
   A built-in spectral DSP (`aura_dsp.cpp`) remains as the "Classic" engine / automatic fallback.
3. **Options:** noise strength (attenuation limit), keep-ambience mix, fast mode (skip deep filtering), **auto-cut silence**
   (`SilenceCutter.kt`), **voice polish** presets (high-pass, EQ, de-esser, compressor), **LUFS** target (BS.1770-4) and look-ahead limiter
   (`cpp/polish.cpp`), plus separated stems (voice / removed background) in Studio Lab.
4. `MediaCodec` encodes AAC-LC `.m4a` (64 kbps) or Opus/Ogg (Android 10+, with automatic fallback).

### Accessibility automation
`ACTION_SEND` (FileProvider URI, `setPackage(<foreground chat app>)`) opens the chat app's share screen. The `AccessibilityService` (restricted to `com.whatsapp`/`.w4b`)
runs `SendFlow`, a tested state machine: find the chat row (exact match, unambiguous prefix, scroll, then picker search) → tap it → tap **Send**
(resource-id, then multi-language labels; click → gesture-tap fallback; 3 retries; 25 s timeout). The chat name is read when you press the mic.
Safety rule: Send is only tapped after the flow itself selected the chat, and it never types into a conversation's message box.

### App features
Floating overlay shown only when WhatsApp is foreground (UsageStatsManager polling, paused when the screen is off), hold/slide-to-cancel/drag-to-snap gestures,
haptics, animated bubble + status pill, interactive spotlight tutorial (real gesture engine), guided permission screen with Play-compliant accessibility
disclosure and "restricted settings" help, Studio Lab (A/B playback), Quick Settings tile, battery-optimisation shortcut, 7 languages
(English, Hindi, Hinglish, Spanish, Portuguese, Arabic/RTL, Indonesian) switchable in-app, light/dark themes.

## Build
```bash
export ANDROID_HOME=/opt/android-sdk          # needs platform 36, build-tools 36, NDK 27.2.12479018, CMake 3.22.1, JDK 21
cd android
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:bundleRelease
```
`settings.gradle.kts` uses Google's Maven-Central mirror first (avoids HTTP 429 on repo.maven.apache.org).
Release signing uses `android/auramic-sample.jks` unless `keystore.properties` exists — **replace for production**.

## Verification performed
* 53 JVM unit/Robolectric tests (+ `hosttest/` running the real Kotlin + ONNX Runtime + JNI pipeline on a desktop JVM): automation state machine & finder, gesture machine, 7-language resource completeness (placeholders, Hinglish script), Compose smoke tests incl. live language preview and tutorial flow.
* Native DSP regression (`tools/test_dsp.py`) and **DeepFilterNet port** (`tools/test_dfn.py`): features match the official libDF to 1e-6, whole-clip output is bit-exact with the numpy reference,
  log-spectral distance to the clean reference on a real noisy recording drops from 10.2 dB to 2.8 dB; BS.1770 loudness exact.
* Host pipeline test (`./gradlew :hosttest:test`): 10.6 s of real noisy speech processed in ~0.4 s, noise floor 25 dB -> 100 dB below speech, output mastered to -16.0 LUFS / -1.5 dBFS peak.
* Android Lint: 0 errors. APK: v2-signed, zip- and ELF-aligned for 16 KB pages, R8 shrunk (≈17 MB).

## Known limits (please read)
* **Not run on a physical device/emulator** (no KVM in the build sandbox). Overlay windows, MediaCodec encoding, foreground-service start rules and,
  above all, **WhatsApp's real view hierarchy** were verified by code review and tests against fake trees only. Expect to tune `WhatsAppUi.kt` after a first device run.
* DeepFilterNet3 is a *denoiser* (keeps your real voice). Generative "enhance speech" tools (e.g. Adobe Podcast) can re-synthesise speech and sound even more polished on very poor recordings, but need the cloud; this is a deliberate on-device trade-off.
* Instagram DMs do not accept audio files from other apps: Instagram support is experimental and saves the note to Music/AuraMic for manual attaching. Telegram/Instagram screen matching is text-based and untested on real devices.
* Shared audio arrives in WhatsApp as an audio *attachment*, not a native push-to-talk voice-note bubble (WhatsApp offers no API for that).
* Google Play may reject non-accessibility-tool apps that automate another app; see `play_store_assets/PLAY_CONSOLE_GUIDE.md`.
