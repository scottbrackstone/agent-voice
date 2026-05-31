# PC Codex Handoff

This file gives a fresh Codex session on the OpenClaw PC enough context to continue AgentVoice setup.

## Current Product State

AgentVoice is a native Android voice layer for AI agents. It is not an OpenClaw-only app. OpenClaw is connector number one behind the relay connector interface.

Implemented so far:

- Android app in `apps/android`
- Relay backend in `services/relay`
- Mock connector working end to end
- Android push-to-talk transcription
- Android TextToSpeech
- Android DataStore settings
- Android Room local history
- Safety modes and queued action display
- OpenClaw connector registered behind `AgentConnector`
- Clear local-dev HTTP support for Android LAN testing

Reference docs:

- `docs/architecture.md`
- `docs/safety.md`
- `docs/privacy.md`
- `docs/testing.md`
- `docs/openclaw-connector.md`

## Important Current Limitation

The OpenClaw connector is intentionally not pretending to send real messages yet.

Local inspection on the Mac only found this OpenCLAW Companion contract:

- `GET /events`
- `POST /feedback`

Those endpoints are not enough for AgentVoice voice command -> agent reply.

The missing piece is the actual OpenClaw message Gateway API, for example something like:

```text
POST /message
body: { "message": "...", "mode": "mobile" }
response: { "reply": "..." }
```

Do not invent the protocol. Inspect the PC/OpenClaw setup and wire only the real endpoint.

## PC Setup

Clone the repo on the PC:

```powershell
git clone https://github.com/scottbrackstone/agent-voice.git
cd agent-voice
```

Install relay dependencies:

```powershell
cd services\relay
npm install
```

Run relay on the PC:

```powershell
$env:OPENCLAW_ENABLED="true"
$env:OPENCLAW_GATEWAY_URL="http://localhost:YOUR_OPENCLAW_PORT"
$env:HOST="0.0.0.0"
$env:PORT="3001"
npm run dev
```

Then set Android app:

```text
Backend URL = http://PC_IP_OR_TAILSCALE_IP:3001
Connector = openclaw
```

Windows Firewall may need an inbound allow rule for port `3001`.

## What To Inspect On The PC

Find the OpenClaw service/Gateway details:

- process name and command
- listening ports
- local config files
- HTTP routes or WebSocket protocol
- auth/token requirements
- message endpoint request shape
- message endpoint response shape

Useful checks:

```powershell
netstat -ano | findstr LISTENING
```

Try known companion endpoint if relevant:

```powershell
curl http://localhost:YOUR_OPENCLAW_PORT/events
```

Then search the OpenClaw files for likely route names:

```powershell
findstr /S /I "message chat prompt gateway events feedback" *.*
```

## Backend Validation Commands

From `services\relay`:

```powershell
npm run typecheck
npm run build
npm test
```

Current Mac validation before handoff:

- `npm run typecheck` passed
- `npm run build` passed
- `npm test` passed, 8/8 tests

## Suggested Skills

- `diagnose` if the PC relay or OpenClaw connectivity fails.
- `github:github` if repository or commit context is needed.
- `zoom-out` if the agent needs to re-orient around the product architecture.

