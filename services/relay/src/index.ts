import { buildServer } from "./server.js";

const port = Number(process.env.PORT ?? 3001);
const host = process.env.HOST ?? "0.0.0.0";

const server = buildServer();

server.listen({ port, host }).catch((error) => {
  server.log.error({ error }, "Failed to start AgentVoice relay");
  process.exit(1);
});

