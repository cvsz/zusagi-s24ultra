import io
import json
import unittest
from unittest.mock import patch
from gateway.server import Handler, config, MAX_BODY, NoRedirect


class GatewayTests(unittest.TestCase):
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

    def request(self, path, body, authorization):
        class FakeHandler(Handler):
            def __init__(self):
                self.path = path
                self.headers = {"Authorization": authorization, "Content-Length": str(len(body))}
                self.rfile = io.BytesIO(body)
                self.wfile = io.BytesIO()
                self.settings = ("x" * 24, "placeholder", "model", "https://example.com/v1")
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
        return {"status": handler.status, **handler.data}


if __name__ == "__main__":
    unittest.main()
