#!/usr/bin/env node

import { readFile } from "node:fs/promises";
import { extname } from "node:path";
import { pathToFileURL } from "node:url";

const MAX_IMAGE_BYTES = 3 * 1024 * 1024;
const MAX_TITLE_LENGTH = 120;
const MAX_BODY_LENGTH = 4000;

function parseArgs(argv) {
  const [command = "help", ...rest] = argv;
  const options = {};
  for (let index = 0; index < rest.length; index += 1) {
    const item = rest[index];
    if (!item.startsWith("--")) continue;
    const key = item.slice(2);
    if (key === "help" || key === "json") {
      options[key] = true;
      continue;
    }
    const next = rest[index + 1];
    if (!next || next.startsWith("--")) throw new Error(`缺少参数值: --${key}`);
    options[key] = next;
    index += 1;
  }
  return { command, options };
}

function usage() {
  return `Dylan小米手环 Android bridge CLI

Usage:
  node bridge/bandctl.mjs health --host 192.168.1.23 --token TOKEN
  node bridge/bandctl.mjs notify --host 192.168.1.23 --token TOKEN \\
    --title "Codex" --body "任务完成"
  node bridge/bandctl.mjs plan --host 192.168.1.23 --token TOKEN \\
    --date 2026-08-20 --title "晨间跑步" --duration 20 \\
    --target "慢跑 3 公里" --note "完成后拉伸"

Environment:
  BAND_BRIDGE_HOST   Android phone LAN address
  BAND_BRIDGE_PORT   default: 8787
  BAND_BRIDGE_TOKEN  bearer token shown by the Android app
`;
}

function required(options, name) {
  const value = options[name];
  if (typeof value !== "string" || value.length === 0) throw new Error(`缺少必填参数: --${name}`);
  return value;
}

function bridgeUrl(options, env) {
  const host = options.host || env.BAND_BRIDGE_HOST;
  const port = options.port || env.BAND_BRIDGE_PORT || "8787";
  if (!host) throw new Error("缺少 Android 手机地址，请传 --host 或设置 BAND_BRIDGE_HOST。");
  return `http://${host.replace(/^https?:\/\//, "")}:${port}`;
}

function token(options, env) {
  const value = options.token || env.BAND_BRIDGE_TOKEN;
  if (!value) throw new Error("缺少 bridge token，请传 --token 或设置 BAND_BRIDGE_TOKEN。");
  return value;
}

function mimeType(filePath) {
  const extension = extname(filePath).toLowerCase();
  if (extension === ".jpg" || extension === ".jpeg") return "image/jpeg";
  if (extension === ".webp") return "image/webp";
  return "image/png";
}

async function encodeImage(filePath) {
  const buffer = await readFile(filePath);
  if (buffer.byteLength > MAX_IMAGE_BYTES) throw new Error("图片超过 3 MiB，请先压缩后再发送。");
  return { imageBase64: buffer.toString("base64"), imageMime: mimeType(filePath) };
}

function textValue(value, max, name) {
  if (typeof value !== "string" || value.length === 0) throw new Error(`缺少必填参数: --${name}`);
  return value.slice(0, max);
}

export function buildNotifyPayload(options, image = null) {
  const payload = {
    type: "notify",
    source: options.source || "desktop",
    title: textValue(options.title, MAX_TITLE_LENGTH, "title"),
    body: textValue(options.body, MAX_BODY_LENGTH, "body")
  };
  if (image) Object.assign(payload, image);
  return payload;
}

export function buildPlanPayload(options) {
  const date = textValue(options.date, 10, "date");
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) throw new Error("--date 必须是 YYYY-MM-DD。");
  const duration = Number(options.duration || 30);
  if (!Number.isInteger(duration) || duration < 1 || duration > 180) throw new Error("--duration 必须是 1 到 180 的整数。");
  const plan = {
    date,
    title: textValue(options.title, 80, "title"),
    duration,
    target: textValue(options.target, 160, "target"),
    note: (options.note || "").slice(0, 200),
    completed: options.completed === "true"
  };
  return {
    type: "plan",
    source: options.source || "desktop",
    title: plan.title,
    body: `${plan.date} · ${plan.duration} 分钟\\n目标：${plan.target}${plan.note ? `\\n备注：${plan.note}` : ""}`,
    plan
  };
}

export async function requestBridge({ baseUrl, token: bearer, path, method = "GET", body }) {
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${bearer}`,
      ...(body ? { "Content-Type": "application/json" } : {})
    },
    body: body ? JSON.stringify(body) : undefined
  });
  const raw = await response.text();
  let data;
  try { data = raw ? JSON.parse(raw) : {}; } catch { data = { raw }; }
  if (!response.ok) throw new Error(`Android bridge ${response.status}: ${data.error || raw || response.statusText}`);
  return data;
}

export async function run(argv, env = process.env) {
  const { command, options } = parseArgs(argv);
  if (command === "help" || options.help) {
    console.log(usage());
    return { ok: true };
  }
  const baseUrl = bridgeUrl(options, env);
  const bearer = token(options, env);
  let result;
  if (command === "health") {
    result = await requestBridge({ baseUrl, token: bearer, path: "/v1/health" });
  } else if (command === "notify") {
    const image = options.image ? await encodeImage(options.image) : null;
    result = await requestBridge({ baseUrl, token: bearer, path: "/v1/notify", method: "POST", body: buildNotifyPayload(options, image) });
  } else if (command === "plan") {
    result = await requestBridge({ baseUrl, token: bearer, path: "/v1/plan", method: "POST", body: buildPlanPayload(options) });
  } else {
    throw new Error(`未知命令: ${command}\\n\\n${usage()}`);
  }
  if (options.json) console.log(JSON.stringify(result, null, 2));
  else console.log(result.message || result.status || "ok");
  return result;
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  run(process.argv.slice(2)).catch((error) => {
    console.error(`bandctl: ${error.message}`);
    process.exitCode = 1;
  });
}
