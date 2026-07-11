import type { FastifyInstance } from "fastify";
import { z } from "zod";
import type { RunCoordinator, RunEvent } from "../runs/RunCoordinator.js";

const Params = z.object({ runId: z.string().trim().min(1) });
const EventsQuery = z.object({ after: z.coerce.number().int().nonnegative().default(0) });

export function registerRunsRoute(server: FastifyInstance, runs: RunCoordinator): void {
  server.get("/api/runs/:runId", async (request, reply) => {
    const params = Params.safeParse(request.params);
    if (!params.success) return reply.status(400).send({ error: "runId is required" });
    const run = runs.getRun(params.data.runId);
    return run ? reply.send(run) : reply.status(404).send({ error: "Run not found" });
  });

  server.post("/api/runs/:runId/cancel", async (request, reply) => {
    const params = Params.safeParse(request.params);
    if (!params.success) return reply.status(400).send({ error: "runId is required" });
    const run = runs.cancel(params.data.runId);
    return run ? reply.send(run) : reply.status(404).send({ error: "Run not found" });
  });

  server.get("/api/runs/:runId/events", async (request, reply) => {
    const params = Params.safeParse(request.params);
    const query = EventsQuery.safeParse(request.query);
    if (!params.success || !query.success) return reply.status(400).send({ error: "Invalid run events request" });
    if (!runs.getRun(params.data.runId)) return reply.status(404).send({ error: "Run not found" });

    reply.raw.writeHead(200, {
      "Content-Type": "text/event-stream",
      "Cache-Control": "no-cache, no-transform",
      Connection: "keep-alive"
    });
    reply.hijack();
    for (const event of runs.bufferedEvents(params.data.runId, query.data.after)) writeRunEvent(reply.raw, event);
    const unsubscribe = runs.subscribe(params.data.runId, (event) => writeRunEvent(reply.raw, event));
    const heartbeat = setInterval(() => {
      reply.raw.write(`event: heartbeat\ndata: ${JSON.stringify({ serverTime: new Date().toISOString() })}\n\n`);
    }, 30_000);
    request.raw.on("close", () => {
      clearInterval(heartbeat);
      unsubscribe();
    });
  });
}

function writeRunEvent(stream: NodeJS.WritableStream, event: RunEvent): void {
  stream.write(`id: ${event.sequence}\n`);
  stream.write(`event: ${event.type}\n`);
  stream.write(`data: ${JSON.stringify({ ...event.data, timestamp: event.timestamp })}\n\n`);
}

