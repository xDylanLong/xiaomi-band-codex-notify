# Band 10 Pro Desktop Bridge

This CLI sends notifications from a Mac/PC to the Android companion over the local network. The Android app then posts a normal Android notification; Mi Fitness can mirror that app's notifications to the band.

## Usage

```bash
export BAND_BRIDGE_HOST=192.168.1.23
export BAND_BRIDGE_TOKEN='token shown by the Android app'

node bridge/bandctl.mjs health --json
node bridge/bandctl.mjs notify --title 'Codex' --body '任务完成'
node bridge/bandctl.mjs plan \
  --date 2026-08-20 \
  --title '晨间跑步' \
  --duration 20 \
  --target '慢跑 3 公里' \
  --note '完成后拉伸'
node bridge/bandctl.mjs notify --title '截图' --body '查看图片' --image ./plan.png
```

The CLI has no account login, cloud service, or npm runtime dependency. It only sends data to the explicit LAN host and token supplied by you.

## Codex integration

Any local script can call the CLI after a task completes:

```bash
node bridge/bandctl.mjs notify \
  --source codex \
  --title 'Codex task finished' \
  --body 'Review the generated files'
```

This is a command-level integration point, not an automatic hook into the Codex desktop app. An automatic Codex hook can be added later by calling the same command from the user's local task runner.
