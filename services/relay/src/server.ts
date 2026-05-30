import Fastify, { type FastifyInstance, type FastifyServerOptions } from "fastify";
import { MockConnector } from "./connectors/MockConnector.js";
import { OpenClawConnector } from "./connectors/OpenClawConnector.js";
import { registerHealthRoute } from "./routes/health.js";
import { registerMessageRoute } from "./routes/message.js";

export type BuildServerOptions = Pick<FastifyServerOptions, "logger">;

export function buildServer(options: BuildServerOptions = {}): FastifyInstance {
  const server = Fastify({
    logger: options.logger ?? {
      level: process.env.LOG_LEVEL ?? "info"
    }
  });

  registerHealthRoute(server);
  registerMessageRoute(server, {
    mock: new MockConnector(),
    openclaw: new OpenClawConnector()
  });

  return server;
}

