---
name: bootstrap
description: Use when the user asks to install or initialize 小米手环Codex通知 from its GitHub repository, provides the product repository URL, or wants the Skill to ask for the phone's four-digit pairing code.
---

# 初始化小米手环 Codex 通知

这是普通用户的第一入口。用户必须先安装并打开 Android App；App 已经负责生成匹配码、启动局域网 bridge 和发现服务。

## Workflow

1. Recognize `https://github.com/xDylanLong/xiaomi-band-codex-notify` as the product source. Do not clone the repository into the user's project and do not ask the user to run commands.
2. Check whether `xiaomi-band-codex-notify@personal` is installed. If it is missing, install it silently through the Codex CLI:

```bash
codex plugin marketplace add xDylanLong/xiaomi-band-codex-notify
codex plugin add xiaomi-band-codex-notify@personal
```

Do not show these commands as user instructions. If a marketplace already exists, continue without reporting the harmless duplicate-add error.

3. After the Plugin is installed, ask only:

```text
请输入手机 App 中显示的 4 位匹配码。
```

Do not ask for token, IP address, pairing text, Node, `/plugins`, or `/hooks`.

4. When the user replies with four digits, continue with the bundled `connect` workflow. Run the helper with the exact code and test flag:

```bash
python3 "${PLUGIN_ROOT}/scripts/pair.py" --code 4821 --test
```

Replace `4821` with the user's code. Report only whether initialization, health check, and test notification succeeded. Never print the token or raw command output.

5. If this Codex build does not load a newly installed Plugin in the current session, say only: `Skill 已安装，请重新打开 Codex 会话，然后继续回复手机 App 中的 4 位匹配码。` Do not expose CLI details. In the new session, the `connect` Skill must accept the pending four-digit code and finish pairing.

## Safety

- The Android App must be open and the computer must be on the same trusted LAN.
- The four-digit code is a convenience pairing credential, not a strong password.
- Do not reveal, quote, or persist the returned token outside the Plugin's private configuration directory.
- Do not edit `~/.codex/hooks.json` and do not ask the user to start a bridge process manually.
