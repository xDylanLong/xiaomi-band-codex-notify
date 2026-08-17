---
name: connect
description: Connect the local Codex session to the 小米手环Codex通知 Android app over LAN. Use when the user says “连接我的小米手环”, pastes pairing information from the app, wants to test the band notification bridge, or asks to enable Codex completion notifications.
---

# Connect 小米手环Codex通知

Use this skill to configure the bundled Codex Stop hook without asking the user to install Node, edit Codex config files, or start a bridge process manually.

## Workflow

1. Ask the user to open the `小米手环Codex通知` Android app and tap `复制 Codex 配对信息`.
2. Ask them to paste the copied block into the chat. The block contains a LAN host, port, and short-lived pairing code, not the Bearer token. Treat the pairing code as a secret and never repeat it in your response or logs.
3. Run the bundled pairing helper with the exact pasted text:

```bash
python3 "${PLUGIN_ROOT}/scripts/pair.py" --text '<pasted pairing block>' --test
```

If `${PLUGIN_ROOT}` is unavailable in the shell, resolve the directory containing this plugin and run its `scripts/pair.py` directly. The helper exchanges the pairing code with the phone and stores the returned token in private plugin data. Do not use the repository's legacy Node installer for the normal flow.

4. Report only whether the bridge health check and test notification succeeded. If it fails, say that the computer and phone must be on the same Wi-Fi and ask the user to copy a fresh pairing block.
5. Tell the user to open `/hooks` once, review and trust the hook named `发送小米手环通知`. Do not claim automatic notifications are active until this trust step is complete.

## Existing configuration

The helper stores the private configuration in the plugin's writable `PLUGIN_DATA` directory. If the user changes phone IP or regenerates the app token, repeat the pairing workflow. Do not create or edit `~/.codex/hooks.json` for this plugin.

## Manual notifications

For a user request such as “给手环发一条消息”, use the existing bridge CLI only if the user is a developer and explicitly asks for a manual command. Normal users should use the paired Plugin and the Stop hook.

## Safety

- Do not reveal, quote, commit, or persist the token outside the plugin data directory.
- Do not enable the optional phone notification listener unless the user asks for it; it can duplicate notifications already sent by the Stop hook.
- Never make network access outside the phone's LAN address and port from the pairing block.
