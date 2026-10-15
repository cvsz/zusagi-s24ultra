# ZEAZDEV Ultimate Super Intelligence — Galaxy S24 Ultra

## Scope
This is a no-root Android client foundation, not AGI. The existing repository is a reusable zTemplate; its generic governance and validation remain unchanged.

## Mobile
- Android Java, minimum API 28; Android 15 target SDK 35.
- HTTPS-only API communication, per-session bearer token input; token is deliberately not persisted.
- Android built-in voice recognition intent and Thai speech synthesis; voice recognition availability depends on installed services.
- No Accessibility service, device admin, root, unrestricted shell execution, or background surveillance.
- The client expects POST /v1/chat with JSON `{"message":"..."}` and a JSON `{"reply":"..."}` response.

## Server contract and planned integrations
ZEAZDEV must host an independently authenticated gateway, add per-device authorization, rate limits, request-size bounds, replay controls and redacted audit logging. Multi-agent orchestration, memory, task execution and Android automation are **not implemented by this mobile foundation**.

Recommended backend modules:
- Router/planner + bounded specialist agents
- PostgreSQL/pgvector memory with tenant isolation, retention and deletion
- Approval-gated task execution with allowlisted tools and append-only audit
- Voice transport alternatives for low latency

## Build
From `android/`, with JDK 17, Android SDK 35 and Gradle 8.9 installed:
```bash
gradle :app:assembleDebug
```
Output: `android/app/build/outputs/apk/debug/app-debug.apk`. The APK is unsigned for release purposes and must be signed for distribution.

## Security & Verification
Do not embed API keys or deploy an unauthenticated gateway. Device tokens are session-only; adopt Android Keystore for durable per-device credentials in a later iteration.
Before release: validate Gradle build, static security scanning, Android 14 device install, gateway authentication failure/success, network failure handling, voice behavior, permission review, and signed APK provenance.
Rollback: revert this feature branch/PR; no existing runtime or template files were replaced.
