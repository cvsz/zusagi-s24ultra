# ZEAZDEV Ultimate Super Intelligence — Galaxy S24 Ultra

## Scope
This is a no-root Android client foundation, not AGI. The existing repository is a reusable zTemplate; its generic governance and validation remain unchanged.

## Mobile
- Android Java, minimum API 28; Android 15 target SDK 35.
- HTTPS-only API communication, per-session bearer token input; token is deliberately not persisted.
- Android built-in voice recognition intent and Thai speech synthesis; voice recognition availability depends on installed services.
- No Accessibility service, device admin, root, unrestricted shell execution, or background surveillance.
- Chat expects POST `/v1/chat` with JSON `{"message":"..."}` and a JSON `{"reply":"..."}` response.

The separate **Analyze Token (Advisory)** action accepts `chain:symbol:address` in the message field and sends POST `/v1/analyze` with `{"token":{"chain":"...","symbol":"...","address":"..."}}`. It sends the session-only device bearer token, follows no redirects, and expects an object containing numeric `scores.opportunity`, `scores.risk`, and `scores.confidence` plus non-empty string `action` and `rug_risk` fields. A malformed or oversized response is rejected rather than displayed as zero-valued scores. This action is read-only decision support: it does not place trades, submit orders, or mutate wallet state.

The gateway implemented in this repository is chat-only and does not provide `/v1/analyze`. Both actions share one configured HTTPS origin, so a deployment needs a separately reviewed router that sends each path to the correct service. The Android bearer header alone is not proof that the analysis service authenticates it. Do not publish the analyzer route unless server-side authentication, authorization, tenant isolation, rate limits, and safe error handling are independently verified. The device token remains session-only and must not be reused as a provider credential.

## Server contract and planned integrations
ZEAZDEV must host an independently authenticated gateway, add per-device authorization, rate limits, request-size bounds, replay controls and redacted audit logging. Multi-agent orchestration, memory, task execution and Android automation are **not implemented by this mobile foundation**.

Recommended backend modules:
- Router/planner + bounded specialist agents
- PostgreSQL/pgvector memory with tenant isolation, retention and deletion
- Approval-gated task execution with allowlisted tools and append-only audit
- Voice transport alternatives for low latency

## Build
From `android/`, with JDK 17, Android SDK 35 and Gradle 8.14.5 installed:
```bash
gradle :app:assembleDebug
```
Output: `android/app/build/outputs/apk/debug/app-debug.apk`. The APK is unsigned for release purposes and must be signed for distribution.

## Security & Verification
Do not embed API keys or deploy an unauthenticated gateway. Device tokens are session-only; adopt Android Keystore for durable per-device credentials in a later iteration.
Before release: validate Gradle build, static security scanning, Android 14 device install, gateway authentication failure/success, network failure handling, voice behavior, permission review, and signed APK provenance.
Rollback: revert this feature branch/PR; no existing runtime or template files were replaced.
