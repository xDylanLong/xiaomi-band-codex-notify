# Band 10 Pro 表盘工坊

一个本地运行的 Xiaomi Smart Band 10 Pro 自定义表盘编辑器。它把表盘设计成严格的 480×336 图片，支持背景图、时间、日期、步数、心率和自定义文字，并可导出 PNG 与可重新编辑的项目 JSON。

## 直接使用

双击打开 [index.html](/Users/thawingx/Documents/ChatGPT/xiaomi-custom-watch-plater/index.html) 即可运行，不需要安装依赖或登录账号。

如果浏览器限制了本地文件的部分能力，也可以在项目目录执行：

```bash
python3 -m http.server 4173
```

然后打开 <http://localhost:4173>。

## 在小米手环 10 Pro 上查看

当前工具采用官方可验证的“相片表盘”路径：

1. 在编辑器中完成设计，点击“导出 PNG”。
2. 将 PNG 传到手机相册。
3. 打开小米运动健康：设备 → 管理表盘 → 全部 → 自定义。
4. 选择一个相片表盘，进入编辑 → 添加照片，选择导出的 PNG 并同步。

这条路径是把图片作为官方相片表盘背景使用，不是把 PNG 编译成小米原生 `.bin` 表盘包。项目 JSON 是为了保存布局和背景，方便继续编辑，也为未来 Android/Vela 适配器提供输入。

注意：导出的 PNG 是静态背景。编辑器里的时间、步数和心率是视觉排版示例，不会通过本工具实时同步到手环；手环能否在相片表盘模板上叠加动态时间/数据，由你在 Mi Fitness 中选择的模板决定。

## 调研文档

详细结论见 [小米手环 10 Pro 表盘能力调研](/Users/thawingx/Documents/ChatGPT/xiaomi-custom-watch-plater/docs/research/xiaomi-band-10-pro-watchface-capabilities.md)。设计边界和数据格式见 [设计文档](/Users/thawingx/Documents/ChatGPT/xiaomi-custom-watch-plater/docs/superpowers/specs/2026-08-16-xiaomi-band-10-pro-watchface-tool-design.md)。

## 验证

```bash
node --check renderer.js
node --check app.js
```

浏览器验证重点：上传背景图、拖动组件、导出 PNG、导出并重新导入 `.watchface.json`。导出的 PNG 应为 480×336。
