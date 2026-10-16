"""Loopback-only ZEAZ Android -> local Ollama gateway (development pilot)."""
import hmac
import json
import os
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.request import Request, urlopen
from urllib.error import URLError, HTTPError

TOKEN = os.environ.get('ZEAZ_GATEWAY_TOKEN', '')
MODEL = os.environ.get('ZEAZ_AI_MODEL', 'qwen2.5-coder:3b')
OLLAMA = 'http://127.0.0.1:11434/api/chat'
MAX_BODY = 8192
MAX_MESSAGE = 4000
MAX_RESPONSE = 262144
SEMAPHORE = threading.BoundedSemaphore(2)

class Handler(BaseHTTPRequestHandler):
    def reply(self, code, payload):
        body = json.dumps(payload, ensure_ascii=False).encode('utf-8')
        self.send_response(code)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Cache-Control', 'no-store')
        self.send_header('X-Content-Type-Options', 'nosniff')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == '/health':
            return self.reply(200, {'status': 'ok'})
        return self.reply(404, {'error': 'not_found'})

    def do_POST(self):
        if self.path != '/v1/chat':
            return self.reply(404, {'error': 'not_found'})
        supplied = self.headers.get('Authorization', '')
        if not hmac.compare_digest(supplied.encode(), ('Bearer ' + TOKEN).encode()):
            return self.reply(401, {'error': 'unauthorized'})
        try:
            size = int(self.headers.get('Content-Length', '-1'))
        except ValueError:
            return self.reply(400, {'error': 'invalid_length'})
        if not 0 < size <= MAX_BODY:
            return self.reply(413, {'error': 'invalid_body_size'})
        try:
            payload = json.loads(self.rfile.read(size))
        except (ValueError, UnicodeDecodeError):
            return self.reply(400, {'error': 'invalid_json'})
        message = payload.get('message') if isinstance(payload, dict) else None
        if not isinstance(message, str) or not 0 < len(message.strip()) <= MAX_MESSAGE:
            return self.reply(400, {'error': 'invalid_message'})
        if not SEMAPHORE.acquire(blocking=False):
            return self.reply(503, {'error': 'busy'})
        try:
            upstream = json.dumps({'model': MODEL, 'stream': False, 'messages': [
                {'role': 'system', 'content': 'You are ZEAZ Ultimate. Respond in Thai. No tools or device access.'},
                {'role': 'user', 'content': message}
            ]}).encode()
            req = Request(OLLAMA, data=upstream, headers={'Content-Type': 'application/json'}, method='POST')
            with urlopen(req, timeout=120) as response:
                raw = response.read(MAX_RESPONSE + 1)
            if len(raw) > MAX_RESPONSE:
                return self.reply(502, {'error': 'upstream_too_large'})
            data = json.loads(raw)
            answer = data['message']['content']
            if not isinstance(answer, str) or not answer.strip():
                raise ValueError('empty response')
            return self.reply(200, {'reply': answer})
        except (URLError, HTTPError, TimeoutError, OSError, ValueError, KeyError, TypeError):
            return self.reply(502, {'error': 'upstream_unavailable'})
        finally:
            SEMAPHORE.release()

    def log_message(self, *_):
        pass

if __name__ == '__main__':
    if len(TOKEN) < 32:
        raise SystemExit('ZEAZ_GATEWAY_TOKEN must contain at least 32 characters')
    server = ThreadingHTTPServer(('127.0.0.1', 8765), Handler)
    print('ZEAZ gateway listening on 127.0.0.1:8765', flush=True)
    server.serve_forever()
