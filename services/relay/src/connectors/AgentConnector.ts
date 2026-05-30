import type { AgentMessageRequest, AgentName, AgentResponse } from "../types/agent.js";

export type AgentConnectorInput = AgentMessageRequest & {
  requestId: string;
};

export interface AgentConnector {
  readonly name: AgentName;
  sendMessage(input: AgentConnectorInput): Promise<AgentResponse>;
}

