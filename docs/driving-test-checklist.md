# AgentVoice Driving Test Checklist

Use this checklist for parked or passenger-seat testing before trying AgentVoice during real driving.

## Before Starting

- Confirm the relay is reachable from the phone.
- Confirm Settings > Test Connection succeeds.
- Set Driving mode default to Mobile for the first test.
- Keep Auto-read replies enabled.
- Keep Keep screen awake enabled.
- Confirm the phone has microphone permission.

## Parked Test

- Open AgentVoice and tap the car icon.
- Tap Start hands-free.
- Say: "Jynx, give me a short driving test reply."
- Confirm the app shows Listening, Thinking, Speaking, then returns to listening.
- Tap Interrupt while Jynx is speaking and confirm speech stops quickly.
- After Interrupt, say a new message and confirm it sends.
- Say "stop hands free" while the app is listening and confirm the session stops.

## Jynx Trigger Test

- Turn on Settings > Driving behavior > Require Jynx trigger.
- Start hands-free.
- Say a normal sentence without Jynx and confirm it is ignored.
- Say: "Jynx, give me a short test reply" and confirm it sends.
- Say only "Jynx" and wait for the app to listen again.
- Say a request without Jynx and confirm that one request sends.
- Say "Jynx, repeat that" and confirm the last reply is spoken again.
- Say "Jynx, capture this check the oil tomorrow" and confirm it is queued as capture-only.
- Say "Jynx, review mode" and confirm the driving mode changes.

## Voxtral Turn-Based STT Test

- Add `MISTRAL_API_KEY` to the relay `.env`.
- Restart the AgentVoice relay.
- Open Settings and turn on Use Voxtral transcription.
- Start Driving Mode hands-free.
- Confirm the status shows Recording, then Transcribing, then Thinking.
- Say: "Hey Jynx, give me a short test reply."
- Stop speaking and confirm Recording ends after a short silence rather than waiting for the full timeout.
- Confirm the transcript is cleaner than Android speech recognition.
- Tap Talk while Recording and confirm it stops the recording early.
- Turn Use Voxtral transcription off and confirm Android speech recognition still works.

## Local Queue Confirmation Test

- Use Capture only or say: "Jynx, capture this check the tyres tomorrow."
- Confirm the queued action appears in Driving Mode.
- Say: "Jynx, read queued actions" and confirm Jynx reads the pending queue.
- Say: "Jynx, confirm" and confirm the queued action changes to confirmed locally.
- Capture another item.
- Say: "Jynx, cancel" and confirm the queued action changes to cancelled locally.
- Confirm neither command executes an external action.

## Session Summary Test

- Start hands-free and complete two short turns.
- Trigger at least one ignored utterance with Require Jynx trigger enabled.
- Stop hands-free from the button or say "stop hands free."
- Confirm the session summary shows turns, queued actions, confirmed actions, cancelled actions, ignored utterances, and recoveries.

## Recovery Test

- Start hands-free and stay quiet.
- Confirm the app recovers from "No speech was detected" and listens again.
- Temporarily stop the relay, speak a request, and confirm the app shows a clear relay error without leaving driving mode.
- Restart the relay and confirm Start hands-free works again.

## Capture-Only Test

- Set Driving mode default to Capture only.
- Start hands-free and say: "Remind me to check the tyres tomorrow."
- Confirm the reply is the local capture confirmation.
- Confirm OpenClaw is not called for the capture-only request.
- Confirm the captured action appears as queued for later review.

## Real Drive Test

- Start with a short familiar route.
- Use only Mobile mode or Capture only.
- Keep the phone mounted and visible.
- Do not troubleshoot while moving.
- If the app feels stuck, tap Stop hands-free and test again later.
