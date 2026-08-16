# 小米手环 10 Pro 全能力方案 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a native Android notification companion and a dependency-free desktop CLI to the existing static content editor.

**Architecture:** The Android app owns a token-protected LAN HTTP server and publishes Android notifications. The CLI sends text, plan, and optional image payloads to that server. Mi Fitness remains the device transport, avoiding private BLE/auth reverse engineering in the stable path.

**Tech Stack:** Android Java + Android SDK standard APIs, Node.js 20+ built-in `fetch`, existing HTML/CSS/Canvas editor.

## Global Constraints

- Android companion targets API 26+ and uses no third-party runtime dependency.
- Bridge server listens on port 8787 and requires `Authorization: Bearer <token>`.
- Maximum HTTP request body is 4 MiB.
- Do not claim the band can reply to third-party notifications or send WeChat voice.
- Do not bind the service to a public address or add cloud relay.

---

### Task 1: Define the bridge protocol and CLI

**Files:**
- Create: `bridge/package.json`
- Create: `bridge/bandctl.mjs`
- Create: `bridge/README.md`
- Create: `bridge/protocol.md`

- [ ] Implement argument parsing for `health`, `notify`, and `plan`.
- [ ] Implement Bearer-authenticated JSON POST/GET requests with native `fetch`.
- [ ] Support optional local image encoding with a 3 MiB CLI guard.
- [ ] Return non-zero exit codes and readable errors for 401/400/413/network failures.
- [ ] Document Codex/script usage and the LAN security model.

### Task 2: Scaffold the Android companion

**Files:**
- Create: `android-companion/settings.gradle.kts`
- Create: `android-companion/build.gradle.kts`
- Create: `android-companion/gradle.properties`
- Create: `android-companion/app/build.gradle.kts`
- Create: `android-companion/app/src/main/AndroidManifest.xml`
- Create: `android-companion/app/src/main/java/com/example/bandbridge/MainActivity.java`
- Create: `android-companion/app/src/main/java/com/example/bandbridge/BridgeService.java`
- Create: `android-companion/app/src/main/java/com/example/bandbridge/BridgeHttpServer.java`
- Create: `android-companion/app/src/main/java/com/example/bandbridge/NotificationPublisher.java`
- Create: `android-companion/README.md`

- [ ] Create a standard Android application using Java and API 26+.
- [ ] Build the minimal activity UI to show the token, LAN addresses, start/stop service, request notification permission, and open app notification settings.
- [ ] Implement the foreground service and raw ServerSocket HTTP parser.
- [ ] Implement health, notify, and plan routes with token and request-size validation.
- [ ] Publish text and optional BigPicture Android notifications.
- [ ] Document Mi Fitness notification configuration and the real-device verification step.

### Task 3: Connect the content editor and research docs

**Files:**
- Modify: `README.md`
- Modify: `docs/research/xiaomi-band-10-pro-watchface-capabilities.md`

- [ ] Document the complete desktop → Android → Mi Fitness → band path.
- [ ] Link the CLI protocol and Android project.
- [ ] Preserve the existing static plan-card workflow and its limitation.

### Task 4: Verify and hand off

**Files:**
- Create: `bridge/test-protocol.mjs`

- [ ] Add a local mock HTTP server test for CLI health, notify, auth headers, and plan payloads.
- [ ] Run `node --check bridge/bandctl.mjs` and `node --check bridge/test-protocol.mjs`.
- [ ] Run the protocol test.
- [ ] Attempt Android build only if an Android SDK/Gradle environment is present; otherwise report the missing toolchain explicitly.
- [ ] Commit the implementation and report the exact first-run commands.
