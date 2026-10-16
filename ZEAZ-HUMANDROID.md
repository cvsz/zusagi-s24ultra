# ZEAZ Humandroid: Reference Review and Delivery Record

## Purpose and status

This document records the six supplied visual references and the evidence-backed implementation state of the ZEAZ Android application. The videos are visual inspiration; they do not establish the functionality, safety, or API contracts of the software shown in them.

In this implementation, **4D** means a 3D procedural avatar whose pose changes over time and with assistant state. It does not mean a volumetric/light-field display or physical hologram.

The source videos were available at `C:\Users\cvsz.ZEAZ\Videos\zarvis` and inspected from the WSL-mounted path `/mnt/c/Users/cvsz.ZEAZ/Videos/zarvis`. The six files are `humandroid-1.mp4` through `humandroid-6.mp4`. The first five are portrait 720×1280 clips (about 14–56 seconds); the sixth is a landscape 640×360 clip (about 252 seconds). Frame samples were inspected across each clip. No video files or extracted frames are copied into the repository.

## Reference-by-reference observations

1. **`humandroid-1.mp4` — conversational humanoid display.** A cyan, contour-line bust with a warm gold/orange face is shown on a physical monitor, with subtitles describing questions and status checks. The clip supports the visual direction and conversational presentation only; it does not prove the assistant integration or the displayed capabilities.
2. **`humandroid-2.mp4` — humanoid concept imagery.** A short sequence presents several full-bust and full-body wireframe humanoid designs on black, shifting from monochrome to cyan/gold. This is concept art; no runtime behavior or rigging is established.
3. **`humandroid-3.mp4` — circular command HUD.** A monitor shows a central cyan/gold ring, connected labels and indicators, and side/status elements. It motivates a clear central status visualization and modular data labels, but is not evidence of live telemetry.
4. **`humandroid-4.mp4` — multi-screen interaction.** A rotated handheld recording shows a multi-monitor desk: a command/status dashboard, an avatar display, and prompts or subtitles. The phone orientation, desktop layout, and conversational scene are observable; the underlying interaction flow is not verified.
5. **`humandroid-5.mp4` — animated humanoid and energy field.** A cyan/gold bust appears against a moving wave-like field, with a person gesturing near the display and captions referring to smoother updates. It suggests layered ambient animation and state-responsive visuals. The clip does not establish hand tracking, audio reactivity, or frame-rate performance.
6. **`humandroid-6.mp4` — command-center software.** A landscape desktop interface includes a central circular AI panel, command/event areas, and sequence/workflow panels, interspersed with other desktop windows. It motivates a modular dashboard and workflow preview. The video does not verify the application’s backend, agent execution, camera, pairing, or approval security.

These observations are based on sampled frames, not a frame-by-frame interaction audit. Audio, captions, and visible UI are not treated as authoritative API or system behavior.

## Current repository implementation

The repository currently contains a native Java Android screen, an animated Canvas HUD with a procedural 3D wireframe humanoid bust, Thai-language system speech recognition launch, Thai Text-to-Speech, HTTPS chat, and a read-only advisory `/v1/analyze` client. The bust is generated from ellipsoid meshes and facial contours, perspective-projected to Canvas, with time-varying head yaw and breathing. It is an original low-poly wireframe rendering, not a photorealistic rigged human, volumetric/light-field hologram, or measured-performance avatar. A deterministic assistant state machine models initialization, disconnected, idle, listening, transcribing, thinking, streaming, speaking, error, and paused states. TTS requests transient Android audio focus, stops on focus loss, and releases it on completion, interruption, pause, or teardown. The chat path now negotiates bounded SSE protocol v1 and renders ordered response deltas incrementally; it rejects malformed, out-of-order, oversized, or unterminated streams. The application manifest requests internet access only; it does not request microphone or camera permission. Speech recognition is launched through Android’s recognizer UI, subject to provider behavior and explicit user gesture.

The repository’s Gateway currently implements a chat route, not the Android advisory `/v1/analyze` contract. The client’s HTTPS and redirect checks do not substitute for a verified server-side authentication, authorization, rate-limit, tenant-isolation, or network-egress design. Do not configure this client against a public service until those controls and the endpoint contract are independently verified.

### zworkforce contract investigation

A local zworkforce source snapshot documents `GET /v1/tasks` and `GET /v1/tasks/{id}`. Those routes require trusted-edge headers containing a fixed owner ID and `ZARVIS_EDGE_SHARED_SECRET`; its README says the secret must be at least 32 bytes and must never reach the browser. The snapshot describes a single-owner runtime, not a tenant-scoped device API. No verified deployment URL, TLS edge configuration, device identity mapping, or per-device authorization contract was available. The Android app must not receive the edge secret, and proxying owner-wide task data behind the current shared Gateway token would broaden access. Therefore no zworkforce connection or command dashboard is enabled; read-only integration remains blocked until an authenticated server-side identity mapping and deployment contract are verified.

## Trust boundaries and implementation constraints

- Keep model/provider credentials on a server; never embed them in an APK.
- Treat model output and remote status/event data as untrusted display content. Never interpret it as authorization or shell input.
- Keep integrations read-only until the actual zworkforce API, identity model, authorization checks, approval consumption, idempotency, audit trail, and emergency-stop contract are verified. No endpoint contract is inferred from the video or prompt.
- Keep microphone activation user-initiated. No background recording or implicit wake-word service is in scope.
- Camera preview and QR pairing remain optional and unimplemented. Do not add permissions until a concrete user flow and privacy/security contract are approved and tested.
- Do not add persistent conversation memory without an explicit user control, retention/deletion behavior, and data boundary.
- Preserve the reusable-template repository’s security, CI, and governance requirements.

## Delivery milestones

1. **Android stability and review fixes:** API-compatible bounded responses, secure transport checks, edge-to-edge/IME layout, stale-request protection, instrumentation coverage, and a successful device test run are implemented. The device run validates startup/state and rendered pixels, but the device was dozing during the post-test screenshot attempt; that black lock-screen capture is not evidence of the full app layout.
2. **Deterministic assistant state model:** implemented for current flows. A pure Java transition model rejects illegal events without changing state, preserves the underlying operation while paused, and applies completion/error events received while paused before restoring on resume. The activity projects machine state into the HUD; network generations and unique TTS utterance IDs suppress stale callbacks. Instrumentation covers the voice/request/streaming happy path, illegal events, and asynchronous response completion while paused. Provider permission UX and device audio behavior remain separate verification work.
3. **Humanoid renderer:** an original lightweight procedural 3D wireframe bust with cyan/gold contours, perspective projection, time-varying pose, and breathing is implemented in the native Canvas HUD. Instrumentation checks color output and pose changes. This avoids a third-party rendering engine for the current low-poly mesh. The device was not unlocked for a valid full-screen screenshot, and FPS/battery profiling remains unverified. Confirm reduced-motion and off-screen suspension, then profile before deciding whether to move to a GPU renderer. Android supports OpenGL ES, while current Android guidance prefers Vulkan for new high-performance graphics; [Android's graphics overview](https://developer.android.com/games/develop/vulkan/overview) and [Google Filament](https://github.com/google/filament) are future evaluation references if measured Canvas performance is insufficient.
4. **Thai voice lifecycle:** TTS utterance identity and audio-focus acquisition/release/interruption behavior are implemented; instrumentation tests the focus controller’s grant, denial, idempotent release, and interruption paths. Manual recognition, permission/provider behavior, Thai TTS availability/quality, and real competing-audio interruption remain unverified. Do not promise Thai availability on devices lacking the required recognition/TTS language data.
5. **Gateway streaming and operations:** SSE v1 is implemented for the existing authenticated `/v1/chat` route and documented in `docs/ultimate-gateway-mvp.md`. It requires explicit `Accept` and protocol-version headers, bounds upstream and downstream bytes and total duration, requires the provider terminal marker, sequences deltas, sanitizes mid-stream errors, and closes the upstream when the client disconnects. Android validates sequence and terminal events, bounds line, aggregate bytes, and total duration, displays deltas, and disconnects the request when the Activity is destroyed. Live provider compatibility, proxy buffering behavior, operational rate/concurrency limits, and real network cancellation remain to be validated before production.
6. **Read-only command dashboard:** discover and verify zworkforce contracts first. Then implement read-only agent/task/health/audit views with freshness and error states. Workflow planning and consequential actions require separately reviewed authority/approval contracts.
7. **Release gates:** run required CI/security checks on the exact reviewed commit; obtain independent review; verify signed artifact provenance, target-device behavior, deployment authorization, monitoring, and rollback before making a production claim.

## Verification record

Record exact commit SHA, commands, results, device/API, and artifact hashes in the PR and release evidence. Distinguish `IMPLEMENTED`, `TESTED`, `APPROVED`, `MERGED`, and `DEPLOYED`; these are separate states. A local build or device smoke test is not proof of production Gateway readiness, zworkforce integration, release signing, approval, merge, or deployment.

### State-machine implementation validation

- Android Java and instrumentation sources compiled with `:app:compileDebugAndroidTestJavaWithJavac`.
- `:app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` passed; lint reported 0 errors and 12 warnings (target/compile SDK advisories and existing hardcoded UI strings).
- `:app:connectedDebugAndroidTest` passed 9 tests on Samsung `SM-S948B`, Android 17/API 37; 0 failures, 0 errors, 0 skipped. This includes state transitions, illegal-transition preservation, pause/resume restoration, and earlier HUD/API checks.
- The full-screen UI was not manually inspected in this run. Thai speech-recognition quality, TTS audio focus/quality, live Gateway reachability, FPS, battery use, and release signing remain unverified.

### TTS audio-focus validation

- `:app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` passed with 0 lint errors; the report currently contains SDK advisories and hardcoded UI text warnings.
- `:app:connectedDebugAndroidTest` passed 11 tests on `SM-S948B`, Android 17/API 37; 0 failures, 0 errors, 0 skipped. Added tests verify focus denial, one request per lease, interruption delivery once, and idempotent release.
- The isolated app was rebuilt and installed as `dev.zeaz.ultimate.s26smoke`. Physical TTS playback and competing-audio interruption were not manually exercised because the display remained in Dozing state.
- Android’s audio-focus guidance requires checking `AUDIOFOCUS_REQUEST_GRANTED`, using the same `AudioFocusRequest` to abandon focus, and pausing spoken content when focus is lost; see [Manage audio focus](https://developer.android.com/media/optimize/audio-focus).

At the time this record is written, all six supplied videos were reviewed and the app has an animated, perspective-projected procedural 3D wireframe bust in the native HUD, authenticated chat with SSE v1, and basic voice interaction. Production Humandroid scope is incomplete. No true volumetric display, persistent memory, command center, zworkforce integration, camera preview, QR pairing, measured performance target, or production rollout is claimed.

### Streaming implementation validation

- Gateway unit tests cover negotiation, ordered deltas, malformed provider chunks, upstream byte and duration bounds, required terminal markers, and the existing JSON compatibility path.
- `make validate-template` passed: repository/link validation and all 24 Python tests.
- `:app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest` passed; the Samsung `SM-S948B` (Android 17/API 37) ran 15 instrumentation tests with 0 failures and 0 skips, including SSE parser order, termination, duration, and size checks.
- Lint reported 13 existing warnings (target/compile SDK, dependency freshness, and hard-coded UI strings), with no lint errors.
- Debug APK `dev.zeaz.ultimate.s26smoke` was installed and launched. The Activity was observed resumed and the captured log window contained no fatal exception. The display was dozing during earlier inspection; no manual live Gateway stream or speech interaction was performed.
- Debug APK SHA-256: `66dbcd0120fd4a2abbc9c71dd8fdc9cd80ee6b02fe119a8c7ab4c81ba3d65030`. Android test APK SHA-256: `fce47048e3f3ffc594954f5ea5c62cba73efa51b6ac9838b224cb85f2bcf5bca`.
- SSE v1 implementation remains in the feature worktree until signed commit, exact-head CI, and independent review complete.
