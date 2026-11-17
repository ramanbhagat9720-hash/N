# AI Call Assistant — Setup, Architecture & Compatibility Guide

## 1. Overview
**AI Call Assistant** is an Android (Kotlin + Jetpack Compose + Material 3) personal AI phone assistant. When an incoming call arrives, the assistant identifies the caller, speaks to the user first before answering or rejecting, and—only with explicit user authorization—answers on the user's behalf in the caller's language while keeping the user in live control.

---

## 2. Android Telephony Architecture & OS Limitations

### Supported System Roles & Services
1. **`AiCallScreeningService` (`android.telecom.CallScreeningService`)**
   - Screens incoming calls on Android 10+ (API 29+) without requiring `READ_CALL_LOG`.
   - Enforces the user's Blocklist (`setDisallowCall(true)`, `setRejectCall(true)`).
   - Identifies saved contacts via `ContactsContract.PhoneLookup` and notifies `CallStateCoordinator` so the AI greets the user first.
2. **`AiInCallService` (`android.telecom.InCallService`)**
   - Active when the user grants the **Default Dialer** role (`RoleManager.ROLE_DIALER`).
   - Binds live `android.telecom.Call` objects to `CallStateCoordinator` for **Answer Myself**, **Let AI Talk**, **Reject Call**, and **End Call**.
3. **`AiConnectionService` (`android.telecom.ConnectionService`)**
   - Self-managed Telecom connection service for hosted VoIP / forwarded AI phone bridge calls.
4. **`CallAssistantForegroundService`**
   - Uses `foregroundServiceType="phoneCall"` with high-priority incoming/active call notifications and `FullScreenIntent` (`IncomingCallActivity`).

### Important Android Cellular Audio Limitation & Supported Alternative
Standard Android OS security restricts third-party apps from injecting synthesized audio directly into the raw cellular uplink (`VOICE_UPLINK` / `VOICE_DOWNLINK`) on unrooted consumer devices.
- **On-Device Mode**: Uses `InCallService` / `TelecomManager` call control with speakerphone acoustic coupling (`AudioManager.MODE_IN_COMMUNICATION`) + `SpeechRecognizer` + `TextToSpeech` (subject to device/OEM acoustic behavior).
- **Carrier-Supported Alternative (Recommended for Production Cellular Calls)**: Configure **Conditional Call Forwarding** (`*61*<Hosted_AI_Number>*11*<Delay>#` for no-answer, `*62*` unreachable, `*67*` busy; cancel with `##002#`) directly in the app's **Settings -> Hosted AI Bridge & Call Forwarding** card to route calls to a hosted SIP/PSTN voice gateway (e.g., Twilio / LiveKit / Telnyx + Gemini Multimodal Live API).

---

## 3. Multilingual Voice AI & Sample Mode Notice
- **Language Detection (`LanguageDetector`)**: Automatically detects and switches across **Hindi, Punjabi, English, Urdu, Kashmiri, Bengali, Gujarati, Marathi, Tamil, Telugu, Kannada, Malayalam**, and Romanized mixed speech (e.g., Hinglish, Roman Punjabi).
- **Voice AI Engine (`VoiceAiEngine`)**: Integrates Android `SpeechRecognizer` (streaming partial results + RMS metering + **barge-in interruption detection**) and `TextToSpeech` (locale-aware voice profiles with configurable pitch & rate).
- **AI Backend Integration (`AiRepository`)**: Currently backed by `PlaceholderAiRepository` (Sample AI Mode) because Firebase setup was declined during provisioning. All AI responses, greetings, permission gates, and summaries are routed cleanly through the single `AiRepository` interface so connecting live **Gemini via Firebase AI Logic** (`Firebase.ai`) and **Cloud Firestore** requires only swapping the repository implementation after enabling Firebase.

---

## 4. Security, Privacy & Permissions
- **Call Audio Recording**: Disabled by default (`callRecordingEnabled = false`). Cannot be enabled without explicit user consent acknowledgment.
- **User Control & Permission Gates**: AI asks user approval before sharing personal info, confirming business orders, or scheduling meetings/commitments. Live controls (**Take Over Call**, **Stop AI**, **Mute AI**, **End Call**) remain accessible at all times.
- **Zero Broad Storage Permissions**: Google Play compliant (`READ_EXTERNAL_STORAGE` / `READ_CALL_LOG` omitted).
- **Local Data Deletion**: Users can permanently wipe all call records, messages, contacts, blocklists, and settings from **Settings -> Delete All Local Data**.

---

## 5. Build & Export Instructions
- **Debug APK**: `gradle :app:assembleDebug`
- **Release AAB (Play Store)**: `gradle :app:bundleRelease` (configure `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` environment variables for release signing).
- **Unit & Robolectric Tests**: `gradle :app:testDebugUnitTest`

---

## 6. Device Compatibility Matrix
| Capability | Android 10–16 (API 29–36) | Android 7–9 (API 24–28) | Streaming / Headless Emulator |
| :--- | :--- | :--- | :--- |
| `CallScreeningService` (`ROLE_CALL_SCREENING`) | Full Support | Not Available (uses `InCallService` / `READ_PHONE_STATE`) | UI Greeter Test Flow |
| `InCallService` (`ROLE_DIALER`) | Full Support | Full Support | Supported via Dialer Role |
| `TextToSpeech` Multilingual | Full Support (if language packs installed) | Full Support | Supported (host TTS) |
| `SpeechRecognizer` Live Mic & Barge-in | Full Support (requires Google/OEM Speech Service) | Full Support | Requires Speech Service; text turn input fallback included |
| Direct Cellular Uplink Audio Injection | Restricted by Android OS (use Speaker Bridge or `*61*` Hosted Number) | Restricted by Android OS | Use Hosted AI Bridge |
