import Fastify, { type FastifyInstance, type FastifyServerOptions } from "fastify";
import { HermesConnector } from "./connectors/HermesConnector.js";
import { MockConnector } from "./connectors/MockConnector.js";
import { OpenClawConnector } from "./connectors/OpenClawConnector.js";
import { RelayInbox } from "./db/inbox.js";
import { registerCommandsRoute } from "./routes/commands.js";
import { registerEventsRoute } from "./routes/events.js";
import { registerHealthRoute } from "./routes/health.js";
import { registerMessageRoute } from "./routes/message.js";
import { registerMessagesRoute } from "./routes/messages.js";
import { registerTranscribeRoute } from "./routes/transcribe.js";
import { registerWebhookRoute } from "./routes/webhook.js";

export type BuildServerOptions = Pick<FastifyServerOptions, "logger">;

export function buildServer(options: BuildServerOptions = {}): FastifyInstance {
  const server = Fastify({
    logger: options.logger ?? {
      level: process.env.LOG_LEVEL ?? "info"
    }
  });
  const inbox = new RelayInbox();

  registerHealthRoute(server);
  registerCommandsRoute(server);
  registerMessagesRoute(server, inbox);
  registerWebhookRoute(server, inbox);
  registerEventsRoute(server, inbox);
  registerTranscribeRoute(server);
  registerMessageRoute(server, {
    mock: new MockConnector(),
    hermes: new HermesConnector(),
    openclaw: new OpenClawConnector()
  }, inbox);
  server.addHook("onClose", async () => {
    inbox.close();
  });

  return server;
}
