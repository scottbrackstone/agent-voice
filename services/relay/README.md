# AgentVoice Relay

The AgentVoice relay is a TypeScript Fastify service that gives the Android app one stable API while keeping provider-specific agent protocols behind connector implementations.

OpenClaw is the first planned real connector, but the relay is intentionally generic.

## Commands

```bash
npm install
npm run dev
npm run typecheck
npm run build
npm test
```

## Endpoints

Health:

```bash
curl http://localhost:3001/health
```

Message:

```bash
curl -X POST http://localhost:3001/api/message \
  -H "Content-Type: application/json" \
  -d '{"agent":"mock","message":"What should I work on next?","mode":"normal"}'
```

Mobile capture:

```bash
curl -X POST http://localhost:3001/api/message \
  -H "Content-Type: application/json" \
  -d '{"agent":"mock","message":"Remind me to test OpenClaw tomorrow","mode":"mobile"}'
```

## Safety Shape

Every successful connector response should include:

- `reply`
- `status`
- `mode`
- `queuedActions`
- `warnings`
- `connector`
- `requestId`

Risky actions should be returned as queued actions for later confirmation.

