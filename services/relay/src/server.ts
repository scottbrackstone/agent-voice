import Fastify, { type FastifyInstance, type FastifyServerOptions } from "fastify";
import { HermesConnector } from "./connectors/HermesConnector.js";
import { MockConnector } from "./connectors/MockConnector.js";
import { OpenClawConnector } from "./connectors/OpenClawConnector.js";
import { registerHealthRoute } from "./routes/health.js";
import { registerMessageRoute } from "./routes/message.js";
import { registerTranscribeRoute } from "./routes/transcribe.js";

export type BuildServerOptions = Pick<FastifyServerOptions, "logger">;

export function buildServer(options: BuildServerOptions = {}): FastifyInstance {
  const server = Fastify({
    logger: options.logger ?? {
      level: process.env.LOG_LEVEL ?? "info"
    }
  });

  registerHealthRoute(server);
  registerTranscribeRoute(server);
  registerMessageRoute(server, {
    mock: new MockConnector(),
    hermes: new HermesConnector(),
    openclaw: new OpenClawConnector()
  });

  return server;
}

