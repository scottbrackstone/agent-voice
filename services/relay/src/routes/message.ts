import type { FastifyInstance } from "fastify";
import type { AgentConnector } from "../connectors/AgentConnector.js";
import { AgentMessageRequestSchema, type AgentName } from "../types/agent.js";
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

