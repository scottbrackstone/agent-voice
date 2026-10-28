import { isLoopbackHost, relayToken } from "./auth/relayAuth.js";
import { buildServer } from "./server.js";

const port = Number(process.env.PORT ?? 3001);
const host = process.env.HOST ?? "0.0.0.0";

const server = buildServer();

if (!relayToken() && !isLoopbackHost(host)) {
  server.log.warn(
    { host },
    "AGENTVOICE_RELAY_TOKEN is not set: every relay route except /health is open to anyone who can reach this address."
  );
}

server.listen({ port, host }).catch((error) => {
  server.log.error({ error }, "Failed to start AgentVoice relay");
  process.exit(1);
});

