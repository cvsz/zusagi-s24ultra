import io
import json
import unittest
from unittest.mock import patch

import ollama_gateway


class OllamaGatewayTests(unittest.TestCase):
    def setUp(self):
        ollama_gateway.TOKEN = "x" * 32

    def test_chat_requires_device_token(self):
        handler = self.request(b'{"message":"hello"}', authorization="")
        self.assertEqual(handler.status, 401)
        self.assertEqual(handler.payload, {"error": "unauthorized"})

    def test_chat_rejects_oversized_body(self):
        handler = self.request(b"x" * (ollama_gateway.MAX_BODY + 1))
        self.assertEqual(handler.status, 413)
        self.assertEqual(handler.payload, {"error": "invalid_body_size"})

    def test_chat_returns_ollama_reply(self):
        response = _FakeResponse(
            json.dumps({"message": {"content": "สวัสดี"}}, ensure_ascii=False).encode("utf-8")
        )
        with patch.object(ollama_gateway, "urlopen", return_value=response):
            handler = self.request(b'{"message":"hello"}')
        self.assertEqual(handler.status, 200)
        self.assertEqual(handler.payload, {"reply": "สวัสดี"})

    def request(self, body, authorization="Bearer " + "x" * 32):
        class FakeHandler(ollama_gateway.Handler):
            def __init__(self):
                self.path = "/v1/chat"
                self.headers = {
                    "Authorization": authorization,
                    "Content-Length": str(len(body)),
                }
                self.rfile = io.BytesIO(body)
                self.status = None
                self.payload = None

            def reply(self, status, payload):
                self.status = status
                self.payload = payload

        handler = FakeHandler()
        handler.do_POST()
        return handler


class _FakeResponse:
    def __init__(self, body):
        self.body = body

    def __enter__(self):
        return self

    def __exit__(self, *_):
        return False

    def read(self, _limit):
        return self.body


if __name__ == "__main__":
    unittest.main()
