# AgentVoice

AgentVoice is a native Android voice layer for AI agents.

The current primary connector target is Hermes. AgentVoice is not a Hermes-only app: OpenClaw remains available as a manual connector path, and the architecture is designed as a cross-agent, business-safe communication layer for mobile workers, agent power users, and businesses that need hands-free capture and review workflows.

Core principle:

Capture now. Confirm later.

## MVP Goal

The first MVP should prove this loop:

Voice in -> AgentVoice relay -> agent response -> spoken output -> local history.

The MVP includes:

- Native Android app.
- Push-to-talk speech input.
- Text-to-speech replies.
- Relay backend with mock connector.
- Safety modes.
- Queued action display.
- Local conversation history.

The MVP does not include wake-word detection, always-on listening, Android Accessibility Service, phone screen control, payments, iOS, or a business dashboard.

## Tech Stack

Android:

- Kotlin
- Jetpack Compose
- Material 3
- Android SpeechRecognizer
- Android TextToSpeech
- Ktor Client
- Room
- DataStore

Relay backend:

- Node.js
- TypeScript
- Fastify
- Zod
- Pino

Later:

- Next.js web landing page and dashboard surfaces
- Supabase Auth and Postgres
- Stripe billing

## Repository Layout

```text
apps/
  android/
services/
  relay/
docs/
```

This repository treats `/Users/scott/Dev/agent-voice-chat` as the AgentVoice monorepo root.

## Local Development

Relay commands live in `services/relay/`:

```bash
npm install
npm run dev
npm run typecheck
npm run build
npm test
```

Android commands live in `apps/android/`:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

This local shell currently needs JDK 17 for Android builds:

```bash
JAVA_HOME=/Users/scott/Library/Java/JavaVirtualMachines/azul-17.0.19/Contents/Home ./gradlew assembleDebug
```

The default Android backend URL is `http://10.0.2.2:3001`, which works for the Android emulator talking to a relay on the host machine. A physical phone will usually need the computer's LAN IP address instead.

## Current Status

Stages 0 through 6 are implemented:

- Product architecture and safety docs.
- Monorepo structure.
- Fastify TypeScript relay with mock connector.
- Hermes connector behind the connector abstraction, with OpenClaw preserved as a manual connector.
- Native Android Compose app.
- Push-to-talk SpeechRecognizer loop.
- Android TextToSpeech replies.
- Ktor relay client.
- DataStore-backed settings.
- Room-backed local interaction history.
- Safety modes and queued action display.

## Safety Note

AgentVoice is built for safe capture and review. Risky actions should be queued with explicit confirmation rather than executed automatically, especially in mobile, capture-only, or review-required modes.
