# AgentVoice Relay

The AgentVoice relay is a TypeScript Fastify service that gives the Android app one stable API while keeping provider-specific agent protocols behind connector implementations.

Hermes is the current primary connector, but the relay is intentionally generic. OpenClaw remains available as a manual connector path.

## Commands

Requires Node.js 22.5+ because the scheduled-message inbox uses Node's built-in SQLite module.

```bash
npm install
npm run dev
npm run typecheck
npm run build
npm test
```

## Access token

Set `AGENTVOICE_RELAY_TOKEN` whenever the relay listens on anything other than `127.0.0.1` (the default `HOST` is `0.0.0.0`). Every route except `/health` and the webhook then needs the token, and the relay logs a warning at startup if it is bound wide without one.

```bash
export AGENTVOICE_RELAY_TOKEN="$(openssl rand -hex 32)"

curl "http://localhost:3001/api/messages?conversationId=thread-abc123" \
  -H "Authorization: Bearer $AGENTVOICE_RELAY_TOKEN"
```

The Android app sends the same token when `agentvoice.relayToken` is set in `apps/android/local.properties`. The `curl` examples below omit the header for brevity.

## Endpoints

Health:

```bash
curl http://localhost:3001/health
```

Message:

```bash
curl -X POST http://localhost:3001/api/message \
  -H "Content-Type: application/json" \
  -d '{"agent":"mock","message":"What should I work on next?","mode":"normal"}'
```

Mobile capture:

```bash
curl -X POST http://localhost:3001/api/message \
  -H "Content-Type: application/json" \
  -d '{"agent":"hermes","message":"Remind me to test Hermes tomorrow","mode":"mobile"}'
```

Scheduled message webhook:

```bash
export AGENTVOICE_WEBHOOK_TOKEN="$(openssl rand -hex 32)"

curl -X POST http://localhost:3001/api/webhook/receive \
  -H "Content-Type: application/json" \
  -H "X-Relay-Token: $AGENTVOICE_WEBHOOK_TOKEN" \
  -d '{
    "conversationId":"thread-abc123",
    "content":"Ping from Solara",
    "source":"cron",
    "metadata":{"cronJobName":"One minute test"}
  }'
```

Poll pending messages:

```bash
curl "http://localhost:3001/api/messages?conversationId=thread-abc123"
```

ACK a displayed message:

```bash
curl -X POST http://localhost:3001/api/messages/msg_123/ack
```

## Safety Shape

Every successful connector response should include:

- `reply`
- `status`
- `mode`
- `queuedActions`
- `warnings`
- `connector`
- `requestId`

Risky actions should be returned as queued actions for later confirmation.
