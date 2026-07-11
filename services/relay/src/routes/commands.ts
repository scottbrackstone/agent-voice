import { readFile } from "node:fs/promises";
import type { FastifyInstance } from "fastify";

const COMMAND_CATALOG_JSON_ENV = [
  "HERMES_COMMAND_CATALOG_JSON",
  "AGENTVOICE_COMMAND_CATALOG_JSON"
];
const COMMAND_CATALOG_PATH_ENV = [
  "HERMES_COMMAND_CATALOG_PATH",
  "AGENTVOICE_COMMAND_CATALOG_PATH"
];

export function registerCommandsRoute(server: FastifyInstance): void {
  server.get("/api/commands", async (_request, reply) => {
    const configuredJson = firstEnvValue(COMMAND_CATALOG_JSON_ENV);
    const configuredPath = firstEnvValue(COMMAND_CATALOG_PATH_ENV);

    if (!configuredJson && !configuredPath) {
      return reply.status(404).send({
        error: "Hermes command catalog is not configured.",
        details: [
          "Set HERMES_COMMAND_CATALOG_PATH to a JSON file exported by Solara.",
          "Alternatively set HERMES_COMMAND_CATALOG_JSON to the JSON payload."
        ]
      });
    }

    try {
      const rawCatalog = configuredJson ?? await readFile(configuredPath!, "utf8");
      const parsed = parseCommandCatalog(rawCatalog);
      return reply.send(parsed);
    } catch (error) {
      server.log.warn({ error }, "Unable to load Hermes command catalog");
      return reply.status(500).send({
        error: "Hermes command catalog could not be loaded.",
        details: [
          "Confirm the configured catalog is valid JSON.",
          "The JSON must contain a top-level commands array."
        ]
      });
    }
  });
}

function firstEnvValue(names: string[]): string | undefined {
  for (const name of names) {
    const value = process.env[name]?.trim();
    if (value) {
      return value;
    }
  }
  return undefined;
}

function parseCommandCatalog(rawCatalog: string): unknown {
  const parsed = JSON.parse(rawCatalog);
  if (
    !parsed ||
    typeof parsed !== "object" ||
    !Array.isArray((parsed as { commands?: unknown }).commands)
  ) {
    throw new Error("Command catalog must contain a commands array.");
  }
  return parsed;
}
