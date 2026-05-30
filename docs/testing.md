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
