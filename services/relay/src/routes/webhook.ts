import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { tokensMatch } from "../auth/relayAuth.js";
import { RelayInbox } from "../db/inbox.js";
import type { PushNotifier } from "../push/PushNotifier.js";
import { createRequestId } from "../utils/requestId.js";

const WebhookSchema = z.object({
  eventId: z.string().trim().min(1).max(200).optional(),
  conversationId: z.string().trim().min(1),
  content: z.string().trim().min(1),
  source: z.string().trim().min(1).default("cron"),
  metadata: z.record(z.string(), z.unknown()).default({})
});

export function registerWebhookRoute(
  server: FastifyInstance,
  inbox: RelayInbox,
  push?: PushNotifier
): void {
  server.post("/api/webhook/receive", async (request, reply) => {
    const requestId = createRequestId();
    const expectedToken = process.env.AGENTVOICE_WEBHOOK_TOKEN?.trim();

    if (!expectedToken) {
      return reply.status(503).send({
        error: "AgentVoice webhook token is not configured.",
        requestId
      });
    }

    if (!tokensMatch(request.headers["x-relay-token"], expectedToken)) {
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

    const existing = parsed.data.eventId
      ? inbox.findByExternalEventId(parsed.data.eventId)
      : null;
    const message = existing ?? inbox.createMessage({
      conversationId: parsed.data.conversationId,
      externalEventId: parsed.data.eventId,
      direction: "outgoing",
      content: parsed.data.content,
      source: parsed.data.source,
      status: "pending",
      metadata: parsed.data.metadata
    });

    if (!existing && push) {
      try {
        await push.notify({
          threadId: message.conversationId,
          messageId: message.id,
          title: "Solara",
          body: message.content,
          source: message.source
        });
      } catch (error) {
        server.log.warn(
          { error, messageId: message.id, threadId: message.conversationId },
          "Webhook message stored but push delivery failed"
        );
      }
    }

    return reply.status(201).send({
      id: message.id,
      status: "queued",
      conversationId: message.conversationId,
      duplicate: Boolean(existing),
      serverTime: new Date().toISOString()
    });
  });
}
