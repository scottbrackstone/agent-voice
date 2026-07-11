import type { FastifyInstance } from "fastify";
import { z } from "zod";
import type { HermesApi } from "../connectors/HermesApiClient.js";
import { HermesApiError } from "../connectors/HermesApiClient.js";
import { messageToWire, type RelayInbox } from "../db/inbox.js";
import type { RunCoordinator } from "../runs/RunCoordinator.js";
import { AgentModeSchema } from "../types/agent.js";
import { createRequestId } from "../utils/requestId.js";

const ThreadParams = z.object({ sessionId: z.string().trim().min(1) });
const CreateThreadBody = z.object({ title: z.string().trim().min(1).max(100).default("New chat") });
const RenameThreadBody = z.object({ title: z.string().trim().min(1).max(100) });
const HistoryQuery = z.object({
  before: z.coerce.number().positive().optional(),
  limit: z.coerce.number().int().positive().max(100).default(50)
});
const SendBody = z.object({
  clientMessageId: z.string().trim().min(1).max(200),
  content: z.string().trim().min(1).max(8000),
  mode: AgentModeSchema.default("normal"),
  inputType: z.enum(["text", "voice"]).default("text")
});

export function registerThreadsRoute(
  server: FastifyInstance,
  hermes: HermesApi,
  store: RelayInbox,
  runs: RunCoordinator
): void {
  server.get("/api/threads", async (_request, reply) => {
    try {
      const registered = store.listRegisteredThreads();
      if (registered.length === 0) return reply.send({ threads: [] });
      const sessions = await hermes.listSessions();
      const byId = new Map(sessions.map((session) => [session.id, session]));
      return reply.send({
        threads: registered.map((thread) => {
          const session = byId.get(thread.sessionId);
          return {
            id: thread.sessionId,
            title: session?.title ?? thread.title,
            createdAt: session?.startedAt ?? thread.createdAt,
            updatedAt: session?.endedAt ?? thread.updatedAt,
            messageCount: session?.messageCount ?? 0
          };
        })
      });
    } catch (error) {
      return sendHermesError(reply, error);
    }
  });

  server.post("/api/threads", async (request, reply) => {
    const parsed = CreateThreadBody.safeParse(request.body);
    if (!parsed.success) return reply.status(400).send({ error: "Invalid thread title", requestId: createRequestId() });
    try {
      const session = await hermes.createSession(parsed.data.title);
      store.registerThread(session.id, session.title);
      return reply.status(201).send({
        id: session.id,
        title: session.title,
        createdAt: session.startedAt ?? Date.now() / 1000,
        updatedAt: session.endedAt ?? session.startedAt ?? Date.now() / 1000,
        messageCount: session.messageCount ?? 0
      });
    } catch (error) {
      return sendHermesError(reply, error);
    }
  });

  server.patch("/api/threads/:sessionId", async (request, reply) => {
    const params = ThreadParams.safeParse(request.params);
    const body = RenameThreadBody.safeParse(request.body);
    if (!params.success || !body.success) return reply.status(400).send({ error: "Invalid rename request", requestId: createRequestId() });
    if (!store.getRegisteredThread(params.data.sessionId)) return reply.status(404).send({ error: "Thread not found", requestId: createRequestId() });
    try {
      const session = await hermes.renameSession(params.data.sessionId, body.data.title);
      store.registerThread(session.id, session.title);
      return reply.send({ id: session.id, title: session.title });
    } catch (error) {
      return sendHermesError(reply, error);
    }
  });

  server.get("/api/threads/:sessionId/messages", async (request, reply) => {
    const params = ThreadParams.safeParse(request.params);
    const query = HistoryQuery.safeParse(request.query);
    if (!params.success || !query.success) return reply.status(400).send({ error: "Invalid history request", requestId: createRequestId() });
    if (!store.getRegisteredThread(params.data.sessionId)) {
      if (!store.hasConversation(params.data.sessionId)) {
        return reply.status(404).send({ error: "Thread not found", requestId: createRequestId() });
      }
      const beforeIso = query.data.before
        ? new Date(query.data.before * 1000).toISOString()
        : undefined;
      const messages = store.history(params.data.sessionId, {
        before: beforeIso,
        limit: query.data.limit
      });
      return reply.send({
        threadId: params.data.sessionId,
        conversationId: params.data.sessionId,
        messages: messages.map(messageToWire),
        hasMore: messages.length === query.data.limit,
        oldestMessageId: messages.at(0)?.id ?? null
      });
    }
    try {
      const messages = await hermes.listMessages(params.data.sessionId, query.data);
      return reply.send({
        threadId: params.data.sessionId,
        messages: messages.map((message) => ({
          id: message.id,
          role: message.role,
          content: message.content,
          timestamp: message.timestamp,
          finishReason: message.finishReason,
          toolName: message.toolName
        })),
        hasMore: messages.length === query.data.limit
      });
    } catch (error) {
      return sendHermesError(reply, error);
    }
  });

  server.post("/api/threads/:sessionId/messages", async (request, reply) => {
    const params = ThreadParams.safeParse(request.params);
    const body = SendBody.safeParse(request.body);
    if (!params.success || !body.success) return reply.status(400).send({ error: "Invalid message request", requestId: createRequestId() });
    if (!store.getRegisteredThread(params.data.sessionId)) return reply.status(404).send({ error: "Thread not found", requestId: createRequestId() });
    const result = runs.start({
      sessionId: params.data.sessionId,
      clientMessageId: body.data.clientMessageId,
      message: body.data.content,
      mode: body.data.mode
    });
    return reply.status(result.created ? 202 : 200).send({
      runId: result.run.id,
      threadId: result.run.sessionId,
      clientMessageId: result.run.clientMessageId,
      status: result.run.status,
      duplicate: !result.created
    });
  });
}

function sendHermesError(reply: { status(code: number): { send(value: unknown): unknown } }, error: unknown): unknown {
  const requestId = createRequestId();
  if (error instanceof HermesApiError) {
    return reply.status(error.statusCode).send({ error: error.message, detail: error.detail, requestId });
  }
  return reply.status(502).send({ error: error instanceof Error ? error.message : "Hermes request failed.", requestId });
}
