# 小米手环 Codex 通知 APK-first Bootstrap 设计

## 目标

把首次使用收敛为：先安装并打开 Android APK，手机完成本地初始化；用户随后只需在 Codex 复制一句话，Codex 自动安装并启用 Skill，Skill 再询问手机上的 4 位匹配码；用户回复数字后，Codex 自动完成配对、测试和任务完成通知配置。

## 最终用户流程

### 第一步：安装 APK

用户安装并打开「小米手环 Codex 通知」APK：

- 允许 Android 通知权限；
- App 自动生成并保存 4 位 Codex 匹配码；
- App 自动启动局域网 bridge 和发现服务；
- 用户在 Mi Fitness 中开启该 App 的通知同步；
- 用户不需要复制 token、手机 IP 或任何配对文本。

手机 App 后续只需要保持打开或重新打开，bridge 会自动启动。

### 第二步：复制一句话到 Codex

用户复制以下模板，替换 APK 中显示的匹配码之前不需要填写数字：

```text
请根据 GitHub 仓库 https://github.com/xDylanLong/xiaomi-band-codex-notify 安装并启用“小米手环 Codex 通知”Skill。安装完成后，请只询问我手机 App 中显示的 4 位匹配码，不要让我执行命令、复制 token 或输入手机 IP。
```

Bootstrap 引导负责：

- 安装公开 marketplace 和 `xiaomi-band-codex-notify` Plugin；
- 让用户只看到 Skill 安装结果；
- Skill 安装完成后询问 4 位匹配码；
- 不要求用户输入 `/plugins`、`/hooks`、Node 命令或 bridge 命令。

如果当前 Codex 版本在安装 Plugin 后要求重新加载会话，Bootstrap 只能给出产品化的“重新打开 Codex 会话”提示，不暴露命令细节。

### 第三步：回复匹配码

Skill 只询问：

```text
请输入手机 App 中显示的 4 位匹配码。
```

用户回复 4 位数字后，Skill 自动执行：

1. UDP 局域网发现手机；
2. 使用匹配码交换内部 token；
3. 保存私有配置；
4. 检查 bridge 健康状态；
5. 发送测试通知；
6. 完成 Codex Stop Hook 初始化。

首次出现 Codex Hook 安全提示时，用户只需点击一次“允许”；不要求用户主动输入 `/hooks`。

## 日常使用

初始化完成后：

```text
打开手机 App → 正常使用 Codex → 任务完成通知自动到达小米手环
```

更换 Wi-Fi 或刷新匹配码时，Skill 重新询问新数字即可。token 始终只由 Skill 内部管理。

## 验收标准

- README 顶部第一步是安装 APK，不是安装 Codex Plugin；
- 用户可复制一段完整中文引导语启动 Bootstrap；
- Bootstrap 安装 Skill 后询问 4 位匹配码；
- 用户只需回复 4 位数字，Codex 完成剩余初始化；
- 普通用户不需要接触 `/plugins`、`/hooks`、Node、IP、token 或 bridge；
- App 打开后自动启动 bridge，日常无需电脑端常驻 Node 进程；
- README 同时提供完整使用指南链接和必要的安全提示。
