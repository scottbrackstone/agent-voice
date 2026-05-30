# AgentVoice Architecture

AgentVoice is a voice-first communication layer for AI agents. The product is not an OpenClaw-only voice app. OpenClaw is the first connector, while the long-term architecture supports multiple agent backends such as OpenClaw, Hermes, Telegram bots, webhooks, and custom business agents.

The product principle is: Capture now. Confirm later.

## System Overview

AgentVoice has three main layers:

1. Native Android app
2. AgentVoice relay backend
3. Agent connector implementations

The Android app captures speech, turns it into text, sends structured requests to the relay, displays responses and queued actions, reads safe responses aloud, and stores local history.

The relay owns backend validation, request IDs, connector selection, response shaping, safety-aware behavior, and future integration with cloud services.

Connectors translate the generic AgentVoice message contract into provider-specific agent calls. OpenClaw lives behind this connector boundary.

## Android App Responsibilities

The Android app is responsible for:

- Push-to-talk speech capture using Android SpeechRecognizer in the MVP.
- Text-to-speech playback using Android TextToSpeech.
- Displaying transcript, response, mode, queued actions, connection status, and recent history.
- Letting the user select safety modes: normal, mobile, capture_only, and review_required.
- Sending typed and spoken messages to the relay using the generic AgentVoice API.
- Storing local settings and local conversation history.
- Making microphone state obvious and user-triggered.

The Android app must not:

- Store raw audio by default.
- Include service-role keys or backend secrets.
- Know OpenClaw protocol details.
- Execute risky actions directly.
- Use wake-word or always-on listening in the MVP.

## Relay Backend Responsibilities

The relay backend is responsible for:

- Exposing health and message endpoints.
- Validating request bodies.
- Assigning request IDs.
- Selecting the requested connector.
- Applying common response and safety conventions.
- Returning reply text, status, mode, queued actions, warnings, connector, and request ID.
- Keeping provider-specific protocols out of the Android app.
- Holding future server-side secrets and agent tokens.
- Supporting future long-running connector behavior such as WebSocket clients.

The relay is intentionally separate from the Android app so agent protocols, credentials, routing, logging, and safety behavior can evolve without shipping a new mobile client for every backend change.

## OpenClaw As A Connector

OpenClaw is connector number one, not the product identity.

This keeps AgentVoice positioned as a reusable voice layer for agents. The connector boundary lets the relay support OpenClaw first while preserving room for Hermes, generic webhooks, Telegram bots, and custom business agents.

The Android app sends:

```json
{
  "agent": "mock",
  "message": "What should I work on next?",
  "mode": "normal"
}
```

It should not know whether the relay calls OpenClaw over HTTP, WebSocket, a local gateway, or a future hosted service.

## Android To Relay Flow

1. User taps Talk.
2. Android SpeechRecognizer captures speech after explicit user action.
3. Android converts speech to a transcript.
4. Android sends the transcript to `POST /api/message`.
5. Relay validates the request and chooses a connector.
6. Connector returns a generic AgentResponse.
7. Android displays the reply and queued actions.
8. Android reads the reply aloud if TTS is enabled.
9. Android saves the interaction to local history.

## Relay To Agent Connector Flow

The relay calls a connector through a generic interface:

```text
AgentConnector.sendMessage(input) -> AgentResponse
```

The connector receives generic fields such as agent, message, mode, conversation ID, timestamp, and request ID. It returns a generic AgentResponse with reply text, status, mode, queued actions, warnings, connector, and request ID.

Provider-specific details remain inside connector implementations.

## Why Supabase Later

Supabase is useful later for authentication, user profiles, workspaces, conversation sync, queued actions, audit logs, and waitlist data.

It is intentionally not part of the local MVP because the first milestone should prove the phone-side loop:

Voice in -> relay -> agent response -> spoken output -> local history.

No Supabase service-role key should ever be shipped inside the Android app.

## Why Vercel Later

Vercel is a good fit later for the public web landing page, waitlist UI, and dashboard-style surfaces. It is not assumed to be the long-running relay host.

Persistent relay processes and future WebSocket connector clients may need a host designed for long-running services, such as Railway, Fly.io, Render Web Service, or a VPS.

## Long-Running Relay Hosting

The relay may eventually maintain persistent connections to agent gateways. Serverless request/response functions are not always a suitable fit for persistent WebSocket sessions or connector processes.

The architecture should therefore allow the relay to run as a standard Node.js service locally during development and on a long-running host later.

