# 小米手环Codex通知零 Node Plugin 设计

## 目标

把当前“安装 APK + 手动启动 bridge + 执行 Node 安装脚本 + 手动配置 hook”的流程收敛为：安装 Android App、安装 Codex Plugin、完成一次配对，之后 Codex 任务完成自动通知手环。

## 已确认的产品边界

- 产品名固定为 `小米手环Codex通知`。
- 电脑与手机只通过同一局域网通信，不引入云服务。
- 用户不需要接触 Node、编辑 `~/.codex/hooks.json` 或手动启动 bridge。
- Codex 的非托管 Plugin hook 仍需用户在 `/hooks` 中审核并信任一次，这是 Codex 的安全边界。
- 微信语音回复不在本次范围内。

## 方案

### Android App

- Activity 打开时自动启动 `BridgeService`，并保留服务状态提示。
- 主界面只保留：bridge 状态、复制配对信息、发送测试通知、打开 Mi Fitness 通知设置。
- 配对信息是可粘贴到 Codex 的一段文本，包含局域网地址和一次性配对码；App 不把 Bearer token 放进剪贴板。
- Plugin 通过局域网 `/v1/pair` 用配对码换取 token，再把 token 写入插件私有数据目录。
- 现有的可选手机通知监听继续保留，但放入“高级选项”，避免和 Codex Plugin 产生重复通知。

### Codex Plugin

插件目录为 `plugins/xiaomi-band-codex-notify/`，包含：

- `.codex-plugin/plugin.json`：插件元数据、skill 路径和 hook 路径。
- `hooks/hooks.json`：异步 `Stop` hook，命令使用系统 Python，不依赖 Node。
- `hooks/stop.py`：读取 Codex Stop JSON，读取 `PLUGIN_DATA/config.json`，向 Android bridge 发送摘要通知；失败静默，不阻塞 Codex。
- `skills/connect/SKILL.md`：当用户说“连接我的小米手环”或粘贴配对信息时，指导 Codex 验证 bridge、写入 `PLUGIN_DATA/config.json`、发送测试通知，并提示 `/hooks` 审核。
- `assets/logo.png`：复用产品 logo。

### Marketplace 与文档

- 仓库增加 `.agents/plugins/marketplace.json`，让 Codex 桌面端和 CLI 可以从本仓库发现插件。
- README 主流程改为 APK → `/plugins` → “连接我的小米手环”，Node 安装脚本降级为开发者兼容入口。
- `docs/usage.md` 只保留普通用户必须步骤，并明确 bridge 自动启动和可选高级监听。

## 数据流

```text
Android App 打开
  → 自动启动 BridgeService :8787
  → App 复制配对文本
  → Codex Plugin Skill 验证 /v1/health 并保存 PLUGIN_DATA/config.json
  → Codex Stop hook 读取 last_assistant_message
  → POST /v1/notify
  → Android 通知
  → Mi Fitness
  → 小米手环
```

## 失败处理与安全

- 配对校验失败时，Skill 只报告手机地址、端口或 token 不匹配，不打印完整 token。
- Hook 设置约 2.5 秒超时，网络失败直接退出 0，不能阻塞 Codex。
- 配置文件写入时使用 `0600`（系统不支持时保持用户私有目录权限）。
- `/v1/pair` 不要求 Bearer token，但必须提供 App 生成的配对码；普通 `/v1/health` 和 `/v1/notify` 仍要求 Bearer token。
- Hook 只接受 `hook_event_name=Stop` 且 `stop_hook_active` 不为 true 的事件，避免重复发送。
- App 仍要求用户显式允许通知；Mi Fitness 的 App 通知开关仍由用户在系统中确认。

## 验收标准

1. 新用户不运行 Node、不手动启动 bridge，也能按 README 完成安装与配对。
2. Plugin manifest、marketplace、skill 和 hook 可通过 Codex/plugin 校验。
3. Python hook 能在 mock bridge 上发送摘要，并在 bridge 不可达时静默退出。
4. Android APK 启动 Activity 后自动启动 bridge，现有手动通知和通知转发功能不回归。
5. README、使用说明、版本号和 Release 指向新的零 Node 流程。
