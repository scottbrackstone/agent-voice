# PC Codex Handoff

This file gives a fresh Codex session on the PC enough context to continue AgentVoice setup.

## Current Product State

AgentVoice is a native Android voice layer for AI agents. Hermes is the current primary connector behind the relay connector interface. OpenClaw remains available as a manual connector path.

Implemented so far:

- Android app in `apps/android`
- Relay backend in `services/relay`
- Mock connector working end to end
- Android push-to-talk transcription
- Android TextToSpeech
- Android DataStore settings
- Android Room local history
- Safety modes and queued action display
- Hermes connector registered behind `AgentConnector`
- OpenClaw connector preserved behind `AgentConnector`
- Clear local-dev HTTP support for Android LAN testing

Reference docs:

- `docs/architecture.md`
- `docs/safety.md`
- `docs/privacy.md`
- `docs/testing.md`
- `docs/hermes-connector.md`
- `docs/openclaw-connector.md`

## Current Hermes Integration Path

The Hermes connector assumes an OpenAI-compatible HTTP endpoint:

```text
POST /v1/chat/completions
```

Keep the Hermes token/password only in AgentVoice Relay environment variables. Do not put it in the Android app.

## Optional OpenClaw Integration Path

The OpenClaw connector now uses the documented OpenClaw Gateway OpenAI-compatible HTTP endpoint:

```text
POST /v1/chat/completions
```

The endpoint is disabled by default in OpenClaw. Enable it in OpenClaw config and restart the Gateway:

```powershell
openclaw config set gateway.http.endpoints.chatCompletions.enabled true
```

Keep the OpenClaw token/password only in AgentVoice Relay environment variables. Do not put it in the Android app.

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
$env:HERMES_ENABLED="true"
$env:HERMES_BASE_URL="http://localhost:YOUR_HERMES_PORT"
$env:HERMES_TOKEN="YOUR_HERMES_TOKEN_OR_PASSWORD"
$env:HERMES_MODEL="hermes"
$env:HOST="0.0.0.0"
$env:PORT="3001"
npm run dev
```

Then set Android app:

```text
Backend URL = http://PC_IP_OR_TAILSCALE_IP:3001
Connector = hermes
```

Windows Firewall may need an inbound allow rule for port `3001`.

## What To Inspect On The PC

Find or verify the Hermes service details:

- process name and command
- listening ports
- local config files
- HTTP chat completions endpoint enabled or equivalent Hermes route
- auth token/password requirements

Useful checks:

```powershell
netstat -ano | findstr LISTENING
```

Try the Hermes HTTP endpoint if enabled:

```powershell
curl http://localhost:YOUR_HERMES_PORT/v1/models
```

Then search the Hermes files for likely route names:

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

- `diagnose` if the PC relay or Hermes connectivity fails.
- `github:github` if repository or commit context is needed.
- `zoom-out` if the agent needs to re-orient around the product architecture.

