import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { RelayInbox } from "../db/inbox.js";
import { messageToWire } from "../db/inbox.js";

const EventsQuerySchema = z.object({
  conversationId: z.string().trim().min(1)
});

export function registerEventsRoute(server: FastifyInstance, inbox: RelayInbox): void {
  server.get("/api/events", async (request, reply) => {
    const parsed = EventsQuerySchema.safeParse(request.query);

    reply.raw.writeHead(200, {
      "Content-Type": "text/event-stream",
      "Cache-Control": "no-cache, no-transform",
      Connection: "keep-alive"
    });
    reply.hijack();

    if (!parsed.success) {
      writeEvent(reply.raw, "error", { error: "conversationId is required" });
      reply.raw.end();
      return;
    }

    const unsubscribe = inbox.onMessage(parsed.data.conversationId, (message) => {
      writeEvent(reply.raw, "message", {
        conversationId: message.conversationId,
        ...messageToWire(message)
      });
    });

    const heartbeat = setInterval(() => {
      writeEvent(reply.raw, "heartbeat", { serverTime: new Date().toISOString() });
    }, 30_000);

    request.raw.on("close", () => {
      clearInterval(heartbeat);
      unsubscribe();
    });

    writeEvent(reply.raw, "heartbeat", { serverTime: new Date().toISOString() });
  });
}

function writeEvent(
  stream: NodeJS.WritableStream,
  event: string,
  data: Record<string, unknown>
): void {
  stream.write(`event: ${event}\n`);
  stream.write(`data: ${JSON.stringify(data)}\n\n`);
}
