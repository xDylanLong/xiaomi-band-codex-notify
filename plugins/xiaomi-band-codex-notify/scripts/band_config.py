import json
import os
import re
import urllib.request
from pathlib import Path


MAX_BODY_LENGTH = 1200
DEFAULT_PORT = 8787


def build_notification(event):
    if not isinstance(event, dict):
        return None
    if event.get("hook_event_name") != "Stop" or event.get("stop_hook_active") is True:
        return None
    summary = str(event.get("last_assistant_message") or "任务已完成").rstrip()[:MAX_BODY_LENGTH]
    return {
        "type": "notify",
        "source": "codex",
        "title": "Codex 已完成",
        "body": summary or "任务已完成，可以回来看结果了",
    }


def parse_pairing(text):
    raw = str(text or "").strip()
    if not raw:
        raise ValueError("配对信息为空")
    try:
        value = json.loads(raw)
        if isinstance(value, dict):
            return normalize_pairing(value)
    except json.JSONDecodeError:
        pass

    values = {}
    for line in raw.splitlines():
        line = line.strip()
        if "=" in line:
            key, value = line.split("=", 1)
            values[key.strip().lower()] = value.strip()
        elif line.startswith(("http://", "https://")):
            match = re.match(r"https?://([^:/\s]+)(?::(\d+))?", line)
            if match:
                values["host"] = match.group(1)
                values["port"] = match.group(2) or str(DEFAULT_PORT)
    return normalize_pairing(values)


def normalize_pairing(value):
    base = normalize_host_port(value)
    token = str(value.get("token") or "").strip()
    code = str(value.get("code") or value.get("pairing_code") or "").strip()
    if not token and not code:
        raise ValueError("配对信息缺少配对码")
    if token:
        base["token"] = token
    else:
        base["code"] = code
    return base


def normalize_config(value):
    base = normalize_host_port(value)
    token = str(value.get("token") or "").strip()
    if not token:
        raise ValueError("配置缺少 token")
    base["token"] = token
    return base


def normalize_host_port(value):
    host = str(value.get("host") or value.get("ip") or "").strip()
    host = re.sub(r"^https?://", "", host).split("/", 1)[0].split(":", 1)[0]
    port = str(value.get("port") or DEFAULT_PORT).strip()
    if not host or not re.fullmatch(r"\d{1,5}", port):
        raise ValueError("配对信息缺少手机地址或端口")
    if not 1 <= int(port) <= 65535:
        raise ValueError("端口无效")
    return {"host": host, "port": int(port)}


def config_path(environ=None):
    env = environ or os.environ
    explicit = env.get("XIAOMI_BAND_CODEX_CONFIG")
    if explicit:
        return Path(explicit).expanduser()
    plugin_data = env.get("PLUGIN_DATA")
    if plugin_data:
        return Path(plugin_data).expanduser() / "config.json"
    return Path.home() / ".config" / "xiaomi-band-codex-notify" / "config.json"


def config_candidates(environ=None):
    env = environ or os.environ
    candidates = []
    explicit = env.get("XIAOMI_BAND_CODEX_CONFIG")
    plugin_data = env.get("PLUGIN_DATA")
    if explicit:
        candidates.append(Path(explicit).expanduser())
    if plugin_data:
        candidates.append(Path(plugin_data).expanduser() / "config.json")
    candidates.append(Path.home() / ".config" / "xiaomi-band-codex-notify" / "config.json")
    unique = []
    for path in candidates:
        if path not in unique:
            unique.append(path)
    return unique


def write_config(config, environ=None):
    path = config_path(environ)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(config, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    try:
        path.chmod(0o600)
    except OSError:
        pass
    return path


def read_config(environ=None):
    for path in config_candidates(environ):
        try:
            return normalize_config(json.loads(path.read_text(encoding="utf-8")))
        except FileNotFoundError:
            continue
    raise FileNotFoundError("没有找到小米手环配对配置")


def request(config, method, path, payload=None, timeout=2.5, authenticate=True):
    url = f"http://{config['host']}:{config['port']}{path}"
    body = None if payload is None else json.dumps(payload, ensure_ascii=False).encode("utf-8")
    request_obj = urllib.request.Request(url, data=body, method=method)
    if authenticate:
        request_obj.add_header("Authorization", f"Bearer {config['token']}")
    if body is not None:
        request_obj.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(request_obj, timeout=timeout) as response:
        return response.status, response.read()


def send_notification(config, payload):
    status, _ = request(config, "POST", "/v1/notify", payload)
    return status in (200, 202)
