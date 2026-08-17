import http.server
import json
import socketserver
import sys
import tempfile
import threading
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from band_config import build_notification, config_path, parse_pairing, read_config, request, send_notification, write_config


class Handler(http.server.BaseHTTPRequestHandler):
    requests = []

    def do_POST(self):
        length = int(self.headers.get("Content-Length", "0"))
        payload = json.loads(self.rfile.read(length))
        Handler.requests.append((self.path, self.headers.get("Authorization"), payload))
        self.send_response(200 if self.path == "/v1/pair" else 202)
        self.end_headers()
        if self.path == "/v1/pair":
            self.wfile.write(b'{"ok":true,"token":"secret"}')

    def log_message(self, *_):
        pass


class HookTests(unittest.TestCase):
    def test_build_notification_filters_and_truncates(self):
        self.assertIsNone(build_notification({"hook_event_name": "UserPromptSubmit"}))
        self.assertIsNone(build_notification({"hook_event_name": "Stop", "stop_hook_active": True}))
        payload = build_notification({"hook_event_name": "Stop", "last_assistant_message": "完成\n\n"})
        self.assertEqual(payload["body"], "完成")
        long_payload = build_notification({"hook_event_name": "Stop", "last_assistant_message": "x" * 2000})
        self.assertEqual(len(long_payload["body"]), 1200)

    def test_pairing_and_private_config(self):
        pairing = parse_pairing("小米手环Codex通知配对信息\nhost=127.0.0.1\nport=8787\ncode=pair-code")
        self.assertEqual(pairing["host"], "127.0.0.1")
        self.assertEqual(pairing["code"], "pair-code")
        config = {"host": "127.0.0.1", "port": 8787, "token": "secret"}
        with tempfile.TemporaryDirectory() as directory:
            env = {"PLUGIN_DATA": directory}
            path = write_config(config, env)
            self.assertEqual(read_config(env), config)
            self.assertEqual(path.stat().st_mode & 0o777, 0o600)
            self.assertEqual(config_path(env), Path(directory) / "config.json")

    def test_http_request(self):
        Handler.requests = []
        with socketserver.TCPServer(("127.0.0.1", 0), Handler) as server:
            thread = threading.Thread(target=server.serve_forever, daemon=True)
            thread.start()
            port = server.server_address[1]
            pairing = {"host": "127.0.0.1", "port": port, "code": "pair-code"}
            status, response = request(pairing, "POST", "/v1/pair", {"code": "pair-code"}, authenticate=False)
            self.assertEqual(status, 200)
            self.assertEqual(json.loads(response)["token"], "secret")
            config = {"host": "127.0.0.1", "port": port, "token": "secret"}
            self.assertTrue(send_notification(config, {"title": "Codex", "body": "完成"}))
            server.shutdown()
        self.assertEqual(Handler.requests[1][0], "/v1/notify")
        self.assertEqual(Handler.requests[1][1], "Bearer secret")
        self.assertEqual(Handler.requests[1][2]["body"], "完成")


if __name__ == "__main__":
    unittest.main()
