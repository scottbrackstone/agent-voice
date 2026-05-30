# AgentVoice Threat Model

AgentVoice handles spoken user intent, agent responses, queued actions, and eventually business workflow data. The system should assume user messages and connector responses can contain sensitive or malicious content.

## Prompt Injection

Risk:

An agent, webpage, email, or user-provided content may include instructions that try to override safety rules or execute unintended actions.

Mitigations:

- Keep risky actions queued for confirmation.
- Separate user-visible reply text from action payloads.
- Add server-side validation for queued action types.
- Treat connector output as untrusted until validated.
- Preserve audit logs when cloud workspaces are added.

## Over-Permissioned Agents

Risk:

An agent connector may have access to too many tools or systems.

Mitigations:

- Use least-privilege connector credentials.
- Keep connector tokens server-side.
- Require confirmation for destructive or external side-effect actions.
- Add workspace-level permissions before business rollout.

## Shared Agent Or Workspace Risks

Risk:

Business users may share an agent or workspace. One user could expose data or trigger actions that affect others.

Mitigations:

- Add workspace membership and role checks before cloud business use.
- Use row-level security.
- Record audit logs for queued and confirmed actions.
- Avoid shared connector tokens across unrelated businesses.

## Stolen Phone

Risk:

An attacker with the unlocked phone could view local history or send commands.

Mitigations:

- Provide local history deletion.
- Avoid storing connector secrets locally in the MVP.
- Use Android secure storage for sensitive tokens if added later.
- Consider app lock or biometric gate for business versions.

## Leaked Token

Risk:

Backend or connector tokens could be leaked through source control, logs, mobile storage, or crash reports.

Mitigations:

- Do not commit `.env` files.
- Keep `.env.example` free of real secrets.
- Do not put service-role keys in Android.
- Redact secrets from logs.
- Use token rotation and revocation in production.

## Voice Misrecognition

Risk:

Speech-to-text may misunderstand the user and create the wrong intent.

Mitigations:

- Show transcript before or alongside action results.
- Queue risky actions for review.
- Keep mobile responses short and confirm what was captured.
- Store transcript history for review.

## Accidental Command Execution

Risk:

The user may speak casually and the system may interpret it as a command.

Mitigations:

- Do not auto-execute risky actions.
- Use capture and review modes.
- Mark queued actions as pending confirmation.
- Make action status visible in the UI.

## Backend And Connector Failures

Risk:

Agent backends may timeout, return invalid data, or expose unexpected protocol errors.

Mitigations:

- Validate relay request and response shapes.
- Return typed errors and warnings.
- Preserve request IDs.
- Keep MockConnector working as a test baseline.
- Do not fake OpenClaw behavior before protocol details are known.

