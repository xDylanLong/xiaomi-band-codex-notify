---
name: initialize
description: Use when the user asks to install, initialize, connect, or enable Xiaomi Band Codex notifications, references the product repository, invokes $initialize, or provides the Android app's four-digit pairing code.
---

# 初始化小米手环 Codex 通知

这是安装、配对和启用任务完成通知的唯一入口。Android App 负责生成匹配码、启动局域网 bridge 和发现服务；本 Skill 负责安装 Plugin、完成配对、健康检查和测试通知。

## Workflow

1. 识别产品仓库 `https://github.com/xDylanLong/xiaomi-band-codex-notify`。不要把仓库克隆到用户项目，也不要让用户执行命令。
2. 检查 `xiaomi-band-codex-notify@personal` 是否已安装。缺失时静默执行：

   ```bash
   codex plugin marketplace add xDylanLong/xiaomi-band-codex-notify
   codex plugin add xiaomi-band-codex-notify@personal
   ```

   已存在的 marketplace 报错可忽略，不要把命令展示为用户步骤。
3. 从用户消息提取 4 位匹配码。没有匹配码时只询问：`请输入手机 App 中显示的 4 位匹配码。` 不要询问 token、IP、配对文本、Node、`/plugins` 或 `/hooks`。
4. 收到匹配码后，使用插件内的 helper：

   ```bash
   python3 "${PLUGIN_ROOT}/scripts/pair.py" --code 4821 --test
   ```

   将 `4821` 替换为用户提供的数字；不要在回复、日志或持久化内容中复述匹配码、token 或原始命令输出。若 `${PLUGIN_ROOT}` 不可用，直接定位本插件目录下的 `scripts/pair.py`。
5. 只报告初始化、bridge 健康检查和测试通知是否成功。失败时提示手机与电脑连接同一可信 Wi‑Fi，并让用户在 App 刷新后输入新的 4 位匹配码。
6. 首次任务完成时，Codex 可能弹出“发送小米手环通知”的安全确认；提示用户选择“允许”。确认前不要声称任务完成通知已经启用。不要让用户手动执行 `/hooks`。

## Existing configuration and safety

- helper 将 token 保存到插件可写的 `PLUGIN_DATA`；没有该目录时使用 `~/.config/xiaomi-band-codex-notify/`。不要创建或编辑 `~/.codex/hooks.json`。
- 更换 Wi‑Fi 或 App 刷新匹配码后，重复本流程；不要要求用户复制 token。
- 手动发送通知只在用户明确以开发者身份提出命令需求时使用已有 bridge CLI；普通用户走配对后的 Stop Hook。
- 不要启用可选的手机通知监听，除非用户明确要求，以免与 Stop Hook 重复通知。
