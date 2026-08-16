# 小米手环Codex通知

一个只走局域网的 Codex 完成通知工具：手机安装「小米手环Codex通知」App，电脑端安装一次 Codex Stop hook，任务完成后即可通过 Mi Fitness 把通知同步到小米手环。仓库也保留表盘、截图/飞书文档图片和运动计划卡片能力。

[下载 Android APK（v0.2.0）](https://github.com/xDylanLong/xiaomi-band-codex-notify/releases/latest/download/xiaomi-band-codex-notify-v0.2.0.apk) · [查看 GitHub Releases](https://github.com/xDylanLong/xiaomi-band-codex-notify/releases)

## 直接使用

双击打开 [index.html](index.html) 即可运行，不需要安装依赖或登录账号。

如果浏览器限制了本地文件的部分能力，也可以在项目目录执行：

```bash
python3 -m http.server 4173
```

然后打开 <http://localhost:4173>。

## Codex 完成通知

1. 手机上安装 [小米手环Codex通知 APK](android-companion/releases/小米手环Codex通知-debug.apk)。
2. App onboarding 中允许通知、启动 LAN bridge，并在 Mi Fitness 中允许 `小米手环Codex通知`。
3. 电脑端执行一次：

```bash
node codex/install-hook.mjs --host <手机IP> --token '<App 中的 token>'
```

4. 在 Codex 中执行 `/hooks`，审核并信任“发送小米手环通知”。

之后 Codex 每次完成一个交互式任务 turn，手机会收到摘要通知，再由 Mi Fitness 同步到手环。整个方案只走局域网，不使用云服务。

## 在小米手环 10 Pro 上查看

当前工具采用官方可验证的“相片表盘”路径：

1. 在编辑器中完成设计，点击“导出 PNG”。
2. 将 PNG 传到手机相册。
3. 打开小米运动健康：设备 → 管理表盘 → 全部 → 自定义。
4. 选择一个相片表盘，进入编辑 → 添加照片，选择导出的 PNG 并同步。

这条路径是把图片作为官方相片表盘背景使用，不是把 PNG 编译成小米原生 `.bin` 表盘包。项目 JSON 是为了保存布局和背景，方便继续编辑，也为未来 Android/Vela 适配器提供输入。

注意：导出的 PNG 是静态背景。编辑器里的时间、步数和心率是视觉排版示例，不会通过本工具实时同步到手环；手环能否在相片表盘模板上叠加动态时间/数据，由你在 Mi Fitness 中选择的模板决定。

## 三个核心需求的当前结论

- 手机第三方通知到手环：支持。Mi Fitness 可以把手机通知同步到手环，需要在“设备 → 通知和来电 → App 通知”里开启。电脑或 Codex 通知先由 Android 伴侣发布到手机通知栏，再沿这条路径到手环。
- 截图、飞书文档和运动计划：支持静态查看。上传截图/文档图片，或切换到“运动计划卡片”填写日期、标题、时长、目标和备注，再导出 PNG 同步。
- 微信语音回复：不支持。10 Pro 官方 FAQ 说明第三方 App 通知只能查看、不能回复；来电快捷回复是预设文字，不是微信语音。要做电脑通知桥接或双向回复，需要另外开发 Android 伴侣 App，并不能由当前浏览器编辑器直接完成。

## 能力最全方案

仓库现在包含 Android 伴侣和电脑端 bridge CLI：

- [Android 伴侣源码](android-companion/README.md)：极简 onboarding、可选通知监听，在手机局域网 `8787` 端口接收通知请求，并发布标准 Android 通知。
- [Codex hook](codex/stop-hook.mjs)：在 Codex turn 结束时自动发送摘要。
- [电脑端 CLI](bridge/README.md)：用 `node bridge/bandctl.mjs notify ...` 给 Android 伴侣发通知。
- [桥接协议](bridge/protocol.md)：适合 Codex、脚本和其他程序直接调用。
- [完整使用说明](docs/usage.md)：安装 APK、配置 Mi Fitness、发送通知和运动计划。

完整路径是：电脑/Codex → `bandctl` → Android 伴侣 → Mi Fitness App 通知同步 → 小米手环。仓库已提供并验证 Debug APK；如果要重新构建，可使用 Android Studio，或执行 `android-companion/build-local.sh`。

## 调研文档

详细结论见 [小米手环 10 Pro 表盘能力调研](docs/research/xiaomi-band-10-pro-watchface-capabilities.md)。设计边界和数据格式见 [设计文档](docs/superpowers/specs/2026-08-16-xiaomi-band-10-pro-watchface-tool-design.md)。

## 验证

```bash
node --check renderer.js
node --check app.js
```

浏览器验证重点：上传背景图、拖动组件、导出 PNG、导出并重新导入 `.watchface.json`。导出的 PNG 应为 480×336。
