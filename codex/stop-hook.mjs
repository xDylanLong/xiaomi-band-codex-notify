#!/usr/bin/env node

import { homedir } from "node:os";
import { join } from "node:path";
import { readFile } from "node:fs/promises";

const DEFAULT_PORT = "8787";
const MAX_BODY_LENGTH = 1200;

export function configPath(env = process.env) {
  const configRoot = env.XDG_CONFIG_HOME || (env.APPDATA ? join(env.APPDATA) : join(homedir(), ".config"));
  return env.XIAOMI_BAND_CODEX_CONFIG || join(configRoot, "xiaomi-band-codex-notify", "config.json");
}

export function buildNotification(input) {
  if (!input || input.hook_event_name !== "Stop" || input.stop_hook_active === true) return null;
  const summary = String(input.last_assistant_message || "任务已完成")
    .replace(/\s+$/u, "")
    .slice(0, MAX_BODY_LENGTH);
  return {
    type: "notify",
    source: "codex",
    title: "Codex 已完成",
    body: summary || "任务已完成，可以回来看结果了"
  };
}

async function readJsonStdin() {
  let value = "";
  for await (const chunk of process.stdin) value += chunk;
  return JSON.parse(value || "{}");
}

async function sendNotification(payload, config) {
  const host = String(config.host || "").replace(/^https?:\/\//u, "");
  const port = String(config.port || DEFAULT_PORT);
  const token = String(config.token || "");
  if (!host || !token) return;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 2500);
  try {
    await fetch(`http://${host}:${port}/v1/notify`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify(payload),
      signal: controller.signal
    });
  } finally {
    clearTimeout(timer);
  }
}

export async function runHook(stdinText, env = process.env) {
  const payload = buildNotification(JSON.parse(stdinText || "{}"));
  if (!payload) return { sent: false, reason: "not-a-final-stop" };
  let config;
  try {
    config = JSON.parse(await readFile(configPath(env), "utf8"));
  } catch {
    return { sent: false, reason: "missing-config" };
  }
  try {
    await sendNotification(payload, config);
    return { sent: true, payload };
  } catch {
    return { sent: false, reason: "bridge-unreachable" };
  }
}

if (import.meta.url === `file://${process.argv[1]}`) {
  readJsonStdin()
    .then((input) => runHook(JSON.stringify(input)))
    .then(() => process.exit(0))
    .catch(() => process.exit(0));
}
