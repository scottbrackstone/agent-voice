# AgentVoice Personal Test Plan

Stage 8 is a personal real-world testing build. The goal is to learn whether AgentVoice is useful in normal daily capture, while keeping the product principle intact: capture now, confirm later.

Do not use this plan to add Stage 9 features. No wake word, always-on listening, Accessibility Service, payments, iOS, or business dashboard work belongs here.

## Setup

- Relay reachable from phone: `http://100.109.251.92:3001`.
- Hermes service reachable from the relay.
- Start relay when needed:

```bash
wsl.exe bash -lc "systemctl --user start agentvoice-relay.service"
```

- Stop relay when needed:

```bash
wsl.exe bash -lc "systemctl --user stop agentvoice-relay.service"
```

## Daily Baseline Checks

- Open the Android app.
- Open Settings and tap Test Connection.
- Confirm Hermes works through the relay.
- Confirm Mock still works.
- Send one typed message.
- Send one voice message.
- Turn TTS off and on.
- Use Speak again on a reply.
- Copy the transcript and reply.
- Open one history item and confirm detail is readable.
- Open Driving mode and confirm the large Talk button works.
- Start a hands-free session, complete two turns, then tap Stop.
- Interrupt the assistant mid-reply and confirm listening starts immediately.
- Add the Quick Settings tile and confirm it starts a visible hands-free session.
- Show the driving notification shortcut and confirm Talk starts a visible hands-free session.
- Toggle Start in Driving mode, Keep screen awake, and Auto-read replies.
- Long-press the launcher icon and test Talk and Driving shortcuts.

## Day 1 - First Pocket Test

- Use AgentVoice at home on Wi-Fi.
- Capture 10 short, low-risk thoughts.
- Include at least three typed messages.
- Include one message in each mode: normal, mobile, capture only, and review required.
- Note any confusing loading, error, retry, or history behavior.

## Day 2 - Network Edges

- Test on Wi-Fi, then mobile data if available.
- Stop the relay and confirm the app shows a clear failure.
- Restart the relay and use Test Connection before sending another message.
- Confirm Retry works after a failed message.

## Day 3 - Hermes Reality Check

- Select Hermes in Settings.
- Send five real prompts that you would normally give from your PC.
- Confirm Android never stores or asks for Hermes tokens.
- Compare phone replies with the gateway behavior you expect.
- Record any prompt types that need better mobile framing.

## Day 4 - Capture Later Review

- Use mobile or capture-only mode for reminders, notes, or follow-ups.
- Confirm queued actions are visible and clearly require later review.
- Copy at least one transcript and one reply into another app.
- Check that local history preserves useful context after app restart.

## Day 5 - Friction Hunt

- Use the app when walking around, sitting down, and switching apps.
- Try one noisy-room voice capture.
- Use typed fallback whenever speech recognition misses.
- Watch for any point where you hesitate because the next action is unclear.

## Day 6 - Failure And Recovery

- Test a bad backend URL.
- Test an unreachable relay.
- Test a blank typed message.
- Test TTS off, then Speak again after turning TTS back on.
- Test the driving shortcut notification permission flow.
- Test the Quick Settings tile from the shade.
- Confirm hands-free stops after a silence or speech error.
- Restart the app and confirm Start in Driving mode works only when enabled.
- Confirm the screen stays awake in Driving mode only when Keep screen awake is enabled.
- Change Driving mode default and confirm it does not change the normal app mode.
- Clear history and confirm the app returns to a clean state.

## Day 7 - Decision Pass

- Review the week's notes.
- Pick the top three fixes needed before Stage 9.
- Decide whether the current relay URL and connector settings are comfortable for continued personal use.
- Confirm Hermes connector, Mock connector, typed fallback, voice capture, TTS, copy actions, retry, history, Driving mode, Quick Settings tile, and notification shortcut all still work.

## Notes Template

For each issue, capture:

- Date and time.
- Connector: Hermes or Mock.
- Mode.
- Input type: voice or typed.
- Network: Wi-Fi or mobile data.
- What happened.
- What you expected.
- Whether Retry, Test Connection, or typed fallback helped.
