# Google Play Console checklist — AuraMic AI

**Package:** `com.kaafisir.auramic` · **Category:** Tools (alt: Communication) · **Contact:** support@fireclashpro.com
**Privacy policy URL:** https://auramic.fireclashpro.com/privacy.html (publish `website/` there first)

## Store listing
| Field | File |
|---|---|
| Title / short / full description (7 locales) | `listing/<locale>/*.txt` (regenerate + validate: `python listing/build_listing.py`) |
| App icon 512×512 | `icon/app_icon_512.png` |
| Feature graphic 1024×500 | `feature_graphic/feature_graphic.png` |
| Phone screenshots (8) | `screenshots/phone_0*.png` (1080×1920) |

Locales: `en-US`, `en-IN` (Hinglish copy), `hi-IN`, `es-419`, `pt-BR`, `ar`, `id`.
Regenerate all graphics: `CHROMIUM_PATH=... python generate_assets.py`.

## Upload
Upload `app-release.aab` (built by `./gradlew :app:bundleRelease`). **Replace the sample keystore** (`android/auramic-sample.jks`)
with your own upload key: create `android/keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`) — never commit it.
Use Play App Signing. Language splits are disabled on purpose (the in-app language picker needs every translation on-device).

## Declarations (copy/paste starters)
**Accessibility API declaration** — *Core functionality:* "After the user records a voice note with our floating microphone, the service
selects the chat the user was in and taps the Send button inside WhatsApp so the note is delivered without extra taps. It is limited to
the supported chat apps (WhatsApp, WhatsApp Business, Telegram, Instagram; user-selectable), reads only chat/button labels needed to find them, never message content, and stores/transmits nothing.
The feature is optional (Auto-send can be turned off)." Add a short screen recording of: in-app disclosure → settings → record → auto-send.
The in-app prominent disclosure is already implemented (`disclosure_*` strings; shown before opening Accessibility settings).

**Foreground service (microphone):** records the user's voice note while they hold the floating button; started from the visible app UI / Quick Settings tile.
**PACKAGE_USAGE_STATS:** detects when WhatsApp is the foreground app so the overlay shows only there (declare as core functionality).
**SYSTEM_ALERT_WINDOW:** draws the floating mic.
**Data safety:** collects **no** data; no data shared; data processed on-device; no account. Audio is processed ephemerally on-device (not "collected").
**Content rating:** utility, no user-generated content hosted → Everyone. **Ads:** none. **Target audience:** 13+.

## Honest risk notes
* Google reviews Accessibility-API use closely. Automating another app's *Send* button is the core feature but may be questioned by reviewers;
  keep the declaration/video precise and the disclosure prominent. If rejected, distribute the signed APK from the website instead
  (Android 13+ users need “Allow restricted settings”, documented in-app and on the site).
* The "WhatsApp" name appears in the description to describe compatibility only, with a non-affiliation disclaimer; it is not in the title/icon.
* WhatsApp can change view IDs. Matching is isolated in `automation/WhatsAppUi.kt` (ids + multi-language labels) with text fallbacks, and covered by unit tests.
