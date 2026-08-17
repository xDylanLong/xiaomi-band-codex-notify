---
name: connect
description: Use when the user asks to initialize 小米手环Codex通知, invokes $connect, provides the GitHub repository URL, gives a four-digit pairing code after Bootstrap, or wants Codex completion notifications on a Xiaomi Band.
---

# Connect 小米手环Codex通知

Use this skill to configure the bundled Codex Stop hook without asking the user to install Node, edit Codex config files, or start a bridge process manually.

## Workflow

1. Extract the four-digit code from the user's initialization sentence. If it is missing, ask only for the code shown under `Codex 匹配码` in the Android app. Treat the pairing code as a secret and never repeat it in your response or logs.
2. If the sentence includes `https://github.com/xDylanLong/xiaomi-band-codex-notify`, recognize it as the product reference. Do not clone that repository or ask the user to run repository commands; use the bundled helper below.
3. Run the bundled pairing helper with the extracted four-digit code:

```bash
python3 "${PLUGIN_ROOT}/scripts/pair.py" --code 4821 --test
```

If `${PLUGIN_ROOT}` is unavailable in the shell, resolve the directory containing this plugin and run its `scripts/pair.py` directly. The helper discovers the phone over LAN, exchanges the pairing code for a token, and stores the returned token in private plugin data. Do not use the repository's legacy Node installer for the normal flow.

4. Report only whether initialization, bridge health check, and test notification succeeded. If it fails, say that the computer and phone must be on the same Wi-Fi and ask the user to enter a fresh four-digit code.
5. Do not ask the user to type `/hooks`. Tell them that Codex may show a one-time security prompt for `发送小米手环通知`; they should choose `允许` if it appears. If no prompt appears, continue normally. Do not claim automatic notifications are active until the hook is trusted.

## Existing configuration

The helper stores the private configuration in the plugin's writable `PLUGIN_DATA` directory when available, otherwise in the user's private `~/.config/xiaomi-band-codex-notify/` directory. If the user changes Wi-Fi or refreshes the app pairing code, repeat the pairing workflow. Do not create or edit `~/.codex/hooks.json` for this plugin.

## Manual notifications

For a user request such as “给手环发一条消息”, use the existing bridge CLI only if the user is a developer and explicitly asks for a manual command. Normal users should use the paired Plugin and the Stop hook.

## Safety

- Do not reveal, quote, commit, or persist the token outside the plugin data directory.
- The four-digit code is a convenience pairing credential for a trusted LAN, not a strong password. The Android app rate-limits successful discovery responses and lets the user refresh the code.
- Do not enable the optional phone notification listener unless the user asks for it; it can duplicate notifications already sent by the Stop hook.
- Never make network access outside the phone's LAN address discovered by the pairing helper.
