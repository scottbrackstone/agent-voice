import type { AgentMode, QueuedAction, QueuedActionType } from "../types/agent.js";

const ACTION_PATTERNS: Array<{ type: QueuedActionType; pattern: RegExp }> = [
  { type: "reminder", pattern: /\b(remind me|reminder|tomorrow|later)\b/i },
  { type: "draft_email", pattern: /\b(draft|email)\b/i },
  { type: "draft_message", pattern: /\b(message|text|sms|reply to)\b/i },
  { type: "task", pattern: /\b(task|todo|to-do|follow up|follow-up)\b/i },
  { type: "note", pattern: /\b(note|log|capture|remember)\b/i }
];

export function isCaptureMode(mode: AgentMode): boolean {
  return mode === "mobile" || mode === "capture_only" || mode === "review_required";
}

export function buildQueuedAction(
  message: string,
  mode: AgentMode,
  createId: () => string
): QueuedAction | null {
  if (!isCaptureMode(mode)) {
    return null;
  }

  const actionType = detectActionType(message);

  if (!actionType) {
    return null;
  }

  return {
    id: createId(),
    type: actionType,
    summary: summarizeAction(message),
    requiresConfirmation: true,
    status: "queued"
  };
}

export function buildCaptureOnlyAction(message: string, createId: () => string): QueuedAction {
  const actionType = detectActionType(message) ?? "note";

  return {
    id: createId(),
    type: actionType,
    summary: summarizeAction(message),
    requiresConfirmation: true,
    status: "queued"
  };
}

function detectActionType(message: string): QueuedActionType | null {
  const match = ACTION_PATTERNS.find((candidate) => candidate.pattern.test(message));
  return match?.type ?? null;
}

function summarizeAction(message: string): string {
  const trimmed = message
    .trim()
    .replace(/^please\s+/i, "")
    .replace(/^capture\s+/i, "")
    .replace(/^remind me to\s+/i, "")
    .replace(/^can you\s+/i, "")
    .replace(/^could you\s+/i, "");

  if (!trimmed) {
    return "Captured action";
  }

  return trimmed.charAt(0).toUpperCase() + trimmed.slice(1);
}

