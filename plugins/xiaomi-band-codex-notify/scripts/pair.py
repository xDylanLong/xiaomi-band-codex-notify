#!/usr/bin/env python3
import argparse
import sys

import json

from band_config import parse_pairing, request, send_notification, write_config


def main(argv=None):
    parser = argparse.ArgumentParser(description="Pair 小米手环Codex通知 with Codex")
    parser.add_argument("--text", help="Pairing block copied from the Android app")
    parser.add_argument("--test", action="store_true", help="Send a test notification after pairing")
    args = parser.parse_args(argv)
    text = args.text if args.text is not None else sys.stdin.read()
    try:
        pairing = parse_pairing(text)
        if "token" in pairing:
            config = pairing
        else:
            status, response = request(pairing, "POST", "/v1/pair", {"code": pairing["code"]}, authenticate=False)
            if status != 200:
                raise RuntimeError("配对码无效")
            token = json.loads(response.decode("utf-8")).get("token", "")
            if not token:
                raise RuntimeError("手机没有返回 token")
            config = {"host": pairing["host"], "port": pairing["port"], "token": token}
        status, _ = request(config, "GET", "/v1/health")
        if status != 200:
            raise RuntimeError("bridge health check failed")
        path = write_config(config)
        if args.test and not send_notification(config, {
            "type": "notify",
            "source": "codex-plugin",
            "title": "小米手环Codex通知",
            "body": "Codex 配对测试成功",
        }):
            raise RuntimeError("test notification failed")
        print(f"小米手环已连接，配置已保存到 {path}")
        print("请在 Codex 中执行 /hooks，审核并信任“发送小米手环通知”。")
        return 0
    except Exception as error:
        print(f"连接失败：{error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
