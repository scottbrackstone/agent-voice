import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { RelayInbox } from "../db/inbox.js";
import { createRequestId } from "../utils/requestId.js";

const WebhookSchema = z.object({
  conversationId: z.string().trim().min(1),
  content: z.string().trim().min(1),
  source: z.string().trim().min(1).default("cron"),
  metadata: z.record(z.unknown()).default({})
});

export function registerWebhookRoute(server: FastifyInstance, inbox: RelayInbox): void {
  server.post("/api/webhook/receive", async (request, reply) => {
    const requestId = createRequestId();
    const expectedToken = process.env.AGENTVOICE_WEBHOOK_TOKEN?.trim();

    if (!expectedToken) {
      return reply.status(503).send({
        error: "AgentVoice webhook token is not configured.",
        requestId
      });
    }

    if (request.headers["x-relay-token"] !== expectedToken) {
      return reply.status(401).send({
        error: "Unauthorized",
        requestId
      });
    }

    const parsed = WebhookSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.status(400).send({
        error: parsed.error.issues.some((issue) => issue.path.includes("conversationId"))
          ? "conversationId is required"
          : "Invalid webhook body",
        requestId
      });
    }

    const message = inbox.createMessage({
      conversationId: parsed.data.conversationId,
      direction: "outgoing",
      content: parsed.data.content,
      source: parsed.data.source,
      status: "pending",
      metadata: parsed.data.metadata
    });

    return reply.status(201).send({
      id: message.id,
      status: "queued",
      conversationId: message.conversationId,
      serverTime: new Date().toISOString()
    });
  });
}
