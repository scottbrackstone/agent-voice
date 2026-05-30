import type { FastifyInstance } from "fastify";

export function registerHealthRoute(server: FastifyInstance): void {
  server.get("/health", async () => ({
    ok: true,
    service: "agentvoice-relay"
  }));
}

