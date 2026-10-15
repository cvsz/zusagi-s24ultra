# ZEAZ Ultimate Gateway MVP

This is an initial **chat-only AI assistant vertical slice**, not a production-ready multi-agent system. It implements the existing Android `POST /v1/chat` contract. No external tools, device commands, repository mutations, background execution, or memory are enabled.

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

## Limitations and release gates

- Static bearer token is suitable only for controlled development; replace with expiring per-device credentials and revocation before production.
- No tool execution, multi-agent planning, durable memory, streaming, push notifications, or background actions.
- Gateway currently depends on the configured provider's OpenAI-compatible response contract.
- Require TLS, proxy rate limits, provider allowlisting, operational logs with redaction, concurrency limits, and device integration tests before production.
- Test actual S24 Ultra installation, network failure, authentication failure, voice/TTS, and rollback. CI passing alone does not establish device readiness.
