# 小米手环Codex通知 产品化设计

## 目标

把现有 LAN bridge 产品化为一个可公开分享的 Android companion：安装手机 App、完成一次极简配置后，Codex 每轮任务结束都能通过局域网把摘要发送到手机，再由 Mi Fitness 同步到小米手环。

产品名统一为「小米手环Codex通知」。仓库同时保留手动通知、运动计划和图片通知能力，但首屏只突出 Codex 完成通知。

## 能力边界

- Android App 不能凭空读取电脑上的 Codex 状态，因此“只安装手机 App”不足以触发电脑任务通知。
- Codex 当前提供 `Stop` 生命周期 hook；电脑端需要一次性安装本仓库提供的 hook。之后每个 Codex turn 结束会自动 POST 到手机 App。
- 通讯只走局域网 HTTP，默认端口 `8787`，不使用云服务；token 只保存在手机和电脑本地配置中。
- App 可选启用 Android Notification Listener，用于转发手机上已出现的 Codex/ChatGPT 相关通知；它不是电脑 Codex hook 的替代品。

## 架构

```text
Codex Stop hook
      │ stdin JSON: last_assistant_message
      ▼
电脑端 hook sender ── HTTP POST /v1/notify ──► Android App
                                                    │
                                                    ├─ Android notification
                                                    └─ Mi Fitness App 通知同步
                                                              ▼
                                                            手环
```

组件职责：

1. `android-companion`：Android App、onboarding、前台 LAN bridge、通知发布和可选通知监听。
2. `bridge/bandctl.mjs`：手动健康检查、通知、运动计划和图片通知。
3. `codex/stop-hook.mjs`：读取 Codex hook stdin，提取任务结束摘要并发送通知；失败时静默退出，不能阻塞 Codex。
4. `codex/install-hook.mjs`：保存本地手机地址/token，合并 `~/.codex/hooks.json`，不覆盖其他 hooks。
5. `assets/logo.png`：产品 logo、README 头像和 Android launcher icon 来源。

## Onboarding

首次打开只展示三件事：

1. 允许本 App 通知；
2. 按钮打开 Android 通知监听设置（可跳过）；
3. 启动 LAN bridge，并展示手机 IP、token 和电脑端一键安装命令。

配置完成后显示一个简短状态卡：`LAN bridge 运行中`、端口、复制 token、打开通知设置、重新查看安装命令。高级能力放在折叠区域，不进入首屏。

## 安全与兼容

- 所有 bridge 请求使用 `Authorization: Bearer <token>`。
- App 只接受局域网明示地址，不提供公网穿透。
- Codex hook 本身不把 token 写入仓库；安装器将配置保存到用户目录，并限制为仅用户可读写。
- 不修改现有 Java package/applicationId，保证当前安装和 Mi Fitness 配置可以继续使用。
- Hook 使用 `async: true`，避免网络不可达时拖慢 Codex。

## 验收标准

- 新 APK 的应用名、通知标题和 launcher icon 为「小米手环Codex通知」。
- 首次启动可完成通知权限、可选通知监听和 LAN bridge 三步配置。
- `node codex/install-hook.mjs --host ... --token ...` 可幂等写入用户 hooks 配置。
- 用模拟 Stop hook stdin 能得到一次 `/v1/notify` 请求，重复 Stop continuation 不重复发送。
- Android bridge 原有 `health`、`notify`、`plan`、图片通知能力不回退。
- 公开 README 说明真实安装流程、LAN 限制、Codex 版本/hook 信任步骤和排障方式。
