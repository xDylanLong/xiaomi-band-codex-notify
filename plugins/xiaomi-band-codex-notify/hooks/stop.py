#!/usr/bin/env python3
import json
import sys
from pathlib import Path


sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))
from band_config import build_notification, read_config, send_notification


def main():
    try:
        event = json.load(sys.stdin)
        payload = build_notification(event)
        if payload is None:
            return 0
        config = read_config()
        send_notification(config, payload)
    except Exception:
        # Notification delivery must never change Codex's task result.
        pass
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
