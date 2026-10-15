# ZEAZ Ultimate Android UI

The application uses a chat-first dark interface built with Android SDK widgets in Java 17. It needs no third-party UI dependencies.

## Features
- Gateway endpoint field, persisted using activity preferences.
- Session-only password-masked device token. The token is not persisted to local storage.
- Thai speech recognition intent, preserving the platform's recognition flow.
- Thai text-to-speech playback after a response arrives.
- Network requests off the UI thread with connect/read timeouts, no redirects, bounded successful response, and HTTP error handling.
- Disabled Send button while a response is pending.

## Run
Build the Android debug APK with the existing Android GitHub Actions workflow or Android Studio. Install on your S24 Ultra. Configure the **HTTPS origin** of the gateway (no /v1/chat suffix), enter the device token, and send a message.

**Never put private provider credentials inside the APK.** The application requires a verified HTTPS reverse proxy forwarding POST /v1/chat to the loopback gateway. The endpoint is not preconfigured and this change does not deploy a server.

## Verification
- Check Android build and lint where available, then install the generated APK.
- Verify portrait layout, keyboard dismissal, speech-recognition availability, Thai TTS, network failures, HTTP 401 behavior, and a successful authenticated gateway request.
- Confirm endpoint preferences persist across restarts and device token does not.
- Review accessibility contrast and small/large font sizes on the target device.
- No live device or HTTPS gateway E2E verification was performed as part of this PR.

## Limitations
This is a single-turn chat interface with response playback, not a multi-turn persistent chat, remote tools, or a production-grade agent executor. Android device tests, credential rotation and reverse proxy hardening remain release gates.

## JARVIS-inspired depth HUD
- A custom Canvas-based animated holographic core appears below the header (elliptical 3D-style rings, perspective grid, pulsing center, rotating status arcs).
- Processing indicator reacts to outgoing AI requests; HUD is decorative and has no microphone, camera or network access.
- Avoids third-party rendering SDKs and preserves Java 17, API 28+ compatibility.
- Requires real-device performance, pause/resume, and accessibility/reduced-motion checks before release.
