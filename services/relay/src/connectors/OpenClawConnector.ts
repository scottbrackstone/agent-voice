import type { AgentConnector, AgentConnectorInput } from "./AgentConnector.js";
import type { AgentResponse } from "../types/agent.js";

type OpenClawConfig = {
  enabled: boolean;
  gatewayUrl?: string;
  token?: string;
};

export class OpenClawConnector implements AgentConnector {
  readonly name = "openclaw" as const;

  constructor(private readonly config: OpenClawConfig = readOpenClawConfig()) {}

  async sendMessage(input: AgentConnectorInput): Promise<AgentResponse> {
    if (!this.config.enabled) {
      return this.failure(input, "OpenClaw connector is disabled.", [
        "Set OPENCLAW_ENABLED=true only after the OpenClaw Gateway protocol is confirmed."
      ]);
    }

    if (!this.config.gatewayUrl) {
      return this.failure(input, "OpenClaw connector is missing a Gateway URL.", [
        "Set OPENCLAW_GATEWAY_URL in the relay environment."
      ]);
    }

    return this.failure(input, "OpenClaw connector protocol is not implemented yet.", [
      "Provide the OpenClaw Gateway auth method, request shape, response shape, and timeout/error behavior."
    ]);
  }

  private failure(input: AgentConnectorInput, reply: string, warnings: string[]): AgentResponse {
    return {
      reply,
      status: "failed",
      mode: input.mode,
      queuedActions: [],
      warnings,
      connector: this.name,
      requestId: input.requestId
    };
  }
}

function readOpenClawConfig(): OpenClawConfig {
  return {
    enabled: process.env.OPENCLAW_ENABLED === "true",
    gatewayUrl: process.env.OPENCLAW_GATEWAY_URL,
    token: process.env.OPENCLAW_TOKEN
  };
}

