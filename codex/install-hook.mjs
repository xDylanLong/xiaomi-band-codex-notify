#!/usr/bin/env node

import { chmod, mkdir, readFile, writeFile } from "node:fs/promises";
import { homedir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

function parseArgs(argv) {
  const options = {};
  for (let index = 0; index < argv.length; index += 1) {
    const item = argv[index];
    if (!item.startsWith("--")) continue;
    const key = item.slice(2);
    const value = argv[index + 1];
    if (!value || value.startsWith("--")) throw new Error(`缺少参数值: --${key}`);
    options[key] = value;
    index += 1;
  }
  return options;
}

function required(options, key) {
  if (!options[key]) throw new Error(`缺少必填参数: --${key}`);
  return options[key];
}

function configRoot(env = process.env) {
  return env.XDG_CONFIG_HOME || (env.APPDATA ? join(env.APPDATA) : join(homedir(), ".config"));
}

function hookCommand(hookPath) {
  return `node "${hookPath.replaceAll('"', '\\"')}"`;
}

export function mergeHookConfig(existing, command) {
  const result = existing && typeof existing === "object" ? existing : {};
  if (!result.hooks || typeof result.hooks !== "object") result.hooks = {};
  if (!Array.isArray(result.hooks.Stop)) result.hooks.Stop = [];
  const alreadyInstalled = result.hooks.Stop.some((group) =>
    Array.isArray(group?.hooks) && group.hooks.some((handler) => handler?.type === "command" && handler.command === command)
  );
  if (!alreadyInstalled) {
    result.hooks.Stop.push({
      hooks: [{
        type: "command",
        command,
        async: true,
        timeout: 5,
        statusMessage: "发送小米手环通知"
      }]
    });
  }
  return result;
}

export async function install(options, env = process.env) {
  const host = required(options, "host");
  const token = required(options, "token");
  const stateDir = join(configRoot(env), "xiaomi-band-codex-notify");
  const statePath = join(stateDir, "config.json");
  const hookPath = join(stateDir, "stop-hook.mjs");
  const codexHome = env.CODEX_HOME || join(homedir(), ".codex");
  const hooksPath = join(codexHome, "hooks.json");
  const sourceHook = join(dirname(fileURLToPath(import.meta.url)), "stop-hook.mjs");

  await mkdir(stateDir, { recursive: true });
  await mkdir(codexHome, { recursive: true });
  await writeFile(statePath, JSON.stringify({ host, port: options.port || "8787", token }, null, 2) + "\n", { mode: 0o600 });
  await chmod(statePath, 0o600);
  await writeFile(hookPath, await readFile(sourceHook));

  let existing = {};
  try { existing = JSON.parse(await readFile(hooksPath, "utf8")); } catch { /* first install */ }
  const command = hookCommand(hookPath);
  const merged = mergeHookConfig(existing, command);
  await writeFile(hooksPath, JSON.stringify(merged, null, 2) + "\n", { mode: 0o600 });
  await chmod(hooksPath, 0o600);
  return { hooksPath, statePath, hookPath, command };
}

if (import.meta.url === `file://${process.argv[1]}`) {
  install(parseArgs(process.argv.slice(2)))
    .then(({ hooksPath }) => {
      console.log(`已安装 Codex Stop hook: ${hooksPath}`);
      console.log("请在 Codex 中执行 /hooks，审核并信任“发送小米手环通知”。");
    })
    .catch((error) => {
      console.error(`安装失败: ${error.message}`);
      process.exitCode = 1;
    });
}
