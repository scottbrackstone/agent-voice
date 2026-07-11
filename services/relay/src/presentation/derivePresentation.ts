import type { PresentationState } from "../connectors/HermesApiClient.js";

const containsAny = (text: string, words: string[]): boolean =>
  words.some((word) => text.includes(word));

export function derivePresentation(
  reply: string,
  fallback?: PresentationState
): PresentationState {
  const text = reply.toLowerCase();
  const emphasis = Math.min(
    1,
    0.25 + ((reply.match(/!/g)?.length ?? 0) * 0.16) + (reply.length > 500 ? 0.1 : 0)
  );

  if (containsAny(text, ["unable", "failed", "error", "warning", "careful", "concern", "sorry"])) {
    return {
      mood: "concerned",
      intensity: Math.max(0.45, emphasis),
      state: "error",
      motionHint: "reassure"
    };
  }

  if (containsAny(text, ["congrat", "nailed it", "excellent", "amazing", "great work", "proud of you"])) {
    return {
      mood: emphasis >= 0.6 ? "excited" : "happy",
      intensity: Math.max(0.55, emphasis),
      state: "success",
      motionHint: "celebrate"
    };
  }

  if (containsAny(text, ["haha", "hehe", "playful", "cheeky", "plot twist"])) {
    return {
      mood: "playful",
      intensity: Math.max(0.45, emphasis),
      state: "speaking",
      motionHint: "speak"
    };
  }

  if (containsAny(text, ["breathe", "take your time", "steady", "no rush", "you are okay"])) {
    return {
      mood: "calm",
      intensity: 0.35,
      state: "speaking",
      motionHint: "reassure"
    };
  }

  if (containsAny(text, ["plan", "steps", "checking", "investigat", "analysis", "here's what", "found"])) {
    return {
      mood: "focused",
      intensity: Math.max(0.35, emphasis),
      state: "success",
      motionHint: "speak"
    };
  }

  if (fallback && fallback.mood !== "neutral") return fallback;
  return {
    mood: "neutral",
    intensity: emphasis,
    state: "success",
    motionHint: "speak"
  };
}
