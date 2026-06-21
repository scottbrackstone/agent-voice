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
- Say: "Hermes, give me a short driving test reply."
- Confirm the app shows Listening, Thinking, Speaking, then returns to listening.
- Tap Interrupt while the assistant is speaking and confirm speech stops quickly.
- After Interrupt, say a new message and confirm it sends.
- Say "stop hands free" while the app is listening and confirm the session stops.

## Trigger Phrase Test

- Turn on Settings > Driving behavior > Require trigger phrase.
- Start hands-free.
- Say a normal sentence without the trigger phrase and confirm it is ignored.
- Say: "Hermes, give me a short test reply" and confirm it sends.
- Say only "Hermes" and wait for the app to listen again.
- Say a request without the trigger phrase and confirm that one request sends.
- Say "Hermes, repeat that" and confirm the last reply is spoken again.
- Say "Hermes, capture this check the oil tomorrow" and confirm it is queued as capture-only.
- Say "Hermes, review mode" and confirm the driving mode changes.

## Voxtral Turn-Based STT Test

- Add `MISTRAL_API_KEY` to the relay `.env`.
- Restart the AgentVoice relay.
- Open Settings and turn on Use Voxtral transcription.
- Start Driving Mode hands-free.
- Confirm the status shows Recording, then Transcribing, then Thinking.
- Say: "Hey Hermes, give me a short test reply."
- Stop speaking and confirm Recording ends after about 2.5 seconds of silence rather than waiting for the full 60-second timeout.
- Record for longer than 12 seconds and confirm the app keeps recording until silence, tap-to-stop, or the 60-second cap.
- Confirm the transcript is cleaner than Android speech recognition.
- Tap Talk while Recording and confirm it stops the recording early.
- Turn Use Voxtral transcription off and confirm Android speech recognition still works.

## Local Queue Confirmation Test

- Use Capture only or say: "Hermes, capture this check the tyres tomorrow."
- Confirm the queued action appears in Driving Mode.
- Say: "Hermes, read queued actions" and confirm the assistant reads the pending queue.
- Say: "Hermes, confirm" and confirm the queued action changes to confirmed locally.
- Capture another item.
- Say: "Hermes, cancel" and confirm the queued action changes to cancelled locally.
- Confirm neither command executes an external action.

## Session Summary Test

- Start hands-free and complete two short turns.
- Trigger at least one ignored utterance with Require trigger phrase enabled.
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
- Confirm Hermes is not called for the capture-only request.
- Confirm the captured action appears as queued for later review.

## Real Drive Test

- Start with a short familiar route.
- Use only Mobile mode or Capture only.
- Keep the phone mounted and visible.
- Do not troubleshoot while moving.
- If the app feels stuck, tap Stop hands-free and test again later.
