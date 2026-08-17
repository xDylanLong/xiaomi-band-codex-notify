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

输出文件为 `android-companion/releases/小米手环Codex通知-v0.3.6.apk`。

### 签名密钥

脚本默认把固定签名密钥保存在 `android-companion/.signing/release.keystore`。该目录已加入 `.gitignore`，不会提交到公开仓库；重复构建时会复用同一个密钥，因此后续 APK 可以覆盖升级。构建脚本会把当前 keystore 与已发布的 v0.3.2 APK 做证书指纹比对，并在签名完成后再次检查最终 APK；不一致时直接失败，绝不会静默生成一把无法覆盖安装的新钥匙。发布者应备份这个 keystore，丢失后无法继续签名可升级版本。

如需使用 CI 或其他位置的同一把密钥，可通过 `BAND_BRIDGE_KEYSTORE`、`BAND_BRIDGE_KEYSTORE_PASSWORD`、`BAND_BRIDGE_KEY_PASSWORD` 和 `BAND_BRIDGE_KEY_ALIAS` 指定；不要为升级包使用临时目录或新生成的 keystore。需要更换签名证书时，必须把它当作卸载重装版本单独发布，不能伪装成可覆盖升级。只有明确要创建全新安装分支时，才可设置 `BAND_BRIDGE_ALLOW_NEW_KEY=1`。

## 首次配置

1. 安装并打开 App，按系统提示允许通知权限。
2. App 会自动启动 LAN bridge；电脑与手机连接同一个局域网。
3. 在 Mi Fitness → 设备 → 通知和来电 → App 通知中，允许 `小米手环Codex通知`。
4. 记下 App 显示的 4 位 Codex 匹配码，按 [`../docs/usage.md`](../docs/usage.md) 安装 Plugin 并直接输入这 4 个数字。

已有 APK：`releases/小米手环Codex通知-v0.3.6.apk`。如果手机已安装旧签名的 v0.3.1，需要先卸载一次再安装；之后版本可直接覆盖升级。安装命令和完整内容类型见 [`../docs/usage.md`](../docs/usage.md)。

## HTTP API

见 [`../bridge/protocol.md`](../bridge/protocol.md)。成功响应只表示 Android 已创建通知，不证明 Mi Fitness 和手环已经同步；最终链路需要真实手机、Mi Fitness 和手环测试。

## 安全模型

- 服务只建议在可信局域网运行。
- 通知和健康接口要求 Bearer token；首次配对使用局域网 4 位匹配码，token 由 App 自动换取并保存在 Codex 私有配置中。
- UDP `8788` 只响应正确匹配码，并按来源限制响应次数；刷新 App 匹配码即可使旧码失效。
- token 保存在 Android 私有 SharedPreferences 中。
- 没有云服务、账号登录、端口转发或公网监听设计。
- Codex hook 只在任务结束时发送摘要，不会读取或上传完整 transcript。
