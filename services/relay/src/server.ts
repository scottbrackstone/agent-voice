import Fastify, { type FastifyInstance, type FastifyServerOptions } from "fastify";
import { HermesConnector } from "./connectors/HermesConnector.js";
import { HermesApiClient, type HermesApi } from "./connectors/HermesApiClient.js";
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
import { registerThreadsRoute } from "./routes/threads.js";
import { registerRunsRoute } from "./routes/runs.js";
import { registerDevicesRoute } from "./routes/devices.js";
import { RunCoordinator } from "./runs/RunCoordinator.js";
import {
  DisabledPushNotifier,
  FirebasePushNotifier,
  type PushNotifier
} from "./push/PushNotifier.js";

export type BuildServerOptions = Pick<FastifyServerOptions, "logger"> & {
  inbox?: RelayInbox;
  hermesApi?: HermesApi;
  pushNotifier?: PushNotifier;
};

export function buildServer(options: BuildServerOptions = {}): FastifyInstance {
  const server = Fastify({
    logger: options.logger ?? {
      level: process.env.LOG_LEVEL ?? "info"
    }
  });
  const inbox = options.inbox ?? new RelayInbox();
  const hermesApi = options.hermesApi ?? new HermesApiClient();
  const pushNotifier = options.pushNotifier ?? (
    process.env.AGENTVOICE_FIREBASE_ENABLED === "true"
      ? new FirebasePushNotifier(inbox)
      : new DisabledPushNotifier()
  );
  const runs = new RunCoordinator(inbox, hermesApi, pushNotifier, server.log);

  registerHealthRoute(server);
  registerCommandsRoute(server);
  registerMessagesRoute(server, inbox);
  registerWebhookRoute(server, inbox, pushNotifier);
  registerEventsRoute(server, inbox);
  registerThreadsRoute(server, hermesApi, inbox, runs);
  registerRunsRoute(server, runs);
  registerDevicesRoute(server, inbox);
  registerTranscribeRoute(server);
  registerMessageRoute(server, {
    mock: new MockConnector(),
    hermes: new HermesConnector(),
    openclaw: new OpenClawConnector()
  }, inbox);
  server.addHook("onClose", async () => {
    runs.close();
    inbox.close();
  });

  return server;
}
