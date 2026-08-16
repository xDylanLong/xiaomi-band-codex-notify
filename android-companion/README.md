# Band 10 Pro Bridge Android Companion

这是电脑端桥接的 Android 端。它在局域网 `8787` 端口监听带 token 的 HTTP 请求，并把请求发布为标准 Android 通知。打开 Mi Fitness 的 App 通知同步后，这些通知可以继续到达 Xiaomi Smart Band 10 Pro。

## 构建

用 Android Studio 打开 `android-companion/`，等待 Gradle sync，然后运行 `app`。项目使用 Java、Android API 26+、Android Gradle Plugin 8.7.3，不依赖 AndroidX 或第三方运行时库。

当前开发机没有 Android SDK、Gradle 或 JDK，因此本仓库只能做源码和协议级验证，不能在当前环境直接生成 APK。具备 Android 工具链后可执行：

```bash
cd android-companion
gradle assembleDebug
```

## 首次配置

1. 安装并打开 App，允许通知权限。
2. 点击“启动 LAN bridge”，记下页面显示的手机 IP 和 token。
3. 在 Mi Fitness → 设备 → 通知和来电 → App 通知中，允许 `Band 10 Pro Bridge`。
4. 电脑与手机连接同一个局域网。
5. 使用 `bridge/bandctl.mjs` 调用 Android 接口。

## HTTP API

见 [`../bridge/protocol.md`](../bridge/protocol.md)。成功响应只表示 Android 已创建通知，不证明 Mi Fitness 和手环已经同步；最终链路需要真实手机、Mi Fitness 和手环测试。

## 安全模型

- 服务只建议在可信局域网运行。
- 所有接口要求 Bearer token。
- token 保存在 Android 私有 SharedPreferences 中。
- 没有云服务、账号登录、端口转发或公网监听设计。
