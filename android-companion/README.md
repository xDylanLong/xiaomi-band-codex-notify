# 小米手环Codex通知 Android Companion

这是电脑端桥接的 Android 端。它在局域网 `8787` 端口监听带 token 的 HTTP 请求，并把请求发布为标准 Android 通知。打开 Mi Fitness 的 App 通知同步后，这些通知可以继续到达 Xiaomi Smart Band 10 Pro。

## 构建

用 Android Studio 打开 `android-companion/`，等待 Gradle sync，然后运行 `app`。项目使用 Java、Android API 26+、Android Gradle Plugin 8.7.3，不依赖 AndroidX 或第三方运行时库。

标准 Android Studio 环境可以直接构建 APK。若使用 Gradle，可执行：

```bash
cd android-companion
gradle assembleDebug
```

如果没有 Gradle，也可以使用仓库内的标准工具链脚本：

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_SDK_ROOT=/path/to/android-sdk
./android-companion/build-local.sh
```

输出文件为 `android-companion/releases/小米手环Codex通知-debug.apk`。

## 首次配置

1. 安装并打开 App，按 onboarding 允许通知权限。
2. 可选：点击“开启手机通知监听”，在系统“通知使用权”里允许本 App 转发 Codex/ChatGPT 通知。
3. 点击“启动 LAN bridge”，记下页面显示的手机 IP 和 token。
4. 在 Mi Fitness → 设备 → 通知和来电 → App 通知中，允许 `小米手环Codex通知`。
5. 电脑与手机连接同一个局域网。
6. 在电脑端按 [`../docs/usage.md`](../docs/usage.md) 安装 Codex Stop hook。

已有 Debug APK：`releases/小米手环Codex通知-debug.apk`。安装命令和完整内容类型见 [`../docs/usage.md`](../docs/usage.md)。

## HTTP API

见 [`../bridge/protocol.md`](../bridge/protocol.md)。成功响应只表示 Android 已创建通知，不证明 Mi Fitness 和手环已经同步；最终链路需要真实手机、Mi Fitness 和手环测试。

## 安全模型

- 服务只建议在可信局域网运行。
- 所有接口要求 Bearer token。
- token 保存在 Android 私有 SharedPreferences 中。
- 没有云服务、账号登录、端口转发或公网监听设计。
- Codex hook 只在任务结束时发送摘要，不会读取或上传完整 transcript。
