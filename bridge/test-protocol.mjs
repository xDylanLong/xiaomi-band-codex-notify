import assert from "node:assert/strict";
import { createServer } from "node:http";
import { buildNotifyPayload, buildPlanPayload, requestBridge, run } from "./bandctl.mjs";

const requests = [];
const server = createServer(async (request, response) => {
  let body = "";
  for await (const chunk of request) body += chunk;
  requests.push({ method: request.method, url: request.url, auth: request.headers.authorization, body: body ? JSON.parse(body) : null });
  response.setHeader("Content-Type", "application/json");
  if (request.headers.authorization !== "Bearer test-token") {
    response.writeHead(401);
    response.end(JSON.stringify({ error: "unauthorized" }));
    return;
  }
  response.end(JSON.stringify({ ok: true, message: "accepted" }));
});

await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
const port = server.address().port;
const baseUrl = `http://127.0.0.1:${port}`;

const notify = buildNotifyPayload({ source: "codex", title: "Codex", body: "done" });
assert.deepEqual(notify, { type: "notify", source: "codex", title: "Codex", body: "done" });
const plan = buildPlanPayload({ date: "2026-08-20", title: "Run", duration: "20", target: "3 km", note: "stretch" });
assert.equal(plan.plan.duration, 20);
const health = await requestBridge({ baseUrl, token: "test-token", path: "/v1/health" });
assert.equal(health.ok, true);
await requestBridge({ baseUrl, token: "test-token", path: "/v1/notify", method: "POST", body: notify });
assert.equal(requests.at(-1).auth, "Bearer test-token");
assert.equal(requests.at(-1).body.source, "codex");
const runHealth = await run(["health", "--host", "127.0.0.1", "--port", String(port), "--token", "test-token", "--json"]);
assert.equal(runHealth.ok, true);
await run(["notify", "--host", "127.0.0.1", "--port", String(port), "--token", "test-token", "--title", "Codex", "--body", "done"]);
assert.equal(requests.at(-1).auth, "Bearer test-token");
await assert.rejects(() => requestBridge({ baseUrl, token: "wrong", path: "/v1/health" }), /401/);

await new Promise((resolve) => server.close(resolve));
console.log("bridge protocol tests passed");
