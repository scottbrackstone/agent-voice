# OpenClaw Connector

Stage 7 keeps OpenClaw behind the AgentVoice relay connector interface. The Android app still talks only to AgentVoice Relay.

## Local Inspection Findings

I inspected the local environment for OpenClaw docs/config/repos.

Found:

- `/Users/scott/Dev/Portfolio/OpenclawTamagotchi`
- `SPEC.md`
- `app/src/main/java/com/openclaw/companion/data/api/OpenCLAWApi.kt`
- `app/src/main/java/com/openclaw/companion/data/repository/OpenCLAWRepository.kt`

The available local contract is for an OpenCLAW Companion/Tamagotchi app:

- `GET /events`
- `POST /feedback`

That contract is useful for receiving "did good" events and sending pet/treat/not_good feedback, but it does not define how to send a user message to an OpenClaw agent or how to receive an agent reply.

No running OpenClaw/Gateway process was found in the local process list.

## Current Connector Behavior

`OpenClawConnector` is registered behind the existing `AgentConnector` interface:

```text
AgentConnector.sendMessage(input) -> AgentResponse
```

It now:

- Reads environment-based config.
- Validates that the connector is enabled.
- Validates the Gateway URL.
- Optionally probes the known companion `GET /events` endpoint when enabled.
- Returns a clear failed `AgentResponse` when the message protocol is missing.
- Keeps `MockConnector` untouched.

It does not invent an OpenClaw message protocol.

## Relay Environment Variables

```bash
OPENCLAW_ENABLED=false
OPENCLAW_GATEWAY_URL=
OPENCLAW_TOKEN=
OPENCLAW_TIMEOUT_MS=10000
```

`OPENCLAW_TOKEN` is accepted for future authenticated Gateway support. If present, the current probe sends it as a bearer token. The actual message auth contract still needs confirmation.

## What Is Still Needed

To make AgentVoice talk to a real OpenClaw agent, provide or implement an OpenClaw Gateway message contract with:

- Base Gateway URL.
- Auth method.
- Token/header shape, if required.
- Message endpoint path.
- Request JSON shape.
- Response JSON shape.
- Timeout behavior.
- Error response shape.
- Whether the protocol is HTTP request/response, WebSocket, or something else.

AgentVoice needs a contract that can carry at least:

```json
{
  "message": "string",
  "mode": "normal | mobile | capture_only | review_required",
  "conversationId": "optional string",
  "requestId": "string"
}
```

And return enough data to build:

```json
{
  "reply": "string",
  "status": "completed | queued | failed | partial",
  "queuedActions": [],
  "warnings": []
}
```

The relay can then map that provider response into the generic AgentVoice `AgentResponse`.

## Current Test Command

With no message protocol available, this should return a clear failure:

```bash
curl -X POST http://localhost:3001/api/message \
  -H "Content-Type: application/json" \
  -d '{"agent":"openclaw","message":"What should I do next?","mode":"normal"}'
```

