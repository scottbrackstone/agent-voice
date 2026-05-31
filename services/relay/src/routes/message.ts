import type { FastifyInstance } from "fastify";
import type { AgentConnector } from "../connectors/AgentConnector.js";
import { buildCaptureOnlyAction } from "../safety/modeRules.js";
import {
  AgentMessageRequestSchema,
  type AgentMessageRequest,
  type AgentName,
  type AgentResponse
} from "../types/agent.js";
import { createRequestId } from "../utils/requestId.js";

type ConnectorRegistry = Partial<Record<AgentName, AgentConnector>>;

export function registerMessageRoute(server: FastifyInstance, connectors: ConnectorRegistry): void {
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
      return reply.send(buildCaptureOnlyResponse(parsed.data, requestId));
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

