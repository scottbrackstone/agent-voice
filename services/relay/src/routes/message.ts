import type { FastifyInstance } from "fastify";
import type { AgentConnector } from "../connectors/AgentConnector.js";
import type { RelayInbox } from "../db/inbox.js";
import { buildCaptureOnlyAction } from "../safety/modeRules.js";
import {
  AgentMessageRequestSchema,
  type AgentMessageRequest,
  type AgentName,
  type AgentResponse
} from "../types/agent.js";
import { createRequestId } from "../utils/requestId.js";

type ConnectorRegistry = Partial<Record<AgentName, AgentConnector>>;

export function registerMessageRoute(
  server: FastifyInstance,
  connectors: ConnectorRegistry,
  inbox?: RelayInbox
): void {
  server.post("/api/message", async (request, reply) => {
    const requestId = createRequestId();
    const parsed = AgentMessageRequestSchema.safeParse(request.body);

    if (!parsed.success) {
      return reply.code(400).send({
        error: "Invalid request body",
        requestId,
        issues: parsed.error.issues.map((issue) => ({
          path: issue.path.join("."),
          message: issue.message
        }))
      });
    }

    if (parsed.data.mode === "capture_only") {
      const response = buildCaptureOnlyResponse(parsed.data, requestId);
      recordExchange(inbox, parsed.data, response, requestId);
      return reply.send(response);
    }

    const connector = connectors[parsed.data.agent];

    if (!connector) {
      return reply.code(400).send({
        reply: `Connector '${parsed.data.agent}' is not available in this relay build.`,
        status: "failed",
        mode: parsed.data.mode,
        queuedActions: [],
        warnings: [`Connector '${parsed.data.agent}' is not registered.`],
        connector: parsed.data.agent,
        requestId
      });
    }

    const response = await connector.sendMessage({
      ...parsed.data,
      requestId
    });

    recordExchange(inbox, parsed.data, response, requestId);
    return reply.send(response);
  });
}

function buildCaptureOnlyResponse(
  request: AgentMessageRequest,
  requestId: string
): AgentResponse {
  const queuedAction = buildCaptureOnlyAction(request.message, () => `${requestId}_capture`);

  return {
    reply: "Captured for later review.",
    status: "queued",
    mode: "capture_only",
    queuedActions: [queuedAction],
    warnings: [
      `Capture-only mode did not call the ${request.agent} connector.`
    ],
    connector: "relay",
    requestId
  };
}

function recordExchange(
  inbox: RelayInbox | undefined,
  request: AgentMessageRequest,
  response: AgentResponse,
  requestId: string
): void {
  if (!inbox || !request.conversationId) {
    return;
  }

  const timestamp = request.clientTimestamp ?? new Date().toISOString();
  inbox.createMessage({
    conversationId: request.conversationId,
    direction: "incoming",
    content: request.message,
    source: "user",
    status: "delivered",
    timestamp,
    metadata: { requestId }
  });
  inbox.createMessage({
    conversationId: request.conversationId,
    direction: "outgoing",
    content: response.reply,
    source: response.connector,
    status: "delivered",
    metadata: {
      requestId,
      warnings: response.warnings,
      queuedActions: response.queuedActions
    }
  });
}
