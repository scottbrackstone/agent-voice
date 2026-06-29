# OpenClaw Connector

Stage 7 keeps OpenClaw behind the AgentVoice relay connector interface. The Android app still talks only to AgentVoice Relay.

## Local Inspection Findings

The Windows/WSL test machine has OpenClaw Gateway running as:

```text
openclaw gateway --port 18789
```

OpenClaw documents an OpenAI-compatible HTTP surface on the Gateway:

- `GET /v1/models`
- `POST /v1/chat/completions`
- `POST /v1/responses`

The chat completions endpoint is disabled by default and must be enabled in OpenClaw config before live AgentVoice use:

```bash
openclaw config set gateway.http.endpoints.chatCompletions.enabled true
```

The current Gateway auth mode is token-based, so AgentVoice Relay must hold the Gateway token server-side and send it as a bearer token. Do not put this token in the Android app.

## Current Connector Behavior

`OpenClawConnector` is registered behind the existing `AgentConnector` interface:

```text
AgentConnector.sendMessage(input) -> AgentResponse
```

It now:

- Reads environment-based config.
- Validates that the connector is enabled.
- Validates the Gateway URL.
- Sends user text to OpenClaw `POST /v1/chat/completions`.
- Maps the OpenAI-compatible assistant message into AgentVoice `AgentResponse.reply`.
- Sends an AgentVoice safety/system instruction with the selected mode.
- Keeps `MockConnector` untouched.

It does not expose the OpenClaw token to Android. Android still only talks to AgentVoice Relay.

## Relay Environment Variables

```bash
OPENCLAW_ENABLED=false
OPENCLAW_GATEWAY_URL=
OPENCLAW_TOKEN=
OPENCLAW_MODEL=openclaw
OPENCLAW_USER=agentvoice-mobile
OPENCLAW_SESSION_KEY=
OPENCLAW_AGENT_ID=
OPENCLAW_TIMEOUT_MS=10000
```

`OPENCLAW_TOKEN` is the OpenClaw Gateway token/password for HTTP bearer auth. `OPENCLAW_MODEL` should usually be `openclaw` or `openclaw/default`. Set `OPENCLAW_AGENT_ID` only if you want to force a specific OpenClaw agent. Set `OPENCLAW_SESSION_KEY` if you want all AgentVoice turns to share an explicit OpenClaw session.

## What Is Still Needed For Phone Testing

- Enable OpenClaw `gateway.http.endpoints.chatCompletions.enabled`.
- Start AgentVoice Relay on a LAN-reachable host and port, for example `0.0.0.0:3001`.
- Set the Android app backend URL to the PC LAN or tailnet address for AgentVoice Relay, not to OpenClaw directly.
- Select the OpenClaw connector in AgentVoice settings.

## Manual Gateway Checks

The OpenClaw endpoint should not return the Control UI HTML when enabled. With the token supplied, this should return JSON:

```bash
curl -sS http://127.0.0.1:18789/v1/chat/completions \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "openclaw",
    "user": "agentvoice-mobile",
    "messages": [
      { "role": "user", "content": "Say a short hello from OpenClaw." }
    ]
  }'
```

## AgentVoice Relay Test Command

With the relay running and OpenClaw env configured:

```bash
curl -X POST http://localhost:3001/api/message \
  -H "Content-Type: application/json" \
  -d '{"agent":"openclaw","message":"What should I do next?","mode":"normal"}'
```

Expected result:

- `status: "completed"` and a real OpenClaw reply, or
- a clear `failed` AgentVoice response explaining Gateway auth/config/endpoint errors.

