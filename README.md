# zusagi-s26ultra

## ZEAZ Ultimate Ollama Gateway — development pilot

A standalone local gateway for `POST /v1/chat`, returning `{"reply":"..."}`. Uses Ollama at `127.0.0.1:11434`, with a bounded request body, constant-time bearer-token comparison, two concurrent upstream requests, and no device-side provider credentials. No third-party Python packages.

## Start on the server hosting Ollama

```bash
read -rsp 'Choose a long device token: ' ZEAZ_GATEWAY_TOKEN; echo
export ZEAZ_GATEWAY_TOKEN
export ZEAZ_AI_MODEL=qwen2.5-coder:3b
python3 ollama_gateway.py
```

In a separate terminal:

```bash
curl -i http://127.0.0.1:8765/health
read -rsp 'Device token: ' ZEAZ_GATEWAY_TOKEN; echo
curl -i --max-time 180 http://127.0.0.1:8765/v1/chat \
  -H "Authorization: Bearer $ZEAZ_GATEWAY_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"message":"สวัสดี ZEAZ Ultimate"}'
unset ZEAZ_GATEWAY_TOKEN
```

## HTTPS and phone access

Do not expose port 8765 to the LAN or Internet. The listener is loopback-only. If Android access is needed, configure a dedicated authenticated HTTPS route such as `https://YOUR_GATEWAY_HOST` through a trusted reverse proxy, forwarding only the required gateway paths to `http://127.0.0.1:8765`. Verify the hostname, TLS, routing and authentication before setting Android's gateway URL to that HTTPS origin. Protect the route with per-device authorization, rate limits, access logs without secrets, and abuse controls.

This is **not production-ready**: static shared bearer token, no per-device revocation/expiry, no persistent rate limiting, no robust graceful shutdown, no observability, no automated integration tests. Rotate credentials before any public pilot. Keep tokens outside git, shell history, screenshots and log output. Run behind a supervised service after validation. Open WebUI remains unchanged.
