import io
import json
import unittest
from unittest.mock import patch
from gateway.server import (Handler, config, MAX_BODY, MAX_UPSTREAM, NoRedirect,
                            WindowRateLimiter, positive_setting)


class GatewayTests(unittest.TestCase):
    def test_health_and_readiness_are_public_and_do_not_expose_configuration(self):
        for path, expected in (("/health", 200), ("/healthz", 200), ("/readyz", 200)):
            result = self.get(path)
            self.assertEqual(result["status_code"], expected)
            self.assertNotIn("token", json.dumps(result).lower())

    def test_readiness_fails_when_gateway_configuration_is_missing(self):
        result = self.get("/readyz", settings=None)
        self.assertEqual(result["status_code"], 503)

    def test_global_rate_limit_rejects_excess_chat_without_calling_provider(self):
        limiter = WindowRateLimiter(1, 60, clock=lambda: 100.0)
        self.request("/v1/chat", b'{"message":"first"}', "Bearer " + "x" * 24,
                     rate_limiter=limiter)
        with patch("gateway.server.generate_reply") as upstream:
            result = self.request("/v1/chat", b'{"message":"second"}', "Bearer " + "x" * 24,
                                  rate_limiter=limiter)
        self.assertEqual(result["status"], 429)
        upstream.assert_not_called()

    def test_concurrency_limit_rejects_when_upstream_slots_are_busy(self):
        from threading import BoundedSemaphore
        slots = BoundedSemaphore(1)
        slots.acquire()
        with patch("gateway.server.generate_reply") as upstream:
            result = self.request("/v1/chat", b'{"message":"hi"}', "Bearer " + "x" * 24,
                                  upstream_slots=slots)
        self.assertEqual(result["status"], 503)
        upstream.assert_not_called()
        slots.release()

    def test_upstream_slot_is_released_after_provider_error(self):
        from threading import BoundedSemaphore
        slots = BoundedSemaphore(1)
        limiter = WindowRateLimiter(10, 60, clock=lambda: 100.0)
        with patch("gateway.server.generate_reply", side_effect=[ValueError("invalid"), "recovered"]):
            failed = self.request("/v1/chat", b'{"message":"first"}', "Bearer " + "x" * 24,
                                  rate_limiter=limiter, upstream_slots=slots)
            recovered = self.request("/v1/chat", b'{"message":"second"}', "Bearer " + "x" * 24,
                                     rate_limiter=limiter, upstream_slots=slots)
        self.assertEqual(failed["status"], 502)
        self.assertEqual(recovered["status"], 200)

    def test_rate_limiter_expires_window_entries(self):
        now = [0.0]
        limiter = WindowRateLimiter(1, 10, clock=lambda: now[0])
        self.assertTrue(limiter.allow())
        self.assertFalse(limiter.allow())
        now[0] = 10.0
        self.assertTrue(limiter.allow())

    def test_gateway_limits_are_bounded_positive_settings(self):
        with patch.dict("os.environ", {"ZEAZ_LIMIT": "3"}):
            self.assertEqual(positive_setting("ZEAZ_LIMIT", 60, 4), 3)
        for value in ("0", "5", "invalid"):
            with self.subTest(value=value), patch.dict("os.environ", {"ZEAZ_LIMIT": value}):
                with self.assertRaises(ValueError):
                    positive_setting("ZEAZ_LIMIT", 60, 4)

    def test_config_rejects_http_upstream(self):
        env = {
            "ZEAZ_GATEWAY_TOKEN": "x" * 24,
            "ZEAZ_AI_API_KEY": "placeholder",
            "ZEAZ_AI_MODEL": "example-model",
            "ZEAZ_AI_BASE_URL": "http://localhost:1234/v1",
        }
        with patch.dict("os.environ", env, clear=True):
            with self.assertRaises(ValueError):
                config()

    def test_config_rejects_nonpublic_upstream_addresses(self):
        for upstream in (
            "https://127.0.0.1/v1",
            "https://10.0.0.5/v1",
            "https://192.168.1.5/v1",
            "https://169.254.169.254/v1",
            "https://[::1]/v1",
            "https://service.localhost/v1",
            "https://service.local/v1",
        ):
            with self.subTest(upstream=upstream):
                env = {
                    "ZEAZ_GATEWAY_TOKEN": "x" * 24,
                    "ZEAZ_AI_API_KEY": "placeholder",
                    "ZEAZ_AI_MODEL": "example-model",
                    "ZEAZ_AI_BASE_URL": upstream,
                }
                with patch.dict("os.environ", env, clear=True):
                    with self.assertRaises(ValueError):
                        config()

    def test_config_accepts_public_https_upstream(self):
        env = {
            "ZEAZ_GATEWAY_TOKEN": "x" * 24,
            "ZEAZ_AI_API_KEY": "placeholder",
            "ZEAZ_AI_MODEL": "example-model",
            "ZEAZ_AI_BASE_URL": "https://api.example.com/v1",
        }
        with patch.dict("os.environ", env, clear=True):
            self.assertEqual(config()[3], "https://api.example.com/v1")

    def test_config_rejects_missing_token(self):
        with patch.dict("os.environ", {}, clear=True):
            with self.assertRaises(ValueError):
                config()

    def test_redirect_is_refused(self):
        self.assertIsNone(NoRedirect().redirect_request(None, None, 302, "Found", {}, "https://other.example/v1"))

    def test_invalid_json_is_rejected(self):
        result = self.request("/v1/chat", b"{", "Bearer " + "x" * 24)
        self.assertEqual(result["status"], 400)

    def test_upstream_failure_is_bad_gateway(self):
        with patch("gateway.server.generate_reply", side_effect=ValueError("bad response")):
            result = self.request("/v1/chat", b'{"message":"hi"}', "Bearer " + "x" * 24)
        self.assertEqual(result["status"], 502)

    def test_chat_requires_auth(self):
        self.assertEqual(self.request("/v1/chat", b'{"message":"hi"}', "").get("status"), 401)

    def test_chat_rejects_empty_message(self):
        self.assertEqual(self.request("/v1/chat", b'{"message":""}', "Bearer " + "x" * 24).get("status"), 400)

    def test_chat_returns_reply(self):
        with patch("gateway.server.generate_reply", return_value="สวัสดี"):
            result = self.request("/v1/chat", b'{"message":"hi"}', "Bearer " + "x" * 24)
        self.assertEqual(result["status"], 200)
        self.assertEqual(result["reply"], "สวัสดี")

    def test_chat_stream_negotiation_requires_version_and_accept_header(self):
        result = self.request("/v1/chat", b'{"message":"hi"}', "Bearer " + "x" * 24,
                              {"X-ZEAZ-Stream-Version": "1"})
        self.assertEqual(result["status"], 406)

    def test_chat_stream_returns_ordered_deltas_and_done(self):
        upstream = io.BytesIO(
            b'data: {"choices":[{"delta":{"content":"Hello "}}]}\n\n'
            b'data: {"choices":[{"delta":{"content":"world"}}]}\n\n'
            b'data: [DONE]\n\n')
        with patch("gateway.server.open_upstream", return_value=upstream):
            result = self.request("/v1/chat", b'{"message":"hi"}', "Bearer " + "x" * 24,
                                  {"Accept": "text/event-stream",
                                   "X-ZEAZ-Stream-Version": "1"})
        self.assertEqual(result["status"], 200)
        self.assertIn('event: delta\ndata: {"sequence": 0, "text": "Hello "}', result["body"])
        self.assertIn('event: delta\ndata: {"sequence": 1, "text": "world"}', result["body"])
        self.assertIn('event: done\ndata: {"sequence": 2}', result["body"])

    def test_chat_stream_rejects_malformed_provider_chunk_after_partial_reply(self):
        upstream = io.BytesIO(
            b'data: {"choices":[{"delta":{"content":"partial"}}]}\n\n'
            b'data: {not-json}\n\n')
        with patch("gateway.server.open_upstream", return_value=upstream):
            result = self.request("/v1/chat", b'{"message":"hi"}', "Bearer " + "x" * 24,
                                  {"Accept": "text/event-stream",
                                   "X-ZEAZ-Stream-Version": "1"})
        self.assertEqual(result["status"], 200)
        self.assertIn('"text": "partial"', result["body"])
        self.assertIn('event: error\ndata: {"error": "invalid_upstream_response"}', result["body"])
        self.assertNotIn("not-json", result["body"])

    def test_chat_stream_bounds_provider_bytes(self):
        upstream = io.BytesIO(b"".join((b":" + b"x" * 7998 + b"\n")
                                        for _ in range(MAX_UPSTREAM // 8000 + 2)))
        with patch("gateway.server.open_upstream", return_value=upstream):
            result = self.request("/v1/chat", b'{"message":"hi"}', "Bearer " + "x" * 24,
                                  {"Accept": "text/event-stream",
                                   "X-ZEAZ-Stream-Version": "1"})
        self.assertEqual(result["status"], 502)
        self.assertEqual(result["error"], "invalid_upstream_response")

    def test_chat_stream_requires_upstream_done_marker(self):
        upstream = io.BytesIO(b'data: {"choices":[{"delta":{"content":"partial"}}]}\n\n')
        with patch("gateway.server.open_upstream", return_value=upstream):
            result = self.request("/v1/chat", b'{"message":"hi"}', "Bearer " + "x" * 24,
                                  {"Accept": "text/event-stream",
                                   "X-ZEAZ-Stream-Version": "1"})
        self.assertIn('event: error\ndata: {"error": "invalid_upstream_response"}', result["body"])
        self.assertNotIn("event: done", result["body"])

    def test_chat_stream_enforces_total_upstream_deadline(self):
        elapsed = [0.0]

        class SlowStream(io.BytesIO):
            def readline(self, size=-1):
                elapsed[0] = 121.0
                return super().readline(size)

        with patch("gateway.server.open_upstream", return_value=SlowStream(
                b'data: {"choices":[{"delta":{"content":"late"}}]}\n\n')):
            with patch("gateway.server.time.monotonic", side_effect=lambda: elapsed[0]):
                result = self.request("/v1/chat", b'{"message":"hi"}', "Bearer " + "x" * 24,
                                      {"Accept": "text/event-stream",
                                       "X-ZEAZ-Stream-Version": "1"})
        self.assertEqual(result["status"], 502)
        self.assertEqual(result["error"], "upstream_unavailable")

    def request(self, path, body, authorization, extra_headers=None, rate_limiter=None, upstream_slots=None):
        class FakeHandler(Handler):
            def __init__(self):
                self.path = path
                self.headers = {"Authorization": authorization, "Content-Length": str(len(body))}
                self.headers.update(extra_headers or {})
                self.rfile = io.BytesIO(body)
                self.wfile = io.BytesIO()
                self.settings = ("x" * 24, "placeholder", "model", "https://example.com/v1")
                self.rate_limiter = rate_limiter or WindowRateLimiter(1000, 60)
                from threading import BoundedSemaphore
                self.upstream_slots = upstream_slots or BoundedSemaphore(4)
                self.status = None

            def send_response(self, status):
                self.status = status

            def send_header(self, *_):
                pass

            def end_headers(self):
                pass

            def respond(self, status, data):
                self.status = status
                self.data = data
                return None

        handler = FakeHandler()
        handler.do_POST()
        return {"status": handler.status, **getattr(handler, "data", {}),
                "body": handler.wfile.getvalue().decode("utf-8", errors="replace")}

    def get(self, path, settings=("x" * 24, "placeholder", "model", "https://example.com/v1")):
        class FakeHandler(Handler):
            def __init__(self):
                self.path = path
                self.settings = settings
                self.status = None

            def send_response(self, status): self.status = status
            def send_header(self, *_): pass
            def end_headers(self): pass
            def respond(self, status, data): self.status, self.data = status, data

        handler = FakeHandler()
        handler.do_GET()
        return {"status_code": handler.status, **getattr(handler, "data", {})}


if __name__ == "__main__":
    unittest.main()
