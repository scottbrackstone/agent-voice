# AgentVoice Privacy Principles

AgentVoice should be private by default, especially because voice interfaces often capture sensitive personal and business context.

## Microphone Use

In the MVP, the microphone is active only after the user presses Talk.

AgentVoice does not include:

- Always-on listening
- Wake-word detection
- Background microphone capture
- Hidden recording

The app should show clear UI state while listening.

## Raw Audio Storage

AgentVoice should not store raw audio by default.

The MVP uses Android SpeechRecognizer to convert speech to text and stores the transcript, reply, mode, connector, status, queued action summary, timestamp, and errors in local history.

If raw audio storage is ever added, it should require explicit user opt-in, clear retention controls, and a documented reason.

## Local History

The MVP stores conversation history locally on the device.

Local history should be user-controlled. The app should provide a clear way to delete local history.

The local database should avoid storing secrets.

## Future Cloud Storage

Cloud sync may be added later through Supabase after the local MVP is useful.

Future cloud storage must include:

- User authentication
- Workspace-level access control
- Row-level security
- Clear retention settings
- User deletion controls
- No service-role secrets in the Android app

## Agent Credentials

Connector tokens and agent credentials should not be hardcoded and should not live in source control.

For business use, credentials should be stored server-side with least privilege and revocation support.

## Retention Control

Users should be able to:

- Delete local history.
- Understand whether sync is enabled.
- Disable future sync if cloud history is added.
- Request export and deletion before production rollout.

