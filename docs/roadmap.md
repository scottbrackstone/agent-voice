# AgentVoice Roadmap

This roadmap keeps AgentVoice focused on a safe voice layer for agents, not a single-agent client.

## Stage 0 - Product And Architecture Setup

- Confirm project location and repo state.
- Create or verify `.gitignore`.
- Create architecture, roadmap, safety, privacy, threat model, and testing docs.
- Define connectors as replaceable relay adapters, with Hermes as the current primary connector.

## Stage 1 - Monorepo Setup

- Create the root monorepo structure.
- Add `apps/android/`, `services/relay/`, and `docs/`.
- Add root README.
- Initialize git if needed.

## Stage 2 - Relay Backend With Mock Connector

- Build a Fastify TypeScript relay.
- Add health and message endpoints.
- Add Zod validation.
- Add generic connector interface.
- Add MockConnector.
- Add connector placeholders without inventing provider protocol details.
- Add basic tests.

## Stage 3 - Android App Scaffold

- Create native Android Kotlin project.
- Add Jetpack Compose and Material 3.
- Add main UI scaffold with fake state.
- Add settings placeholder.
- Add clean package structure for UI, viewmodel, data, network, voice, storage, and model.

## Stage 4 - Local Voice Loop On Android

- Add microphone permission.
- Add push-to-talk SpeechRecognizer flow.
- Add local fake response.
- Add TextToSpeech playback.
- Handle permission denial and speech errors.
- Release speech and TTS resources correctly.

## Stage 5 - Android To Relay Connection

- Add Ktor client.
- Add RelayClient.
- Add DataStore settings for backend URL, connector, default mode, and TTS.
- Send transcripts to `POST /api/message`.
- Display relay replies, queued actions, and errors.

## Stage 6 - Local History And Safety Modes

- Add Room local history.
- Save transcript, reply, mode, connector, status, queued action summary, timestamp, and errors.
- Display recent interactions.
- Add local clear history option.
- Make safety modes visible and send mode to relay.

## Stage 7 - Real Agent Connectors

- Add Hermes relay config through environment variables.
- Keep OpenClaw available as a manual connector path.
- Keep the Android app unaware of provider-specific protocol details.

## Stage 8 - Personal Real-World Testing Build

- Improve loading, retry, and error states.
- Add connection test.
- Add speak again, replay response, copy transcript, and copy reply.
- Add typed-message fallback.
- Add history detail.
- Add internal debug view.
- Create a 7-day personal test plan.

## Stage 9 - Mobile Access Features

- Add notification action that opens AgentVoice.
- Add Quick Settings tile.
- Investigate Bluetooth or earbud triggers.
- Add foreground service only if truly needed and compliant.
- Keep microphone use user-triggered and visible.

## Stage 10 - Web Landing Page And Waitlist

- Add Next.js landing page.
- Explain the voice-first agent layer.
- Include waitlist form.
- Include safety and privacy positioning.

## Stage 11 - Supabase Auth And Database

- Add auth, profiles, workspaces, workspace members, agent connection metadata, conversations, messages, queued actions, audit logs, and usage events.
- Enable RLS.
- Keep service-role keys out of the Android app.

## Stage 12 - External Beta

- Support a small group of testers.
- Add simple onboarding and connector selection.
- Consider a generic webhook connector for beta.
- Add logs, feedback, and beta disclaimer.

## Stage 13 - Business Pilot Workflows

- Support first business workflows such as meeting follow-up capture, daily summaries, and call notes.
- Add workspace, team, audit, action review, and permission foundations.
- Do not auto-confirm risky actions.

## Stage 14 - Billing

- Add Stripe only after value is validated.
- Support personal and business billing models.
- Keep Stripe secrets out of the Android app.

## Stage 15 - Production Hardening

- Add monitoring, structured logs, auth, rate limits, privacy policy, terms, deletion flow, release signing, deployment docs, and production acceptance criteria.
- Preserve least privilege and auditability.

## Stage 16 - Advanced Features

- Consider improved STT and TTS providers.
- Consider wake word only after privacy and security design.
- Explore Android assistant role and Android Auto where compliant.
- Add multiple agents, business templates, white-label options, and connector SDK ideas.

