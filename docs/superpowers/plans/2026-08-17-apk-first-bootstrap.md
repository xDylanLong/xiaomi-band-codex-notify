# 小米手环 Codex 通知 APK-first Bootstrap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让用户先安装并打开 APK，再通过一段中文引导语让 Codex 自动安装 Plugin、询问 4 位匹配码并完成全部初始化。

**Architecture:** 保留 Android App 现有的自动生成匹配码、自动启动 bridge 和 UDP 发现能力。Plugin 增加一个 Bootstrap Skill 作为入口；它负责识别 GitHub 仓库初始化请求、完成 Plugin 安装引导并把用户交给配对 Skill。配对 Skill 只询问 4 位匹配码，使用现有 Python helper 完成局域网发现、token 交换、健康检查和测试通知。

**Tech Stack:** Codex Plugin manifest、Markdown Skills、Python 3 标准库、Codex CLI marketplace commands、GitHub public marketplace。

## Global Constraints

- 第一步必须是安装并打开 Android APK；手机端负责本地初始化。
- 普通用户不需要输入 `/plugins`、`/hooks`、Node 命令、手机 IP 或 token。
- 4 位匹配码只用于可信局域网配对；token 只能写入 Plugin 私有配置。
- 保留现有 `connect` Skill、`pair.py`、Stop Hook 和 APK v0.3.1 的兼容行为。
- Plugin 版本升级到 `0.3.3`；无需重新构建 APK。

---

### Task 1: Add the Bootstrap Skill entry point

**Files:**
- Create: `plugins/xiaomi-band-codex-notify/skills/bootstrap/SKILL.md`
- Create: `plugins/xiaomi-band-codex-notify/skills/bootstrap/agents/openai.yaml`
- Modify: `plugins/xiaomi-band-codex-notify/.codex-plugin/plugin.json`

**Interfaces:**
- Consumes: an initialization sentence containing the public GitHub URL and no pairing code.
- Produces: a user-facing request for exactly the 4-digit code, then delegates pairing to `scripts/pair.py` or the existing `connect` Skill.

- [x] **Step 1: Write the Bootstrap Skill instructions**

The Skill must instruct Codex to:

```text
1. Recognize https://github.com/xDylanLong/xiaomi-band-codex-notify as the product source.
2. If the Plugin is not installed, add the public marketplace and install xiaomi-band-codex-notify through Codex's own plugin commands; do not ask the user to type those commands.
3. After installation, ask only: “请输入手机 App 中显示的 4 位匹配码。”
4. On the next user message, validate exactly four digits and run the bundled pairing helper with --code <code> --test.
5. Report initialization and test status without printing token, IP, or command output.
```

The Skill must explain that if the current Codex build requires a new session after Plugin installation, it should give a short “请重新打开 Codex 会话，然后继续回复 4 位匹配码” prompt rather than exposing CLI details.

- [x] **Step 2: Add UI metadata**

Use this metadata:

```yaml
interface:
  display_name: "初始化小米手环 Codex 通知"
  short_description: "安装 Skill 后自动询问 4 位匹配码并完成配对"
  default_prompt: "请根据 GitHub 仓库安装并初始化小米手环 Codex 通知，安装完成后询问我的 4 位匹配码。"
  brand_color: "#1877F2"
```

- [x] **Step 3: Bump plugin version**

Change `.codex-plugin/plugin.json` from `0.3.2` to `0.3.3` and keep both `./skills/` and the default `./hooks/hooks.json` discovery behavior.

- [x] **Step 4: Validate metadata**

Run a JSON parse for `plugin.json`, assert the Bootstrap Skill and its `agents/openai.yaml` exist, and run `git diff --check`.

### Task 2: Make pairing a strict second phase

**Files:**
- Modify: `plugins/xiaomi-band-codex-notify/skills/connect/SKILL.md`
- Modify: `plugins/xiaomi-band-codex-notify/scripts/pair.py`
- Test: `plugins/xiaomi-band-codex-notify/tests/test_hook.py`

**Interfaces:**
- Consumes: exactly one 4-digit user response after Bootstrap asks for it.
- Produces: `python3 .../pair.py --code <code> --test` result and private token configuration.

- [x] **Step 1: Add strict code-validation coverage**

Add tests that `parse_pairing("code=1234")` succeeds, `parse_pairing("code=123")` fails, and the existing UDP discovery test remains green.

- [x] **Step 2: Update connect Skill wording**

Make `connect` behave as the second phase: if no code is present, ask only for the 4-digit code; never ask for repository text, IP, token, `/hooks`, or Node commands.

- [x] **Step 3: Run the Python tests**

Run:

```bash
python3 -m unittest discover -s plugins/xiaomi-band-codex-notify/tests -v
```

Expected: all tests pass.

### Task 3: Rewrite the public onboarding copy

**Files:**
- Modify: `README.md`
- Modify: `docs/usage.md`
- Modify: `docs/superpowers/specs/2026-08-17-apk-first-bootstrap-design.md`

**Interfaces:**
- Produces: one public copyable initialization sentence whose first prerequisite is APK installation.

- [x] **Step 1: Put APK installation first**

The top README flow must be:

```text
1. 下载并打开 APK，允许通知，打开 Mi Fitness 通知同步。
2. 复制 Bootstrap 引导语到 Codex。
3. 等待 Skill 安装完成并询问 4 位匹配码。
4. 只回复 4 位数字。
5. 后续只需要打开手机 App 并正常使用 Codex。
```

- [x] **Step 2: Add the exact copyable sentence**

Use this sentence in README and the full guide:

```text
请根据 GitHub 仓库 https://github.com/xDylanLong/xiaomi-band-codex-notify 安装并启用“小米手环 Codex 通知”Skill。安装完成后，请只询问我手机 App 中显示的 4 位匹配码，不要让我执行命令、复制 token 或输入手机 IP。
```

- [x] **Step 3: Remove misleading manual instructions**

Keep developer CLI details in the advanced section only. The normal flow must not tell users to open `/plugins`, `/hooks`, start Node, copy pairing text, or manually enter IP/token.

### Task 4: Verify and publish

**Files:**
- Modify: `plugins/xiaomi-band-codex-notify/.codex-plugin/plugin.json`
- Modify: public README and Skill files as needed after validation.

- [x] **Step 1: Run validation**

Run:

```bash
python3 -m unittest discover -s plugins/xiaomi-band-codex-notify/tests -v
python3 -m py_compile plugins/xiaomi-band-codex-notify/scripts/*.py plugins/xiaomi-band-codex-notify/hooks/*.py
git diff --check
```

- [x] **Step 2: Refresh the local marketplace**

Run `codex plugin marketplace upgrade personal` and verify `xiaomi-band-codex-notify@personal` reports version `0.3.3`.

- [x] **Step 3: Commit and push**

Use commit message `feat: add apk-first bootstrap onboarding`, push `main`, and verify the remote README contains the APK-first flow and the copyable sentence.
