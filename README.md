# 小米手环 Codex 通知

小米手环 10 Pro 的 Codex 任务完成通知工具。通过 Android App、Mi Fitness 和局域网连接，让 Codex、电脑程序和运动计划在小米手环上收到提醒；支持 4 位数字配对，不需要复制 token、输入手机 IP、安装 Node 或启动 bridge。

[下载 Android App v0.3.1](https://github.com/xDylanLong/xiaomi-band-codex-notify/releases/latest/download/xiaomi-band-codex-notify-v0.3.1.apk)　·　[查看 GitHub 仓库](https://github.com/xDylanLong/xiaomi-band-codex-notify)　·　[查看完整使用指南](docs/usage.md)

## 极简使用指南

目标：Codex 完成任务后，手机收到通知，Mi Fitness 再把通知同步到小米手环。

### 1. 安装手机 App

在 Android 手机上安装 [小米手环 Codex 通知 APK](https://github.com/xDylanLong/xiaomi-band-codex-notify/releases/latest/download/xiaomi-band-codex-notify-v0.3.1.apk)，打开后允许通知权限。

App 会自动启动局域网服务。电脑和手机必须连接同一个 Wi‑Fi。

### 2. 打开小米手环通知

在小米运动健康中打开：

```text
设备 → 通知和来电 → App 通知 → 小米手环 Codex 通知
```

先点击手机 App 的“发送测试通知”，确认手环可以收到。

### 3. 安装 Codex Plugin

在 Codex 中打开 `/plugins`，添加并安装：

```text
xDylanLong/xiaomi-band-codex-notify
```

也可以在 Codex CLI 中执行：

```bash
codex plugin marketplace add xDylanLong/xiaomi-band-codex-notify
codex /plugins
```

安装完成后重新打开一个 Codex 会话。

### 4. 只复制这一句话完成初始化

打开手机 App，记下“Codex 匹配码”下的 4 位数字，把下面整句话复制到 Codex：

```text
使用 $connect，根据 GitHub 仓库 https://github.com/xDylanLong/xiaomi-band-codex-notify 完成小米手环 Codex 通知初始化。我的 4 位匹配码是 1234。请自动完成配对、发送测试通知并启用任务完成通知。
```

把 `1234` 替换成手机上实际显示的数字。Skill 会自动完成局域网发现、配对、测试通知和任务完成通知配置。

不需要复制 token、手机 IP、配对文本，也不需要手动输入 Node 命令。

### 5. 首次允许一次安全提示

不需要手动输入 `/hooks`。首次使用时，如果 Codex 弹出“发送小米手环通知”的安全确认，点击“允许”一次即可；之后 Codex 完成交互式任务时会自动发送通知。

### 6. 开始使用

正常使用 Codex 即可：

```text
Codex 完成任务 → 手机通知 → Mi Fitness → 小米手环
```

完整的安装、配对、运动计划、截图和故障排查说明见：[完整使用指南](docs/usage.md)。

## 这个工具能做什么

- **Codex 完成通知**：任务结束后，把完成摘要发送到手机和小米手环。
- **电脑通知**：通过局域网把构建、脚本、定时任务等电脑消息发送到手环。
- **运动计划**：创建日期、时长、目标、备注等运动计划，并发送文字提醒。
- **截图和飞书文档**：上传图片或导出的飞书文档截图，生成适合相片表盘使用的 PNG。
- **小米手环 10 Pro 自定义内容**：通过 Mi Fitness 的相片表盘查看静态计划卡片和图片。
- **极简配对**：4 位数字匹配码 + 局域网自动发现，token 由 Plugin 自动换取和保存。
- **隐私和网络边界**：只在手机与电脑所在的局域网工作，不使用云端中转。

## 当前不支持什么

- **微信语音回复**：小米手环 10 Pro 的第三方 App 通知目前只能查看，不能直接回复微信语音。
- **手环原生动态表盘数据注入**：导出的 PNG 是静态图片，不是小米原生 `.bin` 表盘包。
- **图片一定显示在手环通知中**：手机通知可以携带图片，但手环是否显示图片取决于 Mi Fitness 和手环固件。
- **非交互式 Codex 任务保证触发**：当前主要支持交互式 Codex 任务的完成通知。

## 在小米手环 10 Pro 上查看运动计划或图片

当前采用官方可验证的相片表盘路径：

1. 在根目录打开 [index.html](index.html)。
2. 上传截图、飞书文档图片，或切换到“运动计划卡片”。
3. 填写内容并导出 PNG。
4. 将 PNG 传到手机相册。
5. 在小米运动健康中进入：`设备 → 管理表盘 → 全部 → 自定义`。
6. 选择相片表盘，添加导出的 PNG 并同步。

导出的 PNG 是静态背景。编辑器中的时间、步数和心率是排版示例，不会通过本工具实时写入手环。

## 常见问题

### 配对失败怎么办？

确认手机和电脑连接同一个 Wi‑Fi，并保持 App 打开。然后点击 App 的“刷新匹配码”，在 Codex 中重新输入新的 4 位数字。

### 手机收到通知，但手环没有收到怎么办？

检查以下设置：

- Mi Fitness 是否开启“小米手环 Codex 通知”的 App 通知；
- 手机蓝牙是否连接手环；
- 手环是否开启勿扰模式；
- Android 是否限制 App 后台运行。

### Codex 没有自动通知怎么办？

确认 Plugin 已安装、已经完成“连接我的小米手环”，并在首次安全提示中允许“发送小米手环通知”。如果是 `codex exec` 非交互式任务，可能不会触发 `Stop` Hook；优先使用交互式 Codex 会话。

### 更换 Wi‑Fi 后怎么办？

重新打开手机 App，点击“刷新匹配码”，然后在 Codex 中重新输入新的匹配码。

## 开发者入口

普通用户不需要使用以下内容。仓库同时提供：

- [Android 伴侣源码](android-companion/README.md)：自动启动局域网服务并发布 Android 通知。
- [Codex Plugin](plugins/xiaomi-band-codex-notify/.codex-plugin/plugin.json)：包含连接 Skill 和 Python Stop Hook。
- [电脑端 bridge CLI](bridge/README.md)：向 Android App 发送通知、运动计划和图片。
- [桥接协议](bridge/protocol.md)：供脚本、构建工具和个人自动化调用。
- [调研文档](docs/research/xiaomi-band-10-pro-watchface-capabilities.md)：小米手环 10 Pro 表盘和通知能力边界。
- [完整设计文档](docs/superpowers/specs/2026-08-16-xiaomi-band-10-pro-watchface-tool-design.md)：数据格式和实现边界。

旧版 Codex 或个人脚本仍可使用仓库中的 Node 兼容入口，但普通用户不需要安装 Node、编辑 Codex 配置文件或手动启动 bridge。

## 关键词

小米手环 10 Pro、自定义表盘、小米手环通知、Codex 通知、电脑通知、Android 通知、Mi Fitness、局域网通知、运动计划、截图显示、飞书文档、相片表盘、Codex Plugin、Codex Hook。
