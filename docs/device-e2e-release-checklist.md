# Device E2E and release checklist

This checklist must be completed on a real Samsung S24 Ultra before production release.

## Prerequisites
- Build Android APK from the exact reviewed commit SHA.
- Keep the gateway bound to 127.0.0.1 on the host.
- Provision a trusted HTTPS reverse proxy with authentication and rate limiting.
- Verify POST /v1/chat with a locally entered token; never paste secrets into issues or CI logs.
- Ensure the gateway adapter and provider contract match (Ollama /api/chat is **not** the HTTPS OpenAI-compatible provider used by gateway/server.py).

## Manual acceptance
1. Install the APK and record its package version, source SHA and Android OS version.
2. Launch the app and verify HUD renders, stops on background, and respects reduced-motion setting.
3. Enter an invalid HTTP origin: confirm the app rejects it before any request.
4. Enter HTTPS gateway origin and a deliberately invalid token: expect 401.
5. Enter a valid token and ask a Thai question: expect a non-empty Thai response.
6. Use Voice input, confirm Thai text appears, and send it.
7. Play Thai TTS; background/resume and rotate the device; confirm no crashes.
8. Restart the app: endpoint persists, token does not.
9. Simulate network outage and oversized upstream reply; confirm safe errors.
10. Reinstall the prior known-good APK and confirm rollback is possible.

## Release gates
- Required CI and security checks green on exact head.
- Independent reviewer approval on exact head.
- Signed release artifact provenance and rollback evidence.
- Documented proxy rate limiting, TLS, credential rotation and log redaction.

No device, live HTTPS gateway, GPG signature, or production deployment is claimed by this checklist.
