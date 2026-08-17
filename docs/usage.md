# 小米手环Codex通知 使用说明

## 1. 安装 Android 伴侣

直接安装仓库中的 APK：

```bash
$ANDROID_SDK_ROOT/platform-tools/adb install -r android-companion/releases/小米手环Codex通知-v0.3.4.apk
```

如果手机中已有 v0.3.1 或更早版本，需要先卸载旧 App，再安装 v0.3.4；这是因为旧版本使用了不同签名。v0.3.2 及之后的版本会复用固定签名，可以直接覆盖升级。

也可以把 APK 传到 Android 手机后手动安装：

`android-companion/releases/小米手环Codex通知-v0.3.4.apk`

打开 App 后：

1. 允许通知权限；
2. 确认电脑和手机在同一个局域网；
3. App 会自动启动 LAN bridge；
4. 记下 App 中显示的 4 位“Codex 匹配码”。

## 2. 配置 Mi Fitness

在小米运动健康中打开：

```text
设备 → 通知和来电 → App 通知
```

选中 `小米手环Codex通知`。先用手机通知测试，确认手环可以收到伴侣 App 的通知。

## 3. 复制一句话安装 Skill

不需要先打开 `/plugins`，把下面整句话复制到 Codex：

```text
请根据 GitHub 仓库 https://github.com/xDylanLong/xiaomi-band-codex-notify 安装并启用“小米手环 Codex 通知”Skill。安装完成后，请只询问我手机 App 中显示的 4 位匹配码，不要让我执行命令、复制 token 或输入手机 IP。
```

Skill 安装完成后会主动询问匹配码。回到手机 App 查看 4 位数字，例如 `4821`，只回复：

```text
4821
```

Skill 会通过局域网自动发现手机、保存私有配置并发送测试通知。不需要手动执行 `/hooks`；首次任务结束时，如果 Codex 弹出“发送小米手环通知”的安全确认，点击“允许”一次即可。之后自动生效。如果换 Wi-Fi 或点击了“刷新匹配码”，重新打开 App 并回复新的 4 位数字即可。

4 位匹配码只用于可信局域网内的首次配对，不是高强度密码；App 会限制成功发现响应次数，并提供刷新按钮。token 不会显示在 App 界面，也不需要用户复制。

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

脚本使用 `javac`、`d8`、`aapt2`、`zipalign` 和 `apksigner`，不依赖 Gradle 下载。输出为 `android-companion/releases/小米手环Codex通知-v0.3.4.apk`。固定签名密钥默认保存在被 Git 忽略的 `android-companion/.signing/release.keystore`，请发布者自行备份，不要提交到公开仓库。

## 8. 排查

- `health` 连接失败：确认手机和电脑同一 Wi-Fi，并重新输入 App 显示的 4 位匹配码。
- HTTP 401：点击 App 的“刷新匹配码”，再让 Plugin 使用新数字配对。
- 手机收到但手环没有：检查 Mi Fitness 的 App 通知开关、蓝牙连接、手环勿扰模式和手机后台限制。
- Codex 没有自动通知：确认 Plugin 已安装、已完成“连接我的小米手环”，并在首次安全提示中允许“发送小米手环通知”；当前 Codex `exec` 非交互模式可能不会触发 `Stop` hook，交互式 Codex 任务优先使用。
- 电脑重启后：重新打开 Android App；bridge 会自动启动。

## 9. 旧版开发者兼容入口

仓库仍保留 `codex/install-hook.mjs` 和 `codex/stop-hook.mjs`，用于没有 Plugin 支持的旧版 Codex 或个人脚本。普通用户不要使用这条 Node 流程。
