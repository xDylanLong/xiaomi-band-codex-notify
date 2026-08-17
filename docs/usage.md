# 小米手环Codex通知 使用说明

## 1. 安装 Android 伴侣

直接安装仓库中的 Debug APK：

```bash
$ANDROID_SDK_ROOT/platform-tools/adb install -r android-companion/releases/小米手环Codex通知-v0.3.0.apk
```

也可以把 APK 传到 Android 手机后手动安装：

`android-companion/releases/小米手环Codex通知-v0.3.0.apk`

打开 App 后：

1. 允许通知权限；
2. 确认电脑和手机在同一个局域网；
3. App 会自动启动 LAN bridge；
4. 点击“复制 Codex 配对信息”，稍后粘贴给 Codex。

## 2. 配置 Mi Fitness

在小米运动健康中打开：

```text
设备 → 通知和来电 → App 通知
```

选中 `小米手环Codex通知`。先用手机通知测试，确认手环可以收到伴侣 App 的通知。

## 3. 安装 Plugin 并配对

在 Codex CLI 中添加公开 marketplace：

```bash
codex plugin marketplace add xDylanLong/xiaomi-band-codex-notify
codex /plugins
```

在 Codex 桌面端也可以直接从 Plugins Directory 添加 `xDylanLong/xiaomi-band-codex-notify`。安装后新开一个 Codex 会话。

回到 App 点击“复制 Codex 配对信息”，粘贴给 Codex，并说：

```text
连接我的小米手环
```

Plugin 会自动检查手机、保存私有配置并发送测试通知。然后在 Codex 中执行 `/hooks`，审核并信任“发送小米手环通知”。如果手机 IP 变化，重新复制配对信息并再次执行连接即可。

## 4. 从电脑手动发送通知（开发者）

```bash
export BAND_BRIDGE_HOST=192.168.1.23
export BAND_BRIDGE_TOKEN='Android App 显示的 token'

node bridge/bandctl.mjs health --json
node bridge/bandctl.mjs notify \
  --source codex \
  --title 'Codex' \
  --body '任务完成，可以回来看结果了'
```

任何本地脚本都可以调用同一条命令，所以可以接入 Codex 任务结束脚本、构建脚本、定时任务或个人自动化。

## 5. 发送运动计划

```bash
node bridge/bandctl.mjs plan \
  --date 2026-08-20 \
  --title '晨间跑步' \
  --duration 20 \
  --target '慢跑 3 公里' \
  --note '完成后拉伸'
```

手环上能看到的是一条通知。要查看更完整的排版卡片，打开根目录的 Web 编辑器，切换到“运动计划卡片”，导出 PNG，再通过 Mi Fitness 的相片表盘同步。

## 6. 可以发送什么内容

| 内容 | 方式 | 手环端预期 |
| --- | --- | --- |
| Codex/电脑文字通知 | `bandctl notify` | 文字通知、震动取决于 Mi Fitness 设置 |
| 运动计划 | `bandctl plan` 或编辑器 PNG | 通知文字，或静态计划卡片 |
| 截图/飞书文档图片 | 编辑器上传图片导出 PNG；CLI 也支持 `--image` | 手机通知可带图；手环是否显示图片取决于 Mi Fitness/固件 |
| 微信语音回复 | 当前没有实现 | 10 Pro 官方不支持第三方通知回复 |

## 7. 重新构建 APK（开发者）

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_SDK_ROOT=/path/to/android-sdk
./android-companion/build-local.sh
```

脚本使用 `javac`、`d8`、`aapt2`、`zipalign` 和 `apksigner`，不依赖 Gradle 下载。输出为 `android-companion/releases/小米手环Codex通知-v0.3.0.apk`。

## 8. 排查

- `health` 连接失败：确认手机和电脑同一 Wi-Fi，并重新从 App 复制配对信息。
- HTTP 401：重新复制配对信息，让 Plugin 更新私有配置。
- 手机收到但手环没有：检查 Mi Fitness 的 App 通知开关、蓝牙连接、手环勿扰模式和手机后台限制。
- Codex 没有自动通知：确认 Plugin 已安装、已完成“连接我的小米手环”，并在 Codex `/hooks` 中信任 hook；当前 Codex `exec` 非交互模式可能不会触发 `Stop` hook，交互式 Codex 任务优先使用。
- 电脑重启后：重新打开 Android App；bridge 会自动启动。

## 9. 旧版开发者兼容入口

仓库仍保留 `codex/install-hook.mjs` 和 `codex/stop-hook.mjs`，用于没有 Plugin 支持的旧版 Codex 或个人脚本。普通用户不要使用这条 Node 流程。
