import assert from "node:assert/strict";
import { buildNotification } from "./stop-hook.mjs";
import { mergeHookConfig } from "./install-hook.mjs";

const payload = buildNotification({
  hook_event_name: "Stop",
  stop_hook_active: false,
  last_assistant_message: "任务完成\n\n"
});
assert.deepEqual(payload, {
  type: "notify",
  source: "codex",
  title: "Codex 已完成",
  body: "任务完成"
});
assert.equal(buildNotification({ hook_event_name: "Stop", stop_hook_active: true }), null);
assert.equal(buildNotification({ hook_event_name: "UserPromptSubmit" }), null);

const command = 'node "/tmp/stop-hook.mjs"';
const first = mergeHookConfig({}, command);
const second = mergeHookConfig(first, command);
assert.equal(second.hooks.Stop.length, 1);
assert.equal(second.hooks.Stop[0].hooks[0].async, true);
console.log("Codex hook tests passed");
