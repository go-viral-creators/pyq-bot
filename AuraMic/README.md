# AuraMic AI — complete product ecosystem

Floating-mic voice notes for WhatsApp with **on-device AI noise removal** and **auto-send**.

```
AuraMic/
├── android/            Kotlin + Compose app (MVVM / clean layers, Hilt, Coroutines/Flow), NDK C++ DSP, TFLite model
├── play_store_assets/  ASO listing (7 locales), icon, feature graphic, 6 screenshots (+ generator scripts), Play Console guide
├── website/            Static Tailwind site: landing page, Privacy, Terms, Accessibility disclaimer, support@fireclashpro.com
├── tools/              DSP regression test, TFLite training script, string generator
└── apk/                Debug + release APKs and the release AAB
```

## Technical approach
### Audio
1. `AudioRecord` 48 kHz mono PCM16 → in-memory buffer (≤ 3 min).
2. **Native engine** (`android/app/src/main/cpp`, C++17, built with CMake/NDK for arm64-v8a, armeabi-v7a, x86_64; 16 KB page-aligned):
   sqrt-Hann STFT (1024/256) → noise PSD seeded from the quietest 20 % of the clip and tracked with MCRA speech-presence control →
   decision-directed a-priori SNR → Ephraim-Malah MMSE-LSA gain weighted by speech presence (OM-LSA) → time/frequency smoothing →
   overlap-add → loudness normalisation (-19 dBFS speech RMS target) + soft limiter.
3. **TFLite (LiteRT) refinement**: the DSP exports 64 features/frame; a small MLP (`assets/aura_mask.tflite`, 223 kB) predicts a 32-band speech-keep mask
   that is geometrically blended with the DSP gain. If the model is missing/fails, the DSP alone is used.
4. `MediaCodec` encodes AAC-LC `.m4a` (64 kbps) or Opus/Ogg (Android 10+, with automatic fallback).

### Accessibility automation
`ACTION_SEND` (FileProvider URI, `setPackage(whatsapp)`) opens WhatsApp's chat picker. The `AccessibilityService` (restricted to `com.whatsapp`/`.w4b`)
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
* 33 JVM unit/Robolectric tests: automation state machine & finder, gesture machine, 7-language resource completeness (placeholders, Hinglish script), Compose smoke tests incl. live language preview and tutorial flow.
* Native DSP regression (`tools/test_dsp.py`): +4…8 dB SNR on stationary noise, ~20 dB attenuation of noise-only passages; TFLite model validated end-to-end with the exported file.
* Android Lint: 0 errors. APK: v2-signed, zip- and ELF-aligned for 16 KB pages, R8 shrunk (≈17 MB).

## Known limits (please read)
* **Not run on a physical device/emulator** (no KVM in the build sandbox). Overlay windows, MediaCodec encoding, foreground-service start rules and,
  above all, **WhatsApp's real view hierarchy** were verified by code review and tests against fake trees only. Expect to tune `WhatsAppUi.kt` after a first device run.
* The shipped TFLite model is trained on **synthetic** speech/noise (see `tools/train_mask_model.py`); the DSP carries the quality. Swap in a model trained on real speech (same I/O contract) for better results.
* Shared audio arrives in WhatsApp as an audio *attachment*, not a native push-to-talk voice-note bubble (WhatsApp offers no API for that).
* Google Play may reject non-accessibility-tool apps that automate another app; see `play_store_assets/PLAY_CONSOLE_GUIDE.md`.
