import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { RelayInbox, messageToWire } from "../db/inbox.js";
import { createRequestId } from "../utils/requestId.js";

const QuerySchema = z.object({
  conversationId: z.string().trim().min(1),
  since: z.string().datetime().optional(),
  limit: z.coerce.number().int().positive().max(100).optional()
});

const HistoryParamsSchema = z.object({
  conversationId: z.string().trim().min(1)
});

const HistoryQuerySchema = z.object({
  before: z.string().datetime().optional(),
  limit: z.coerce.number().int().positive().max(100).optional()
});

const AckParamsSchema = z.object({
  id: z.string().trim().min(1)
});

export function registerMessagesRoute(server: FastifyInstance, inbox: RelayInbox): void {
  server.get("/api/messages", async (request, reply) => {
    const requestId = createRequestId();
    const parsed = QuerySchema.safeParse(request.query);

    if (!parsed.success) {
      return reply.status(400).send({
        error: "conversationId is required",
        requestId
      });
    }

    const messages = inbox.listMessages(parsed.data.conversationId, {
      since: parsed.data.since,
      limit: parsed.data.limit,
      pendingOnly: true
    });

    return reply.send({
      conversationId: parsed.data.conversationId,
      messages: messages.map(messageToWire),
      hasMore: messages.length === (parsed.data.limit ?? 20),
      serverTime: new Date().toISOString()
    });
  });

  server.get("/api/threads/:conversationId/messages", async (request, reply) => {
    const requestId = createRequestId();
    const parsedParams = HistoryParamsSchema.safeParse(request.params);
    const parsedQuery = HistoryQuerySchema.safeParse(request.query);

    if (!parsedParams.success || !parsedQuery.success) {
      return reply.status(400).send({
        error: "Invalid thread history request",
        requestId
      });
    }

    if (!inbox.hasConversation(parsedParams.data.conversationId)) {
      return reply.status(404).send({
        error: "Thread not found",
        requestId
      });
    }

    const messages = inbox.history(parsedParams.data.conversationId, parsedQuery.data);

    return reply.send({
      conversationId: parsedParams.data.conversationId,
      messages: messages.map(messageToWire),
      hasMore: messages.length === (parsedQuery.data.limit ?? 20),
      oldestMessageId: messages.at(0)?.id ?? null
    });
  });

  server.get("/api/threads/:conversationId/pending", async (request, reply) => {
    const requestId = createRequestId();
    const parsedParams = HistoryParamsSchema.safeParse(request.params);

    if (!parsedParams.success) {
      return reply.status(400).send({
        error: "conversationId is required",
        requestId
      });
    }

    const pending = inbox.listMessages(parsedParams.data.conversationId, {
      pendingOnly: true,
      limit: 100
    });

    return reply.send({
      conversationId: parsedParams.data.conversationId,
      pending: pending.map(messageToWire),
      serverTime: new Date().toISOString()
    });
  });

  server.post("/api/messages/:id/ack", async (request, reply) => {
    const requestId = createRequestId();
    const parsedParams = AckParamsSchema.safeParse(request.params);

    if (!parsedParams.success) {
      return reply.status(400).send({
        error: "message id is required",
        requestId
      });
    }

    const updated = inbox.ackMessage(parsedParams.data.id);
    if (!updated) {
      return reply.status(404).send({
        error: "Message not found",
        requestId
      });
    }

    return reply.send({
      id: parsedParams.data.id,
      status: "delivered",
      serverTime: new Date().toISOString()
    });
  });
}
