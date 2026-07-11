# Hermes Thread Contract Request

AgentVoice now has a local threaded chat MVP. Remote Hermes thread/history sync is intentionally disabled until Hermes provides a real contract.

Send this prompt to Hermes before replacing the local cache with canonical remote sync:

```text
I am building AgentVoice, an Android text + voice client for Hermes. I need the exact API contract to replace my Telegram workflow.

Please provide:
1. How to list chat threads/conversations.
2. How to create a new thread.
3. How to rename/title a thread.
4. How to fetch messages for a thread.
5. How to send a text message to a specific thread.
6. Whether voice-transcribed text should use the same endpoint as typed text.
7. Every Telegram-style flag command I currently use, with examples.
8. Whether flags should be raw message text or structured metadata.
9. Auth method and required headers.
10. Base URL/path conventions.
11. Request/response JSON examples for success and failure.
12. Whether Hermes stores canonical history and thread titles.
13. Rate limits, timeouts, and long-running response behavior.
```

Current AgentVoice behavior:

- Android stores `ChatThread` and `ChatMessage` locally in Room.
- Typed messages and voice transcripts use the same relay `/api/message` flow.
- Android sends the selected local thread id as `conversationId`.
- Telegram-style flags are not parsed locally; the exact message text is sent to the relay.
- Hermes/OpenClaw receive only the current `/api/message` payload until a native thread contract exists.
