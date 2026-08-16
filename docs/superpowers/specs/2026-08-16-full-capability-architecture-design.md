# 小米手环 10 Pro 全能力方案设计

## 目标

把当前“表盘图片编辑器”升级为面向技术用户的三件套：Android 伴侣 App、电脑端 CLI/HTTP 协议、静态内容编辑器。重点是实现电脑/Codex 消息经手机通知链路到达 Xiaomi Smart Band 10 Pro，而不是继续扩大 Web App 的职责。

## 能力边界

- Xiaomi Smart Band 10 Pro 可以通过 Mi Fitness 同步手机第三方 App 通知；用户需要在 Mi Fitness 中选中伴侣 App 的通知。
- Android 伴侣可以发布标准 Android 通知，因此能够把来自电脑的标题、正文和可选图片交给 Mi Fitness 的通知同步链路。
- 电脑直连手环的私有 BLE/授权协议不作为第一版依赖；这样可以避免破解 Mi Fitness、保存小米账号或处理设备密钥。
- 10 Pro 对第三方通知官方只支持查看，不支持回复；微信语音回复不实现。
- 图片/飞书文档截图使用当前编辑器导出 PNG；Android 伴侣也支持把图片作为 BigPicture 通知发送，但手环是否显示图片由 Mi Fitness/固件决定。

## 架构

```text
Codex / script / desktop app
          │  HTTP POST /v1/notify
          ▼
macOS bridge CLI (bridge/bandctl.mjs)
          │  LAN + Bearer token
          ▼
Android companion (android-companion)
          │  Android NotificationManager
          ▼
Mi Fitness App notification sync
          │  Bluetooth proprietary transport
          ▼
Xiaomi Smart Band 10 Pro
```

### Android 伴侣

- 原生 Java，减少依赖；Android API 26+。
- 前台服务监听局域网 TCP 端口 8787。
- `POST /v1/notify`：发布文字或带图通知。
- `POST /v1/plan`：将运动计划格式化为通知。
- `GET /v1/health`：返回服务状态和协议版本。
- 所有写接口要求 `Authorization: Bearer <token>`。
- Token 只存本机 SharedPreferences，不上传服务器。

### CLI

- Node.js 20+ 内置 `fetch`，不引入 npm 依赖。
- `notify`、`plan`、`health` 三个命令。
- 图片只在用户明确提供 `--image` 时读本地文件并通过局域网发送。
- CLI 不保存 token；通过参数或环境变量 `BAND_BRIDGE_TOKEN` 传入。

## 错误处理与安全

- Android 服务限制最大请求体 4 MiB，超出返回 413。
- 未授权、错误 JSON、未知路径分别返回 401、400、404。
- 只绑定局域网监听地址；不提供公网穿透或云端转发。
- 通知标题限制 120 字符，正文限制 4000 字符；Android 端再做裁剪。
- 图片通知不保证手环端显示，响应只代表 Android 通知已发布。

## 验证标准

- 在有 Android SDK 的环境中可用 Android Studio/Gradle 构建 APK。
- CLI `health` 能访问 Android 服务；`notify` 能在手机通知栏生成通知。
- 开启 Mi Fitness 对伴侣 App 的通知权限后，真实手环能收到该通知；这一步需要目标手机和手环实测。
- 未配置 Android 环境时，当前仓库仍可用 Node 语法检查和协议级测试验证 CLI。
