import { describe, expect, it } from "vitest";
import { derivePresentation } from "./derivePresentation.js";

describe("derivePresentation", () => {
  it("maps celebratory replies to a celebration motion", () => {
    expect(derivePresentation("Excellent work, you nailed it!")).toMatchObject({
      mood: "happy",
      state: "success",
      motionHint: "celebrate"
    });
  });

  it("maps failures to a reassuring concern state", () => {
    expect(derivePresentation("Sorry, I was unable to complete that request.")).toMatchObject({
      mood: "concerned",
      state: "error",
      motionHint: "reassure"
    });
  });

  it("uses a speaking neutral fallback for ordinary replies", () => {
    expect(derivePresentation("The meeting starts at ten.")).toMatchObject({
      mood: "neutral",
      state: "success",
      motionHint: "speak"
    });
  });
});
