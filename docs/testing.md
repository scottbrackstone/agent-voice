# AgentVoice Testing Guide

This document defines the testing checkpoints for the early AgentVoice MVP.

## Stage 0 Checkpoint

Stage 0 has no feature code. Confirm:

- Project location.
- Repo state.
- `.gitignore` exists.
- Core docs exist.
- No secrets were added.

## Relay Backend Commands

Run from `services/relay/` after Stage 2 exists:

```bash
npm install
npm run typecheck
npm run build
npm test
```

Expected early endpoints:

```bash
curl http://localhost:3001/health
```

Expected response:

```json
{
  "ok": true,
  "service": "agentvoice-relay"
}
```

Message example:

```bash
curl -X POST http://localhost:3001/api/message \
  -H "Content-Type: application/json" \
  -d '{"agent":"mock","message":"What should I work on next?","mode":"normal"}'
```

Mobile capture example:

```bash
curl -X POST http://localhost:3001/api/message \
  -H "Content-Type: application/json" \
  -d '{"agent":"mock","message":"Remind me to test OpenClaw tomorrow","mode":"mobile"}'
```

## Android Build Commands

Run from `apps/android/` after Stage 3 exists:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

In this local shell, set JDK 17 explicitly if `java -version` points at Java 11:

```bash
JAVA_HOME=/Users/scott/Library/Java/JavaVirtualMachines/azul-17.0.19/Contents/Home ./gradlew assembleDebug
JAVA_HOME=/Users/scott/Library/Java/JavaVirtualMachines/azul-17.0.19/Contents/Home ./gradlew testDebugUnitTest
```

If the Android SDK or Gradle cannot run in the current environment, report that clearly and do not claim a successful build.

## Manual Phone Test Checklist

Use this checklist after Stages 4 through 6:

- App opens.
- Tap Talk.
- Microphone permission prompt appears if needed.
- Speaking a sentence produces a transcript.
- Fake local response appears in Stage 4.
- Relay response appears in Stage 5.
- TTS reads the response when enabled.
- Turning TTS off prevents spoken playback.
- Backend offline errors are clear.
- Normal, mobile, capture_only, and review_required modes can be selected.
- Mobile or capture-only reminder-style messages create queued actions through the mock relay.
- Recent history persists after app restart.
- Clear history removes local entries.

## Stage 8 And 9 Phone Test Checklist

Use this checklist after installing the personal testing build:

- Settings Test Connection succeeds against the relay URL.
- Mock test prompt returns a mock reply.
- OpenClaw test prompt reaches OpenClaw through the relay.
- Typed message fallback sends and saves to history.
- Copy transcript and copy reply work from the current response.
- Speak again works when TTS is enabled and stays disabled when TTS is off.
- Settings debug status shows app version, last connector, last request, and last error.
- Driving mode opens from the car icon and uses the large Talk button.
- The driving shortcut notification can be shown and hidden from Settings.
- The notification Talk action opens AgentVoice and starts a visible hands-free session after microphone permission is granted.
- The Quick Settings tile can be added manually, then opens AgentVoice and starts a visible hands-free session.
- Start hands-free in Driving mode, speak, wait for the reply, and confirm it starts listening again.
- While Jynx is speaking in hands-free mode, tap Interrupt and confirm TTS stops and listening starts immediately.
- Stop hands-free and confirm TTS stops and the microphone is no longer listening.
- Let a hands-free session run until the 10-minute timeout and confirm it stops.
- Driving behavior settings persist after app restart: Start in Driving mode, Keep screen awake, and Auto-read replies.
- Driving mode default can be changed independently of the normal app mode.
- Driving mode keeps the screen awake only when that setting is enabled.
- Driving mode shows recent replies without needing to open full history.
- Long-pressing the launcher icon shows Talk to Jynx and Driving shortcuts.
- No wake word or always-on listening behavior is present.

## OpenClaw Connector Test Plan

Do not implement live OpenClaw behavior until protocol details are known.

Before Stage 7 implementation, identify:

- Gateway URL.
- Authentication method.
- Required token or credential shape.
- Request format.
- Response format.
- Timeout and error behavior.
- Whether the gateway is HTTP, WebSocket, or another protocol.

Test cases for Stage 7:

- Mock connector still works.
- OpenClaw disabled returns a clear typed error.
- Missing OpenClaw config returns a clear typed error.
- Invalid OpenClaw response is handled safely.
- Live OpenClaw message succeeds only when a real gateway is configured.

Stage 7 inspection found only the OpenCLAW Companion `GET /events` and `POST /feedback` contract. Until a user-message Gateway contract exists, OpenClaw requests should fail clearly rather than pretending to send messages.
