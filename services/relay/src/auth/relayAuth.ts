import { timingSafeEqual } from "node:crypto";
import type { FastifyInstance } from "fastify";
import { createRequestId } from "../utils/requestId.js";

// Health stays open for probes; the webhook checks its own X-Relay-Token.
const PUBLIC_ROUTES = new Set(["/health", "/api/webhook/receive"]);

export function relayToken(): string | undefined {
  return process.env.AGENTVOICE_RELAY_TOKEN?.trim() || undefined;
}

export function tokensMatch(provided: unknown, expected: string): boolean {
  if (typeof provided !== "string") {
    return false;
  }
  const providedBytes = Buffer.from(provided);
  const expectedBytes = Buffer.from(expected);
  return providedBytes.length === expectedBytes.length && timingSafeEqual(providedBytes, expectedBytes);
}

export function isLoopbackHost(host: string): boolean {
  return host === "127.0.0.1" || host === "::1" || host === "localhost";
}

/**
 * When AGENTVOICE_RELAY_TOKEN is set, every route except the public ones needs
 * `Authorization: Bearer <token>`. Unset keeps the relay open, as before.
 */
export function registerRelayAuth(server: FastifyInstance): void {
  server.addHook("onRequest", async (request, reply) => {
    const expectedToken = relayToken();
    if (!expectedToken || PUBLIC_ROUTES.has(request.routeOptions.url ?? "")) {
      return;
    }

    const authorization = request.headers.authorization;
    const provided = authorization?.startsWith("Bearer ")
      ? authorization.slice("Bearer ".length).trim()
      : undefined;

    if (!tokensMatch(provided, expectedToken)) {
      return reply.status(401).send({
        error: "Unauthorized",
        requestId: createRequestId()
      });
    }
  });
}
