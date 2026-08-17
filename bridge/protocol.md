# Band Bridge Protocol v1

Android companion listens on `http://<phone-lan-ip>:8787` and requires:

```http
Authorization: Bearer <token>
Content-Type: application/json
```

## LAN pairing discovery

The Android app also listens on UDP `8788` for a LAN-only discovery packet. The
normal Plugin sends the four-digit code shown in the app, so users do not need
to copy an IP address or token:

```json
{"type":"discover","code":"4821"}
```

The phone responds only when the code matches, with its LAN host and HTTP
port. The Plugin then calls `POST /v1/pair` to exchange the code for the
private Bearer token. The four-digit code is a convenience credential for a
trusted LAN, not a strong password; the app limits responses and supports code
refresh.

## Health

```http
GET /v1/health
```

Response:

```json
{"ok":true,"service":"band10pro-bridge","protocol":1}
```

## Notify

```http
POST /v1/notify
```

```json
{
  "type": "notify",
  "source": "codex",
  "title": "Codex",
  "body": "任务完成",
  "imageBase64": "...optional...",
  "imageMime": "image/png"
}
```

The Android app publishes a standard Android notification. A successful HTTP response means the notification was posted on Android; it does not prove Mi Fitness or the physical band has delivered it.

## Plan

```http
POST /v1/plan
```

```json
{
  "type": "plan",
  "source": "codex",
  "title": "晨间跑步",
  "body": "2026-08-20 · 20 分钟\\n目标：慢跑 3 公里",
  "plan": {
    "date": "2026-08-20",
    "title": "晨间跑步",
    "duration": 20,
    "target": "慢跑 3 公里",
    "note": "完成后拉伸",
    "completed": false
  }
}
```

## Limits

- Request body: 4 MiB on Android; CLI image guard: 3 MiB.
- Title: 120 characters.
- Body: 4000 characters.
- The server is LAN-only and has no cloud relay.
