# ZEAZ Ultimate Gateway MVP

This is an initial **chat-only AI assistant vertical slice**, not a production-ready multi-agent system. It implements the Android `POST /v1/chat` contract. The Android app also has a separate read-only token-advisory action, but this gateway does not implement `POST /v1/analyze`; configure a separately reviewed service and authenticated router before using that action. No external tools, device commands, repository mutations, background execution, or memory are enabled.

## Local setup

Requires Python 3.12+ and an OpenAI-compatible chat-completions provider with an HTTPS endpoint. The AI provider/model is operator-selected; no provider is bundled or presumed free.

```bash
export ZEAZ_GATEWAY_TOKEN="$(python3 -c 'import secrets; print(secrets.token_urlsafe(32))')"
export ZEAZ_AI_BASE_URL="https://YOUR_PROVIDER_HOST/v1"
export ZEAZ_AI_MODEL="YOUR_MODEL"
export ZEAZ_AI_API_KEY="YOUR_PRIVATE_PROVIDER_KEY"
python3 -m gateway.server
```

Do not paste tokens into GitHub, commit them, or place provider credentials on Android. `ZEAZ_GATEWAY_TOKEN` is a separate device-to-gateway credential. Use a secrets manager for persistent deployment.

```bash
python3 -m unittest discover -s tests -p 'test_gateway*.py' -v
```

## Network and security

The server deliberately binds to loopback only (default `127.0.0.1:8765`) and **does not serve TLS**. To use it from Android, deploy behind a trusted HTTPS reverse proxy or Cloudflare Tunnel with authentication, per-device authorization, rate limiting, and request/audit controls. Never expose the raw loopback HTTP listener publicly or reuse the provider API key as a device token.

Android configuration:
- Gateway URL: your verified HTTPS origin, without `/v1/chat`
- Device token: the separately provisioned `ZEAZ_GATEWAY_TOKEN`
- Send: `POST /v1/chat` with `{"message":"..."}`, response `{"reply":"..."}`

For incremental chat responses, negotiate **SSE protocol v1** on the same authenticated route by sending both `Accept: text/event-stream` and `X-ZEAZ-Stream-Version: 1`. The Gateway emits UTF-8 server-sent events:

```text
event: delta
data: {"sequence":0,"text":"..."}

event: done
data: {"sequence":1}

```

Each `delta` sequence starts at zero and increases by one. The `done` sequence is the number of deltas and is mandatory. A sanitized `event: error` can follow deltas if the upstream fails. Clients must reject missing, duplicated, out-of-order, malformed, or oversized events and discard any incomplete answer. The Android client caps a stream at 512 KiB, an individual line at 16 KiB, and total stream duration at 120 seconds (with a 15-second socket read timeout). The Gateway caps provider stream bytes at 256 KiB, serialized client events at 512 KiB, total upstream duration at 120 seconds, and upstream socket inactivity at 10 seconds. Legacy JSON clients that omit both negotiation headers retain the JSON response contract; partial negotiation is rejected with 406.

The token-advisory action sends `POST /v1/analyze` with `{"token":{"chain":"...","symbol":"...","address":"..."}}` and a bearer device token. The Android client rejects redirects, limits responses to 256 KiB, and requires numeric `opportunity`, `risk`, and `confidence` values plus non-empty `action` and `rug_risk` strings. These client checks do not authenticate or secure the analysis service. The gateway in this repository has no `/v1/analyze` route; do not expose an external analyzer publicly until its authentication, authorization, rate limits, tenant isolation, and routing have been verified independently.

## Limitations and release gates

- Static bearer token is suitable only for controlled development; replace with expiring per-device credentials and revocation before production.
- No tool execution, multi-agent planning, durable memory, push notifications, or background actions. SSE v1 streams chat text only; it does not stream tool events or authorize actions.
- Gateway currently depends on the configured provider's OpenAI-compatible response contract.
- Require TLS, proxy rate limits, provider allowlisting, operational logs with redaction, concurrency limits, and device integration tests before production.
- Test actual S24 Ultra installation, network failure, authentication failure, voice/TTS, and rollback. CI passing alone does not establish device readiness.
