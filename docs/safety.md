# AgentVoice Safety Model

AgentVoice is built around one principle:

Capture now. Confirm later.

The product should help users capture intent while mobile without silently executing risky actions.

## Modes

### normal

The default mode for ordinary interactions. The agent can reply with normal detail, but risky actions still require confirmation.

### mobile

For hands-free or away-from-desk use. Replies should be short, spoken-friendly, and focused on capture or summary. Risky actions should be queued for review.

### capture_only

For situations where the user wants to record intent without taking action. The system should capture notes, reminders, drafts, and tasks, then queue anything requiring review.

### review_required

For high-caution contexts. The relay and connectors should assume actions need explicit later confirmation unless they are clearly harmless.

## Safe Action Categories

Usually safe to capture or draft:

- Notes
- Reminders
- Draft messages
- Draft emails
- Task descriptions
- Short summaries
- Follow-up ideas

Even these should be stored or queued in a way the user can review.

## Risky Action Rules

AgentVoice should not automatically:

- Send emails or messages without confirmation.
- Delete files or data.
- Make payments.
- Make purchases.
- Run destructive commands.
- Change sensitive business records.
- Update shared systems in a way that affects other people.
- Perform risky actions while the user is mobile or driving.

Risky actions must be queued with `requiresConfirmation: true`.

## Queued Actions

Queued actions must include:

- Stable ID
- Type
- Human-readable summary
- Confirmation requirement
- Status

Allowed statuses:

- queued
- confirmed
- cancelled
- blocked

Queued actions are not the same as executed actions. They are pending review items.

## Mobile And Driving-Safe Principles

AgentVoice should:

- Prefer short spoken responses.
- Avoid long lists while the user is mobile.
- Capture intent without forcing screen attention.
- Make microphone activity clear.
- Fail safely when speech recognition is uncertain.
- Queue tasks for later review.

AgentVoice should not:

- Encourage complex screen interaction while driving.
- Auto-confirm risky actions.
- Hide recording state.
- Use always-on listening in the MVP.
- Use wake-word detection in the MVP.

## Connector Safety

Connectors must preserve the generic AgentVoice safety shape. If a connector receives a request that would execute a risky action, it should return a queued action or warning instead of silently executing it.

The relay should treat connector-specific failures as typed errors and return clear warnings without exposing secrets.

