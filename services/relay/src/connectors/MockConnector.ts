import { randomUUID } from "node:crypto";
import type { AgentConnector, AgentConnectorInput } from "./AgentConnector.js";
import { buildQueuedAction, isCaptureMode } from "../safety/modeRules.js";
import type { AgentResponse } from "../types/agent.js";

export class MockConnector implements AgentConnector {
  readonly name = "mock" as const;

  async sendMessage(input: AgentConnectorInput): Promise<AgentResponse> {
    const queuedAction = buildQueuedAction(input.message, input.mode, () => randomUUID());

    if (queuedAction) {
      return {
        reply: `Captured. I queued ${articleFor(queuedAction.type)} ${formatActionType(queuedAction.type)} for review.`,
        status: "queued",
        mode: input.mode,
        queuedActions: [queuedAction],
        warnings: [],
        connector: this.name,
        requestId: input.requestId
      };
    }

    if (isCaptureMode(input.mode)) {
      return {
        reply: "Captured.",
        status: "completed",
        mode: input.mode,
        queuedActions: [],
        warnings: [],
        connector: this.name,
        requestId: input.requestId
      };
    }

    return {
      reply: `Mock agent heard: ${input.message}`,
      status: "completed",
      mode: input.mode,
      queuedActions: [],
      warnings: [],
      connector: this.name,
      requestId: input.requestId
    };
  }
}

function formatActionType(type: string): string {
  return type.replace("_", " ");
}

function articleFor(type: string): string {
  return type === "draft_email" ? "an" : "a";
}

