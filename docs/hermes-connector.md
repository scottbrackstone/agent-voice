# Hermes Connector

Hermes is connector #2 behind the AgentVoice Relay.

Android never stores Hermes tokens. The phone sends `agent: "hermes"` to the relay, and the relay reads Hermes configuration from its local environment.

## Relay Environment

Add these values to `services/relay/.env`:

```env
HERMES_ENABLED=true
HERMES_BASE_URL=http://127.0.0.1:18888
HERMES_TOKEN=
HERMES_MODEL=hermes
HERMES_USER=agentvoice-mobile
HERMES_TIMEOUT_MS=30000
```

The first implementation assumes Hermes exposes an OpenAI-compatible endpoint:

```text
POST /v1/chat/completions
```

If Hermes uses a different request or response shape, update `services/relay/src/connectors/HermesConnector.ts` without changing Android.

## Phone Test

- Restart the relay after changing `.env`.
- Open Android Settings.
- Select Hermes as the connector.
- Tap the Hermes test prompt.
- Confirm the reply says it came from Hermes.
