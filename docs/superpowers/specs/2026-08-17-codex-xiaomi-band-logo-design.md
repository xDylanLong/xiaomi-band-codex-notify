# Codex · Xiaomi 绑定 Logo 与通知图标设计

## 目标

将“小米手环 Codex 通知”的现有产品 logo 替换为扁平、Apple 风格的原创视觉标识：两个原创抽象符号由一条智能手环连接，表达 Codex 任务与小米手环通知之间的桥接关系，同时避免直接复制第三方官方商标图形。

产品 logo 和小米手环通知需要保持同一套识别语言，同时适配 Android 通知图标的渲染约束。

## 视觉方案

- 风格：扁平、克制、圆角几何、清晰留白，不使用 3D 建模、复杂阴影或渐变光效。
- 结构：左侧为原创开放环与节点符号，右侧为原创橙色阶梯信号符号，中间是一条明确可识别的智能手环：两端为抽象模块，中间为深色胶囊表盘，蓝色表带贯穿连接两者。
- 识别文字：左侧模块表面加入清晰的 `CODEX`，右侧模块表面加入清晰的 `XIAOMI`；文字是主要识别辅助，不依赖官方 logo 图形。
- 色彩：左侧使用深炭色与白色，右侧使用暖橙色与白色，手环表带使用项目现有的蓝色，表盘使用深炭色；颜色数量保持足够少，缩小后仍可辨认。
- 主 logo：透明背景、方形构图、中心主体占画布大部分，适合产品首页、README、Android launcher 和插件资源。
- 通知 glyph：从同一构图提炼出高对比度的白色单色手环轮廓，透明背景，避免细线、文字和无法在 24dp 中识别的细节。

## 资源与引用范围

### 主 logo

生成一份透明背景的彩色 PNG，作为唯一主视觉源，替换并同步到：

- `assets/logo.png`
- `android-companion/app/src/main/res/drawable/logo.png`
- `plugins/xiaomi-band-codex-notify/assets/logo.png`

保持三份内容完全一致，避免产品页、APK 和插件市场展示不一致。

### Android 通知图标

新增 Android 专用单色 drawable 资源，例如 `android-companion/app/src/main/res/drawable/ic_notification.xml` 或等价的透明 PNG。通知发布逻辑不再使用 `android.R.drawable.ic_dialog_info`，改用项目自己的通知 glyph。

通知构建器同时设置彩色主 logo 作为 `largeIcon`（在 Android/手机通知界面可用时展示），但不依赖 `largeIcon` 作为小米手环最终显示的保证；Mi Fitness 和手环固件是否展示大图仍遵循现有产品说明中的真实边界。

## Android 行为

- 前台 bridge 服务通知和消息通知都使用项目通知 glyph 作为 `smallIcon`。
- 消息通知继续保留现有标题、正文、BigText/BigPicture、点击回 App 和通知渠道行为。
- 不修改 `applicationId`、包名、LAN 协议、配对流程或通知权限流程。
- 主 logo 继续作为 application launcher icon 使用。

## 生成与质量约束

- 使用透明背景生成主 logo，文字只允许为 `CODEX` 与 `XIAOMI`，避免随机字母、额外品牌、人物、设备外壳或水印。
- 生成后检查两个原创抽象符号是否同时存在、手环表带和中央表盘是否形成明确连接、透明边缘是否干净，以及缩小到通知尺寸后是否仍可辨认；不得将图形描述为官方 ChatGPT/OpenAI 或 Xiaomi logo。
- 如果生成图中的品牌细节不够稳定，保留构图作为参考，并用确定性的 Android drawable 重新绘制通知 glyph；不把不可辨认的生成细节直接用于 `smallIcon`。

## 验收标准

1. 三份主 logo 文件存在且像素内容一致，均为透明背景的扁平彩色原创抽象手环标识。
2. Android launcher 使用新主 logo。
3. Android 前台服务通知和消息通知不再使用系统默认 info 图标。
4. 通知使用项目自有单色 glyph 作为 `smallIcon`，并尝试设置主 logo `largeIcon`。
5. Android companion 能使用现有构建流程成功构建 APK，包名、版本和既有通知能力不回退。
6. README、插件资源和 Android 资源不再残留旧版 logo。
7. 验证报告明确区分：代码已使用新图标、手机通知实际展示、以及小米手环/ Mi Fitness 是否最终显示 logo；后两项需要真实设备确认，不能用静态构建结果代替。

## 非目标

- 不改变 Codex hook、LAN bridge、Mi Fitness 配置或配对体验。
- 不承诺 Android 或 Mi Fitness 一定在小米手环上显示彩色 logo。
- 不宣称原创符号已获得商标清除；正式发布前仍应进行商标检索和必要的法律审查。
- 不新增完整品牌手册、动效 logo 或多套营销物料。
