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
Upload `AuraMic-1.0.0-release.aab` (versionCode **1**, versionName 1.0.0; built by `./gradlew :app:bundleRelease`). It is signed with the
**AuraMic upload key** from the `3_Keystore` folder (details in `KEYSTORE_INFO.txt`). To rebuild, copy `keystore.properties.example` to
`android/keystore.properties`, fill in the values and put the `.jks` file next to it - never commit either. Enrol in **Play App Signing** when
Play Console asks (Google keeps the app-signing key; this key is only your *upload* key, so a lost upload key can be reset through Play support).
Each new upload needs a higher versionCode (next: 3).
Language splits are disabled on purpose (the in-app language picker needs every translation on-device).

## Declarations (copy/paste starters)
**Accessibility API declaration** — *Core functionality:* "After the user records a voice note with our floating microphone, the service
selects the chat the user was in and taps the Send button inside WhatsApp so the note is delivered without extra taps. It is limited to
WhatsApp and WhatsApp Business only, reads only chat/button labels needed to find them, never message content, and stores/transmits nothing.
The feature is optional (Auto-send can be turned off)." Add a short screen recording of: in-app disclosure → settings → record → auto-send.
The in-app prominent disclosure is already implemented (`disclosure_*` strings; shown before opening Accessibility settings).

**Foreground service (microphone):** records the user's voice note while they hold the floating button; started from the visible app UI / Quick Settings tile.
**PACKAGE_USAGE_STATS:** detects when WhatsApp is the foreground app so the overlay shows only there (declare as core functionality).
**SYSTEM_ALERT_WINDOW:** draws the floating mic.
**Data safety:** collects **no** data; no data shared; data processed on-device; no account. Audio is processed on-device; the optional saved original/enhanced notes stay in app-private storage on the phone and are never transmitted, which Google does not count as "collected" (re-check this answer if the app ever gets network access).
**Content rating:** utility, no user-generated content hosted → Everyone. **Ads:** none. **Target audience:** 13+.

## Honest risk notes
* Google reviews Accessibility-API use closely. Automating another app's *Send* button is the core feature but may be questioned by reviewers;
  keep the declaration/video precise and the disclosure prominent. If rejected, distribute the signed APK from the website instead
  (Android 13+ users need “Allow restricted settings”, documented in-app and on the site).
* The "WhatsApp" name appears in the description to describe compatibility only, with a non-affiliation disclaimer; it is not in the title/icon.
* WhatsApp can change view IDs. Matching is isolated in `automation/WhatsAppUi.kt` (ids + multi-language labels) with text fallbacks, and covered by unit tests.


---

## Policy checklist (v1.4) - what Play review looks at and how AuraMic answers it

| Area | What to declare / do |
|---|---|
| **Accessibility API declaration** | Core purpose: tap *Send* in WhatsApp / WhatsApp Business after the user's own voice note, and find where WhatsApp's voice button is so the AuraMic mic can sit on it. Not an accessibility tool (`isAccessibilityTool` is not set). Scope limited in `accessibility_service_config.xml` to `com.whatsapp` and `com.whatsapp.w4b`. Prominent in-app disclosure (accept / decline) is shown before the system settings screen opens. Every action is user-initiated (the user records, the user releases); *Auto-send* can be switched off (the app then opens the share sheet and the user taps). Message text is never read. Upload the demo video: enable service -> hold mic -> release -> sent. |
| **Foreground service (microphone)** | Declare `FOREGROUND_SERVICE_MICROPHONE` with the video: the notification is visible, the mic is only used while the user holds the button. |
| **`SYSTEM_ALERT_WINDOW`** | Core functionality: the AuraMic mic is drawn over WhatsApp's own voice button. It is AuraMic-branded (does not imitate WhatsApp's UI), appears only inside a WhatsApp chat, and hides when text is typed. |
| **`PACKAGE_USAGE_STATS`** | Used only to know whether WhatsApp is in front (evaluated on the device, never stored or sent). |
| **Data safety** | Collected: none. Shared: none. All audio is processed on the device; the cache copy is deleted after sending and the optional saved notes (original + enhanced) stay in app-private storage on the phone. (Google Play Billing / Play update APIs are handled by Google; declare "No data collected" and "Data is encrypted in transit".) |
| **Subscriptions** | One subscription `auramic_pro` with base plans `monthly` and `yearly`. The app shows the price from Play, says it renews until cancelled, offers *Manage subscription* and *Restore*. No external payment links. |
| **In-app updates** | `com.google.android.play:app-update` (flexible). Only active for Play installs. |
| **Backup** | Only the tiny Pro / free-try file is included in backup (`backup_rules.xml`, `data_extraction_rules.xml`). |
| **Listing** | Generated by `listing/build_listing.py`: WhatsApp / WhatsApp Business only, no unsupported claims, no comparison with other products, independence disclaimer in every language. |
| **Target API / 64-bit / 16 KB** | targetSdk 36, arm64-v8a + armeabi-v7a, all native libraries aligned to 16 KB pages. |
| **Android ID allow-list** | `ProAllowlist` gives the developer's own devices free Pro. The ID is app-signing-key specific: install the **Play-signed** build, open Settings -> About -> *Device ID*, and add what it shows. |

## Screenshot / trademark note
The screenshots show an **original** WhatsApp-style dark chat drawn for AuraMic - no WhatsApp logo, artwork or real screens - plus a generic
green chat-bubble tag ("AuraMic for WhatsApp"). Naming WhatsApp to say what the app works with is fine; do not use their logo, and keep the
non-affiliation line in the listing: "WhatsApp is a trademark of its owner. AuraMic is an independent app and is not affiliated with or endorsed by WhatsApp."
