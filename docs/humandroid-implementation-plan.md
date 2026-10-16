# ZEAZ Ultimate Humandroid delivery plan

## Scope and provenance
The six Humandroid video references are visual inspiration, not evidence of functioning APIs, autonomous actions, or implementation details. Implement original Android-native rendering and controls. No third-party assets are copied.

## Release gates
1. Android 15 edge-to-edge, IME, navigation bar, rotation and accessibility verified on SM-S948B.
2. HUD state machine: IDLE, LISTENING, THINKING, SPEAKING, ERROR. Rendering pauses off-screen and respects reduced motion.
3. Speech recognition is user-initiated; TTS lifecycle and interruptions must reset the HUD to IDLE. No background microphone capture.
4. Gateway remains HTTPS-only with no redirects, no persistent device token, bounded responses and timeouts.
5. Command dashboard starts read-only. zworkforce actions require authenticated session, scoped permission, explicit approval, idempotency keys, audit evidence and emergency stop. No silent execution.
6. Optional overlay requires user opt-in, explicit Android permission, clear dismiss affordance and no deceptive screen capture or interference.
7. CI, independent reviewer approval, device screenshot and crash-free startup evidence required before merge.

## Milestones
- M1: Android stability and insets (existing PR #8).
- M2: Procedural 3D wireframe bust projected into the native Canvas HUD; deterministic full assistant state machine remains a separate incomplete milestone.
- M3: Speech lifecycle integration, error recovery, tests.
- M4: Authenticated streaming AI Gateway, memory consent and observability.
- M5: Read-only zworkforce dashboard and approved commands.
- M6: Opt-in floating assistant, performance profiling, release signing and rollout.

## Verification on device
Build from latest branch and abort installation on build failure. Capture SHA, build log, screenshot, crash buffer, foreground PID, and manual results for scroll-to-bottom, IME, voice, TTS, network errors and offline mode. Never include tokens in evidence.
