# Hermes + AgentVoice VPS Setup

This setup targets Hermes Agent 0.18.2 at commit
`f64e4f4f5768c18a53f44890747653bafcab2796`. AgentVoice uses Hermes session
IDs as canonical thread IDs. The relay and Hermes can share one VPS and remain
reachable from Android through Tailscale.

## 1. Relay Environment

Set these values in the relay service environment:

```dotenv
AGENT_CONNECTOR=hermes
HERMES_ENABLED=true
HERMES_BASE_URL=http://127.0.0.1:8642
HERMES_TOKEN=<same value as Hermes API_SERVER_KEY>
HERMES_TIMEOUT_MS=120000
AGENTVOICE_WEBHOOK_TOKEN=<one random 64-character hex value>
AGENTVOICE_INBOX_DB_PATH=/var/lib/agentvoice/agentvoice-inbox.sqlite
AGENTVOICE_FIREBASE_ENABLED=true
FIREBASE_PROJECT_ID=<Firebase project id>
GOOGLE_APPLICATION_CREDENTIALS=/etc/agentvoice/firebase-service-account.json
```

Generate the webhook token once with `openssl rand -hex 32`. Use the exact
same value in the Hermes environment below. Keep both secret files outside Git.

## 2. Firebase

1. Create a Firebase project and register an Android app with package
   `com.agentvoice.app`.
2. Put the downloaded `google-services.json` at
   `apps/android/app/google-services.json` before building the APK.
3. In Firebase, create a service-account JSON key for the relay VPS.
4. Install it at `/etc/agentvoice/firebase-service-account.json`, readable only
   by the relay service account.
5. Restart the relay. The Android app registers its FCM token automatically.

Without `google-services.json`, the app still builds and all foreground chat
features work, but background push delivery is disabled.

## 3. Hermes Plugin

On the VPS, from the AgentVoice checkout:

```bash
mkdir -p ~/.hermes/plugins/agentvoice-platform
cp integrations/hermes-agentvoice-platform/plugin.yaml ~/.hermes/plugins/agentvoice-platform/
cp integrations/hermes-agentvoice-platform/adapter.py ~/.hermes/plugins/agentvoice-platform/
cp integrations/hermes-agentvoice-platform/__init__.py ~/.hermes/plugins/agentvoice-platform/
```

Add to `~/.hermes/.env`:

```dotenv
AGENTVOICE_RELAY_URL=http://127.0.0.1:3001
AGENTVOICE_WEBHOOK_TOKEN=<same token as relay>
AGENTVOICE_HOME_CHANNEL=<optional default Hermes session id>
```

Restart and inspect Hermes:

```bash
systemctl --user restart hermes-gateway
systemctl --user status hermes-gateway --no-pager
tail -100 ~/.hermes/logs/gateway.log
```

The plugin registers `agentvoice` through Hermes' `PlatformEntry` registry and
provides the documented `standalone_sender_fn`, so both co-resident gateway
cron and standalone cron delivery work. It does not modify Telegram or CLI.

## 4. Verify Delivery

First copy a real session ID from `GET /api/threads` on the relay. Then test the
webhook directly on the VPS:

```bash
curl -i http://127.0.0.1:3001/api/webhook/receive \
  -H 'Content-Type: application/json' \
  -H 'X-Relay-Token: <shared token>' \
  -d '{"eventId":"manual-test-1","conversationId":"<session id>","content":"AgentVoice delivery test","source":"system","metadata":{"test":true}}'
```

The relay should return `201`, the phone should show a Solara notification, and
tapping it should open that session. Repeating the same `eventId` should return
`duplicate: true` and must not create a second notification.

For a Hermes scheduled job, set its existing `deliver` field to
`agentvoice:<session-id>`. Use the exact session ID returned by AgentVoice's
thread list. Hermes 0.18.2 does not provide a stable per-fire cron event ID, so
the adapter creates one delivery ID per send; the relay still deduplicates any
replayed webhook that carries the same ID.

## 5. Phone Connection

In AgentVoice Settings, set the backend URL to the relay's Tailscale address,
for example `http://<vps-tailscale-ip>:3001`. The phone must be signed into the
same tailnet. Hermes port `8642` does not need to be exposed to the phone.
