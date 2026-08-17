# 小米手环Codex通知零 Node Plugin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将小米手环Codex通知改造成 Android App 自动启动 bridge、Codex Plugin 自动发送 Stop 通知、用户无需接触 Node 的公开产品。

**Architecture:** Android App 负责局域网 bridge 生命周期和一次性配对信息；Codex Plugin 负责 Skill 配置与 Python Stop hook。Hook 从插件的 `PLUGIN_DATA` 读取私有配置，向手机 `8787` 端口发送通知。仓库 marketplace 负责公开发现和安装。

**Tech Stack:** Java Android SDK 35、Python 3 标准库、Codex Plugin manifest、Codex hooks.json、Markdown、GitHub Releases。

## Global Constraints

- 产品名固定为 `小米手环Codex通知`。
- 电脑和手机必须在同一局域网；不增加云服务。
- 普通用户流程不要求 Node、手动启动 bridge 或编辑 Codex 配置文件。
- Hook 失败必须静默退出 0，不能阻塞 Codex。
- token 不得进入 Git、README、测试 fixture 或日志。
- 保留现有手动 bridge CLI 作为开发者兼容入口。

---

### Task 1: 创建 Codex Plugin 和仓库 marketplace

**Files:**
- Create: `plugins/xiaomi-band-codex-notify/.codex-plugin/plugin.json`
- Create: `plugins/xiaomi-band-codex-notify/hooks/hooks.json`
- Create: `plugins/xiaomi-band-codex-notify/skills/connect/SKILL.md`
- Create: `plugins/xiaomi-band-codex-notify/assets/logo.png`
- Create: `.agents/plugins/marketplace.json`
- Test: `plugins/xiaomi-band-codex-notify/.codex-plugin/plugin.json` with plugin validator

**Interfaces:**
- Produces plugin name `xiaomi-band-codex-notify`, version `0.3.0`, default hook path `./hooks/hooks.json`, skill path `./skills/`, and marketplace source path `./plugins/xiaomi-band-codex-notify`.

- [ ] **Step 1: Scaffold the plugin directory**

Run the plugin creator scaffold in the repository plugin directory with skills, hooks, and assets enabled, then remove generated placeholders.

- [ ] **Step 2: Write the manifest and marketplace entry**

Use the manifest paths `"skills": "./skills/"`, `"hooks": "./hooks/hooks.json"`, and install-surface metadata pointing to the public GitHub repository and logo.

- [ ] **Step 3: Write the connect skill**

The skill must trigger on “连接我的小米手环”, instruct Codex to accept a pasted `小米手环Codex通知配对信息` block, call the bundled setup helper or write the plugin data config, verify `/v1/health`, send one test notification, and tell the user to open `/hooks` once.

- [ ] **Step 4: Validate the plugin package**

Run the Codex Plugin Creator validator against `plugins/xiaomi-band-codex-notify` and confirm there are no manifest placeholders or unsupported fields.

### Task 2: Implement the zero-Node Python hook and pairing helper

**Files:**
- Create: `plugins/xiaomi-band-codex-notify/hooks/stop.py`
- Create: `plugins/xiaomi-band-codex-notify/scripts/pair.py`
- Create: `plugins/xiaomi-band-codex-notify/tests/test_hook.py`
- Modify: `plugins/xiaomi-band-codex-notify/hooks/hooks.json`

**Interfaces:**
- `stop.py` reads JSON stdin and environment variables `PLUGIN_DATA` and optional `XIAOMI_BAND_CODEX_CONFIG`; it posts JSON to `http://<host>:<port>/v1/notify`.
- `pair.py` accepts a pairing text from stdin or `--text`, exchanges the App pairing code through `POST /v1/pair`, writes the returned token to `PLUGIN_DATA/config.json` with private permissions, verifies `GET /v1/health`, and supports `--test` to send a test notification.

  - [ ] **Step 1: Write failing Python tests**

Cover Stop filtering, `stop_hook_active`, body truncation, pairing-code parsing, token exchange, config permission, and silent network failure.

- [ ] **Step 2: Run the tests and verify they fail**

Run `python3 -m unittest discover -s plugins/xiaomi-band-codex-notify/tests -v`; expect failures because the hook and pairing modules do not exist.

- [ ] **Step 3: Implement minimal hook and pairing helper**

Use only Python standard library modules (`json`, `os`, `pathlib`, `urllib.request`, `urllib.error`, `sys`). Never print the token or full request headers.

- [ ] **Step 4: Add the plugin hook configuration**

Set the Stop command to `python3 "${PLUGIN_ROOT}/hooks/stop.py"`, `async: true`, `timeout: 5`, and the Chinese status message.

- [ ] **Step 5: Run Python tests and a mock HTTP integration**

Run the unittest suite and a local mock HTTP server test that asserts Bearer authentication, payload title/body, and successful exit code.

### Task 3: Simplify Android App onboarding and bridge lifecycle

**Files:**
- Modify: `android-companion/app/src/main/java/com/example/bandbridge/MainActivity.java`
- Modify: `android-companion/app/src/main/java/com/example/bandbridge/BridgeService.java`
- Modify: `android-companion/app/src/main/res/values/strings.xml`
- Modify: `android-companion/app/src/main/AndroidManifest.xml`
- Modify: `android-companion/app/build.gradle.kts`
- Modify: `android-companion/build-local.sh`

**Interfaces:**
- Activity launch calls `startBridge()` using the stored token and shows only status, pairing copy, test notification, notification permission, and Mi Fitness settings.
- Existing `/v1/health` and `/v1/notify` protocol remains unchanged.

- [ ] **Step 1: Add a regression test target for auto-start behavior**

Document the expected Android behavior in the build/check script and preserve the existing protocol tests as the network-level regression.

- [ ] **Step 2: Implement auto-start and simplified layout**

Remove the mandatory start/stop controls from the primary layout; auto-start on `onCreate`, keep stop only under an advanced section if needed, and add copy pairing info plus send-test action.

- [ ] **Step 3: Keep notification permissions explicit**

Retain App notification settings and Mi Fitness instructions; move Notification Listener to an advanced option with a duplicate-notification warning.

- [ ] **Step 4: Rebuild and inspect the APK**

Build with the bundled JDK 17 and Android SDK 35, then verify application label, icon, version `0.3.0`, and APK v2/v3 signatures.

### Task 4: Rewrite public README and usage flow

**Files:**
- Modify: `README.md`
- Modify: `docs/usage.md`
- Modify: `android-companion/README.md`
- Modify: `codex/install-hook.mjs`
- Modify: `codex/stop-hook.mjs`
- Modify: `codex/test-hook.mjs`

**Interfaces:**
- Public documentation leads with APK download → Codex `/plugins` marketplace installation → App pairing → “连接我的小米手环”.
- Legacy Node hook scripts are labeled developer compatibility and no longer appear in the normal user path.

- [ ] **Step 1: Replace normal-user instructions**

Remove manual bridge start and Node command from the primary flow; add the plugin marketplace command/desktop UI path and pairing prompt.

- [ ] **Step 2: Update legacy compatibility documentation**

Keep the old Node hook and CLI documented under an advanced/developer section only.

- [ ] **Step 3: Run copy and secret sweeps**

Search for old product names, absolute local paths, the sample token, and normal-flow Node instructions; fix every public occurrence.

### Task 5: Validate, commit, publish v0.3.0

**Files:**
- Modify: `README.md`, plugin package, Android sources, and release metadata as required by prior tasks.

- [ ] **Step 1: Run all checks**

Run plugin validation, Python unit/integration tests, existing Node protocol tests, JS syntax checks, Android build, `aapt dump badging`, `apksigner verify`, and `git diff --check`.

- [ ] **Step 2: Inspect staged scope and secrets**

Run `git status -sb`, `git diff --stat`, and a staged-text scan for tokens, local absolute paths, and unrelated changes.

- [ ] **Step 3: Commit the implementation**

Commit with `feat: add zero-node Codex plugin onboarding`.

- [ ] **Step 4: Push and publish**

Push to the public repository, create/update the `v0.3.0` Release with an ASCII APK asset name, and verify the public download URL.
