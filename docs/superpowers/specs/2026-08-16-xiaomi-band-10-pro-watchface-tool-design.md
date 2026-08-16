# 小米手环 10 Pro 自定义表盘工具设计

## 目标

制作一个无需后端、浏览器直接打开即可使用的自定义表盘工具，用户可以设计适配 Xiaomi Smart Band 10 Pro 480×336 屏幕的内容，实时预览并导出图片，随后通过小米运动健康 App 的“相片表盘”功能在手环上查看。

## 调研结论与边界

### 已确认的官方能力

- 小米官方 FAQ 明确支持“相片表盘”：在 Mi Fitness 的设备表盘管理中选择自定义表盘、添加相册图片、裁剪并保存。
- 10 Pro 官方规格页列出 1.74 英寸、480×336 分辨率，因此编辑画布固定为 480×336。
- 官方公开开发者文档目前没有发现针对 10 Pro 第三方表盘生成、安装或蓝牙写入的公开 SDK/API。

### 社区能力（实验性）

- EasyFace 等社区项目能够解析/编辑部分小米穿戴设备的表盘资源，并将 Band 10 系列列为支持设备。
- 米坛社区已有 10 Pro 表盘自定义工具和安装教程，但依赖 Android、设备授权、Mi Fitness/固件状态与社区逆向实现。
- 小米 Vela JS 是面向智能穿戴的轻量应用开发平台，但 Vela 应用（`.rpk`）和表盘资源不是同一格式；是否可在具体 10 Pro 地区固件上安装需要真机验证。

因此，第一版不伪造 `.bin` 表盘包，也不把未验证的 BLE 写入称为已支持。工具生成的是官方相片表盘可用的 PNG，以及一个可供未来 Android/Vela 适配器读取的 `.watchface.json` 项目文件。

## 用户流程

1. 打开 `index.html`。
2. 选择背景颜色或上传一张背景图。
3. 设置时间、日期、步数、心率等信息的显示开关、颜色和位置。
4. 在 480×336 的设备预览中确认效果。
5. 点击“导出 PNG”，得到可导入小米运动健康的图片。
6. 点击“导出项目”，得到包含布局配置和内嵌背景图的 `.watchface.json` 文件。
7. 按页面内的安装指引：把 PNG 传到手机，在 Mi Fitness → 设备 → 管理表盘 → 全部 → 自定义中选择相片表盘并添加图片。

## 设计

### 视觉

采用深色编辑工作台：左侧为编辑控件，中央为带圆角外框的真实比例设备预览，右侧为“可落地路径”提示。默认模板突出时间、日期和三个指标，保证即使用户不上传图片也能立即导出可用结果。

### 功能模块

- `WatchFaceState`：统一保存背景、文本、组件开关、颜色、位置和设备规格。
- `renderer.js`：使用 Canvas 2D 按状态绘制预览和导出 PNG；导出时使用同一渲染函数，避免预览与文件不一致。
- `app.js`：处理表单、拖动组件、图片读取、导出、导入项目和提示状态。
- `index.html` / `styles.css`：布局、响应式适配和操作说明。

### 数据格式

`.watchface.json` 保存：

```json
{
  "format": "xiaomi-band-watchface-project",
  "version": 1,
  "device": { "model": "Xiaomi Smart Band 10 Pro", "width": 480, "height": 336 },
  "background": { "color": "#10141c", "imageDataUrl": null },
  "widgets": [
    { "type": "time", "x": 240, "y": 136, "color": "#ffffff", "visible": true }
  ]
}
```

### 错误处理

- 非图片文件、读取失败或超大图片时显示明确提示，不破坏当前项目。
- 导入 JSON 时校验 `format`、`version`、画布尺寸和组件类型；不符合时拒绝导入。
- 不支持的设备写入能力在 UI 中明确标为“实验性/未验证”，不提供误导性的“已同步”状态。

## 验证标准

- 浏览器直接打开后能看到完整编辑器，不依赖网络或构建步骤。
- 修改控件立即更新预览；上传图片后能正确裁剪到 480×336。
- 导出的 PNG 尺寸严格为 480×336，透明度和文字清晰。
- 导出的项目 JSON 可重新导入并恢复状态。
- 在桌面宽屏和窄屏移动视口下布局可用。
- 文档明确区分官方相片表盘路径、社区逆向路径和 Vela 应用路径。

## 参考来源

- 小米官方 FAQ：https://www.mi.com/global/support/faq/details/KA-703579/
- 小米 Smart Band 10 Pro 规格：https://www.mi.com/cz/product/xiaomi-smart-band-10-pro/specs/
- 小米 Vela JS 文档：https://iot.mi.com/vela/quickapp/en/guide/
- EasyFace：https://github.com/m0tral/EasyFace
- 米坛安装教程：https://wiki.bandbbs.cn/Guides/watchface_custom_tool/watchface_custom_tool-install.html
