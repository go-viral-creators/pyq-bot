# AuraMic AI — Google Play Accessibility API declaration (complete)

| | |
|---|---|
| App | AuraMic AI |
| Package | `com.kaafisir.auramic` |
| Service | `com.kaafisir.auramic.service.accessibility.AuraAccessibilityService` |
| Written for | build 1.0.2 (versionCode 3) and later |
| Public pages that must match this text | https://auramic.fireclashpro.com/accessibility.html and /privacy.html (both updated together with this file) |

> **Hinglish me, kaise use karein:** Play Console -> App content -> **Accessibility API declaration** (ya release ke saath aane wala "Permissions declaration form").
> Section **A** ke jawab form ke fields me copy-paste karo, section **F** ka video banao (YouTube par *Unlisted* upload karke link form me daalo),
> aur section **G** me reviewer ke sawal-jawab padh lo. Jab bhi accessibility ka kaam badle, is file ko, website ke `accessibility.html` ko aur in-app
> disclosure ko **ek saath** update karo (teeno ka matlab ek jaisa hona chahiye).

---

## A. Form answers (copy-paste)

**1. Does your app use the AccessibilityService API?**  Yes.

**2. Is your app an accessibility tool (designed to help people with disabilities)?**  No. `isAccessibilityTool` is not set in the service configuration.

**3. Core functionality that needs the API (the form's short field, 396 characters):**

```
AuraMic AI sends noise-free voice notes in WhatsApp. After the user records with our floating mic, the AccessibilityService selects the chat the user was in (in WhatsApp's share list) and taps Send, and it finds WhatsApp's own voice button so our mic can sit on it. Limited to WhatsApp and WhatsApp Business; message text is never read or stored; nothing leaves the device. Auto-send is optional.
```

**4. How exactly does the app use the API? (long field)**

```
AuraMic AI is a voice-note enhancer for WhatsApp and WhatsApp Business. The user holds our floating microphone, speaks and releases; the audio is cleaned on the phone and handed to WhatsApp through Android's share intent. WhatsApp then shows its "Send to..." list and requires a chat to be chosen and Send to be tapped. WhatsApp offers no public API to deliver a file into a chosen chat, so the AccessibilityService performs exactly those two on-screen steps for the user, only after the user has recorded a voice note.

The service is limited by its configuration to com.whatsapp and com.whatsapp.w4b. It is used for:
1. Finding where WhatsApp's own voice button is on the screen (position only) and whether a conversation is open, so our mic can sit exactly on top of it and hide when no chat is open.
2. Checking whether the message box is empty (not what is written), so our mic steps aside while the user types.
3. After the user releases the mic: reading the title of the open chat and the names shown in WhatsApp's chat list, typing that name into WhatsApp's own search box if the chat is not visible, tapping the matching chat and then Send. It gives up after about 5 seconds and asks the user to choose; it never guesses between two similar chats and never taps Send unless it selected the chat itself.
4. If a chat could not be found (for example the same number is saved under two names), noting which row the user then taps in WhatsApp's chat list and which name it shows, for one minute only, so the name can be remembered on the phone and the next note goes automatically. The user can delete the remembered names in Settings.

Message text is never read. Nothing read through the service is sent off the device; the app has no server. Every action follows a voice note the user has just recorded. The automatic sending can be switched off in Settings (the app then only opens WhatsApp's share screen and the user taps), and the service can be disabled at any time in Android settings.
```

**5. Does the app collect or share user data through the API?**  No data is collected or shared. Screen information read through the service is used on the device for the actions above and is not transmitted. The only thing kept is the optional list of "remembered chat names" (names the user's own pick taught the app), stored in the app's private storage on the phone, deletable in Settings and removed on uninstall.

**6. Is a prominent in-app disclosure shown, and is consent requested before the service is enabled?**  Yes. A dialog with an "I agree" / "Not now" choice is shown before Android's Accessibility settings screen is opened (text in section D).

**7. Could the same feature be done without the API?**  No (section C).

**8. Link to a video that shows the disclosure, the consent and the feature in use:**  `[YouTube unlisted link — see section F]`

---

## B. What the service reads and does (precise)

### B1. Configuration (`res/xml/accessibility_service_config.xml`)

| Setting | Value | Why |
|---|---|---|
| `packageNames` | `com.whatsapp`, `com.whatsapp.w4b` | Only these two apps are ever delivered to the service. Other apps are not inspected. |
| `accessibilityEventTypes` | `typeWindowStateChanged`, `typeWindowContentChanged`, `typeViewTextChanged`, `typeViewClicked` | State/content/text events only **wake** the tracker (screen changed, user typed); their content is not read. `typeViewClicked` is read only in the one minute after an automatic chat search failed (use 4 in section A). |
| `canRetrieveWindowContent` | true | Needed to find the chat row, the Send button, the voice button and the chat title. |
| `accessibilityFlags` | `flagReportViewIds`, `flagIncludeNotImportantViews`, `flagRetrieveInteractiveWindows` | Look up WhatsApp's views by resource id (fast, no tree walk), include views WhatsApp marks as "not important", and see WhatsApp's share screen when it is a separate window. |
| `canPerformGestures` | true | Fallback only: a short tap at a button's position when WhatsApp ignores a normal "click" action on it. |
| `notificationTimeout` | 100 ms | Coalesces bursts of events. |
| `isAccessibilityTool` | not set | The app is not an accessibility tool. |

### B2. Uses, in order of the user's journey

| # | When | What is read | What is done |
|---|---|---|---|
| U1 | WhatsApp is open | Whether a conversation is on screen; position and size of WhatsApp's voice (or Send) button | Nothing is clicked. Our floating mic is drawn at that position; it is hidden when no chat is open. |
| U2 | WhatsApp is open | Whether the message box is empty (a yes/no, not its text) | Our mic hides while the user types so WhatsApp's own Send button stays usable. |
| U3 | The user released the mic (a note is ready) | Title of the open chat (and its header lines); names, labels and buttons of WhatsApp's share list | Types the chat name into WhatsApp's search box if needed; taps the matching chat; taps Send; checks that the screen closed. About 5 s to find the chat, a few seconds for Send. Gives up with a toast; never guesses; never taps Send before it selected the chat; never on a conversation screen. |
| U4 | U3 only | — | Fallback tap through a gesture when a normal click is refused. |
| U5 | One minute after U3 could not find the chat | Name of the chat-list row the user taps, and whether they then tap Send | Stored on the phone after the same pick happened twice, so a chat listed under another name is found next time. Deletable in Settings. |
| U6 | Only if the user opens Settings -> "Floating mic not showing?" | A shortened structure dump of the last screen where auto-send failed (names cut to a few letters) | Shown on screen, copied only if the user taps Copy. |

### B3. What the service never does
* No keystroke logging, no reading of message text, no screenshots or screen recording.
* No inspection of any app other than WhatsApp and WhatsApp Business.
* No changing of device settings, no granting of permissions, no installing, no accessing passwords or payment screens.
* No upload of anything it sees; the app has no server and no analytics/advertising SDK.
* No autonomous decisions: every action follows a voice note the user has just recorded and released.

### B4. Data table

| Data | Purpose | Kept? | Leaves the phone? |
|---|---|---|---|
| Chat title and chat-list names | Find the same chat in the share list | In memory for the few seconds of the hand-off | No |
| Names of chats the user picked by hand (only after a failed search) | Find that chat automatically next time | Yes, app-private storage on the phone, only after the same pick twice; deletable in Settings; removed on uninstall | No |
| Position of WhatsApp's voice button; whether a chat is open; whether the message box is empty | Place / hide our mic | In memory | No |
| Shortened screen-structure dump after a failed send | Help the user report a problem | In memory; shown only when the user opens the troubleshooting screen | Only if the user copies and sends it |
| Message text, contacts, call logs, location, passwords | — | Not read | — |

---

## C. Why the API is necessary (alternatives considered)

| Alternative | Why it does not work |
|---|---|
| Android share intent alone | Opens WhatsApp's "Send to..." list; the user must still find the chat and tap Send. The feature (record and send in one gesture) is impossible without automating these two taps. |
| WhatsApp Business Platform / Cloud API | Needs a server, a business account and phone-number onboarding, and cannot send as the user from their own personal chat. |
| Direct-to-chat intent with a phone number (`jid`) | Needs the other person's number; AuraMic has no access to contacts (and must not request that permission for this feature). |
| Notification reply | Only text replies; no audio files. |
| Asking the user to tap manually | This is what the app does when Auto-send is off or when automatic selection fails, but it removes the core benefit. |

---

## D. Prominent disclosure (exact in-app text, English)

Shown by `PermissionsScreen` when the user taps **Allow** on the Accessibility card, **before** Android's Accessibility settings screen is opened. Buttons: **I agree** (opens the system settings) / **Not now** (does nothing). Available in 7 languages (English, Hindi, Hinglish, Spanish, Portuguese, Arabic, Indonesian).

> **Accessibility disclosure**
>
> AuraMic uses Android's Accessibility Service for one purpose only: after you record a voice note, it selects the chat you were in and taps the Send button inside WhatsApp or WhatsApp Business so the note is delivered without extra taps.
>
> It works only in WhatsApp and WhatsApp Business. It does not read, collect or share your messages, contacts or any personal data, and nothing leaves your phone. Audio is processed on your device.
>
> To put the mic on WhatsApp's own voice button and to hide it while you type, it only checks where that button is and whether the message box is empty. It never reads what you type.
>
> It also looks up where WhatsApp's own voice button is on screen, so the AuraMic microphone can sit exactly on top of it. It never reads message text.
>
> If AuraMic cannot find the chat by itself, it also notes which row you tap in WhatsApp's chat list and which name it shows, only for the next minute, and remembers that name on your phone so the next voice note to that chat goes automatically. You can forget these names in Settings.
>
> [ I agree ]   [ Not now ]

Android's own description of the service (shown on the system Accessibility screen): *"AuraMic taps the Send button in WhatsApp after you record a voice note with the floating mic. It works only inside WhatsApp and never reads or stores your messages. It also finds WhatsApp's voice button so the AuraMic mic can sit on it."*

Further public disclosure: https://auramic.fireclashpro.com/accessibility.html (linked from Settings -> "Accessibility disclosure") and the Privacy Policy.

---

## E. User control

* The service is **off** until the user enables it in Android Settings -> Accessibility, and can be disabled there at any time. Disabling it only disables auto-send; recording and noise removal keep working and the app opens WhatsApp's share screen for the user to finish.
* **Settings -> Auto-send** switches the automatic choosing-and-sending off.
* **Settings -> Remembered chat names** shows how many names are remembered and deletes them all.
* On Android 13+ for sideloaded installs the user may need *Allow restricted settings* (documented in the app and on the site). Not needed for Play installs.

---

## F. Video for the declaration (about 1.5–2 minutes)

Record the phone screen with the system recorder, voice-over or on-screen captions in English. Upload to YouTube as **Unlisted**; title: *"AuraMic AI — Accessibility API use (demo for Google Play)"*; put the link in section A, field 8. Use a test chat with a friend (or "Message yourself"), not a real private conversation.

| Time | Show | Say / caption |
|---|---|---|
| 0:00 | AuraMic home screen | "AuraMic AI cleans voice notes for WhatsApp." |
| 0:08 | Permissions screen -> tap **Allow** on *Accessibility* | "The app asks for the Accessibility Service." |
| 0:15 | The **disclosure dialog**; scroll through all of the text slowly | Read the key line aloud: "It selects the chat you were in and taps Send inside WhatsApp or WhatsApp Business. It never reads your messages." |
| 0:35 | Tap **I agree** -> Android Accessibility settings -> AuraMic -> switch on -> confirm | "Only after I agree, I enable the service myself." |
| 0:50 | Open a WhatsApp chat; the AuraMic mic sits on WhatsApp's voice button | "It finds WhatsApp's voice button so our mic sits on it." |
| 1:00 | Hold the mic, speak, release ("Cleaning…" appears) | "I record and release." |
| 1:10 | WhatsApp's "Send to…" list opens; the right chat is chosen and Send is tapped; the note appears in the chat | "The service selects the same chat and taps Send." |
| 1:25 | Type a few letters in the message box: our mic hides | "While I type, the mic steps aside; it only checks that the box is not empty." |
| 1:35 | Settings -> turn **Auto-send** off -> record again: the share list opens and nothing is tapped | "Auto-send is optional." |
| 1:50 | Settings -> **Remembered chat names** -> open the dialog | "Remembered names stay on the phone and can be deleted." |
| 1:58 | Android Settings -> Accessibility -> switch AuraMic off | "The user can disable it at any time." |

Checklist for the video: the disclosure text is fully readable and the **I agree** tap is visible; the system Accessibility settings screen is visible; the feature working end to end is visible; no real private messages are on screen.

---

## G. Questions a reviewer may ask, and the answers

**Q: Is this an accessibility tool?**  No. It is an automation helper for the user's own voice notes. We say so in the declaration and do not set `isAccessibilityTool`.

**Q: Does the service act on its own, without the user?**  No. It only acts after the user records and releases a voice note with the floating mic, for a few seconds, and stops (a toast tells the user to finish manually if it could not find the chat).

**Q: Does it read private data?**  It reads the title of the open chat and the chat names shown in WhatsApp's own share list to find the right row, and it checks whether the message box is empty. It does not read message text, and none of it leaves the device.

**Q: Why does it listen to click events now?**  Only for one minute after an automatic chat search failed, to note which row the user tapped and the name it shows, so that a chat WhatsApp lists under a different name (the same number saved twice) can be found next time. The name stays on the phone, only after the same pick twice, and the user can delete it in Settings. The tapped screen is not otherwise read.

**Q: Can it send a message to the wrong person?**  It taps Send only after it has itself selected a chat row that matches the open chat exactly (names compared after removing case, accents, emoji and spacing; phone numbers by digits). It never matches by prefix or by similarity, and it stops and asks the user when two chats look equally right or none does.

**Q: Is any data sent to a server?**  No. The app has no account system, no advertising or analytics SDK, and the voice processing is on the device. (Google Play Billing and Play app updates are handled by Google.)

**Q: Can the user turn it off?**  Yes: Android Settings -> Accessibility, and in the app Settings -> Auto-send.

---

## H. Before you submit (checklist)

- [ ] The AAB you submit contains the current disclosure (versionCode 3 / 1.0.2 or later) and the click-event setting.
- [ ] The website pages are re-uploaded: `website/accessibility.html` and `website/privacy.html` (regenerated with `python3 website/build.py`, now dated October 23, 2026).
- [ ] Data safety form: *Data collected: none*, *Data shared: none* (remembered chat names stay on the device and are not "collected" by Google's definition; re-check if the app ever gets network access).
- [ ] The declaration video is public to Google (Unlisted is fine) and matches section F.
- [ ] Section A text is pasted unchanged; if the feature changes later, update this file, the website and the in-app disclosure together.

## I. Where things are in the code (for your own reference)
* Service: `service/accessibility/AuraAccessibilityService.kt` (tracking, send automation, `learnFromClick`).
* Matching and flow: `automation/SendFlow.kt`, `NodeFinder.kt`, `ChatNameMatcher.kt`, `ChatAliasBook.kt`.
* Remembered names: `data/chats/ChatAliasStore.kt` (private SharedPreferences `auramic_chat_aliases`).
* Disclosure UI: `ui/permissions/PermissionsScreen.kt` (`disclosure_*` strings, generated by `tools/gen_strings.py`).
