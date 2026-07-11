import Fastify from "fastify";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { AgentConnector } from "./connectors/AgentConnector.js";
import { registerMessageRoute } from "./routes/message.js";
import { buildServer } from "./server.js";

describe("AgentVoice relay", () => {
  beforeEach(() => {
    process.env.AGENTVOICE_INBOX_DB_PATH = ":memory:";
    delete process.env.AGENTVOICE_WEBHOOK_TOKEN;
  });

  it("returns health status", async () => {
    const server = buildServer({ logger: false });

    const response = await server.inject({
      method: "GET",
      url: "/health"
    });

    await server.close();

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({
      ok: true,
      service: "agentvoice-relay"
    });
  });

  it("returns a clear command catalog error when no catalog is configured", async () => {
    const previousCatalogJson = process.env.HERMES_COMMAND_CATALOG_JSON;
    const previousCatalogPath = process.env.HERMES_COMMAND_CATALOG_PATH;
    const previousAgentVoiceCatalogJson = process.env.AGENTVOICE_COMMAND_CATALOG_JSON;
    const previousAgentVoiceCatalogPath = process.env.AGENTVOICE_COMMAND_CATALOG_PATH;
    delete process.env.HERMES_COMMAND_CATALOG_JSON;
    delete process.env.HERMES_COMMAND_CATALOG_PATH;
    delete process.env.AGENTVOICE_COMMAND_CATALOG_JSON;
    delete process.env.AGENTVOICE_COMMAND_CATALOG_PATH;
    const server = buildServer({ logger: false });

    const response = await server.inject({
      method: "GET",
      url: "/api/commands"
    });

    await server.close();
    if (previousCatalogJson) {
      process.env.HERMES_COMMAND_CATALOG_JSON = previousCatalogJson;
    }
    if (previousCatalogPath) {
      process.env.HERMES_COMMAND_CATALOG_PATH = previousCatalogPath;
    }
    if (previousAgentVoiceCatalogJson) {
      process.env.AGENTVOICE_COMMAND_CATALOG_JSON = previousAgentVoiceCatalogJson;
    }
    if (previousAgentVoiceCatalogPath) {
      process.env.AGENTVOICE_COMMAND_CATALOG_PATH = previousAgentVoiceCatalogPath;
    }

    expect(response.statusCode).toBe(404);
    expect(response.json()).toMatchObject({
      error: "Hermes command catalog is not configured."
    });
  });

  it("returns a configured Solara command catalog", async () => {
    const previousCatalogJson = process.env.HERMES_COMMAND_CATALOG_JSON;
    process.env.HERMES_COMMAND_CATALOG_JSON = JSON.stringify({
      version: 1,
      source: "test",
      commands: [
        {
          name: "/model",
          label: "Model",
          template: "/model {api} {model}",
          sendMode: "fill_composer",
          steps: [
            {
              id: "api",
              label: "Select a provider",
              type: "single_select",
              options: [
                { label: "DeepSeek (4)", value: "DeepSeek" }
              ]
            },
            {
              id: "model",
              label: "Select a model",
              type: "single_select",
              optionsBy: "api",
              optionGroups: {
                DeepSeek: [
                  { label: "deepseek-v4-pro", value: "deepseek-v4-pro" }
                ]
              }
            }
          ]
        }
      ]
    });
    const server = buildServer({ logger: false });

    const response = await server.inject({
      method: "GET",
      url: "/api/commands"
    });

    await server.close();
    if (previousCatalogJson) {
      process.env.HERMES_COMMAND_CATALOG_JSON = previousCatalogJson;
    } else {
      delete process.env.HERMES_COMMAND_CATALOG_JSON;
    }

    expect(response.statusCode).toBe(200);
    expect(response.json()).toMatchObject({
      version: 1,
      source: "test",
      commands: [
        expect.objectContaining({
          name: "/model"
        })
      ]
    });
  });

  it("queues webhook messages, returns them in polling, then acknowledges them", async () => {
    process.env.AGENTVOICE_WEBHOOK_TOKEN = "test-token";
    const server = buildServer({ logger: false });

    const webhookResponse = await server.inject({
      method: "POST",
      url: "/api/webhook/receive",
      headers: {
        "x-relay-token": "test-token"
      },
      payload: {
        conversationId: "thread-abc123",
        content: "Ping from Solara",
        source: "cron",
        metadata: {
          cronJobId: "job_1"
        }
      }
    });

    expect(webhookResponse.statusCode).toBe(201);
    const messageId = webhookResponse.json().id;

    const pollResponse = await server.inject({
      method: "GET",
      url: "/api/messages?conversationId=thread-abc123"
    });

    expect(pollResponse.statusCode).toBe(200);
    expect(pollResponse.json()).toMatchObject({
      conversationId: "thread-abc123",
      messages: [
        expect.objectContaining({
          id: messageId,
          direction: "outgoing",
          content: "Ping from Solara",
          source: "cron",
          status: "pending",
          metadata: {
            cronJobId: "job_1"
          }
        })
      ],
      hasMore: false
    });

    const ackResponse = await server.inject({
      method: "POST",
      url: `/api/messages/${messageId}/ack`
    });

    expect(ackResponse.statusCode).toBe(200);
    expect(ackResponse.json()).toMatchObject({
      id: messageId,
      status: "delivered"
    });

    const afterAckResponse = await server.inject({
      method: "GET",
      url: "/api/messages?conversationId=thread-abc123"
    });

    await server.close();

    expect(afterAckResponse.statusCode).toBe(200);
    expect(afterAckResponse.json().messages).toEqual([]);
  });

  it("records app-sent messages and immediate replies in thread history", async () => {
    const server = buildServer({ logger: false });

    const response = await server.inject({
      method: "POST",
      url: "/api/message",
      payload: {
        agent: "mock",
        message: "Hello from the app",
        mode: "normal",
        conversationId: "local-thread-123"
      }
    });

    expect(response.statusCode).toBe(200);

    const historyResponse = await server.inject({
      method: "GET",
      url: "/api/threads/local-thread-123/messages"
    });

    const pendingResponse = await server.inject({
      method: "GET",
      url: "/api/messages?conversationId=local-thread-123"
    });

    await server.close();

    expect(historyResponse.statusCode).toBe(200);
    expect(historyResponse.json().messages).toEqual([
      expect.objectContaining({
        direction: "incoming",
        content: "Hello from the app",
        source: "user",
        status: "delivered"
      }),
      expect.objectContaining({
        direction: "outgoing",
        content: "Mock agent heard: Hello from the app",
        source: "mock",
        status: "delivered"
      })
    ]);
    expect(pendingResponse.statusCode).toBe(200);
    expect(pendingResponse.json().messages).toEqual([]);
  });

  it("rejects invalid message bodies", async () => {
    const server = buildServer({ logger: false });

    const response = await server.inject({
      method: "POST",
      url: "/api/message",
      payload: {
        agent: "mock",
        message: "",
        mode: "normal"
      }
    });

    await server.close();

    expect(response.statusCode).toBe(400);
    expect(response.json().error).toBe("Invalid request body");
  });

  it("returns a normal mock connector response", async () => {
    const server = buildServer({ logger: false });

    const response = await server.inject({
      method: "POST",
      url: "/api/message",
      payload: {
        agent: "mock",
        message: "What should I work on next?",
        mode: "normal"
      }
    });

    await server.close();

    expect(response.statusCode).toBe(200);
    expect(response.json()).toMatchObject({
      reply: "Mock agent heard: What should I work on next?",
      status: "completed",
      mode: "normal",
      queuedActions: [],
      warnings: [],
      connector: "mock"
    });
    expect(response.json().requestId).toMatch(/^req_/);
  });

  it("passes conversationId and flag-style text through to connectors", async () => {
    const server = Fastify({ logger: false });
    const sendMessage = vi.fn<AgentConnector["sendMessage"]>(async (input) => ({
      reply: "ok",
      status: "completed",
      mode: input.mode,
      queuedActions: [],
      warnings: [],
      connector: "mock",
      requestId: input.requestId
    }));
    const message = "/brief --thread hermes --dry-run keep these flags";

    registerMessageRoute(server, {
      mock: {
        name: "mock",
        sendMessage
      }
    });

    const response = await server.inject({
      method: "POST",
      url: "/api/message",
      payload: {
        agent: "mock",
        message,
        mode: "normal",
        conversationId: "local-thread-123"
      }
    });

    await server.close();

    expect(response.statusCode).toBe(200);
    expect(sendMessage).toHaveBeenCalledWith(
      expect.objectContaining({
        message,
        conversationId: "local-thread-123"
      })
    );
  });

  it("queues reminder-style actions in mobile mode", async () => {
    const server = buildServer({ logger: false });

    const response = await server.inject({
      method: "POST",
      url: "/api/message",
      payload: {
        agent: "mock",
        message: "Remind me to test OpenClaw tomorrow",
        mode: "mobile"
      }
    });

    await server.close();

    expect(response.statusCode).toBe(200);
    expect(response.json()).toMatchObject({
      reply: "Captured. I queued a reminder for review.",
      status: "queued",
      mode: "mobile",
      connector: "mock"
    });
    expect(response.json().queuedActions).toHaveLength(1);
    expect(response.json().queuedActions[0]).toMatchObject({
      type: "reminder",
      summary: "Test OpenClaw tomorrow",
      requiresConfirmation: true,
      status: "queued"
    });
  });

  it("captures capture-only messages without calling the selected connector", async () => {
    const server = Fastify({ logger: false });
    const sendMessage = vi.fn<AgentConnector["sendMessage"]>();

    registerMessageRoute(server, {
      mock: {
        name: "mock",
        sendMessage
      }
    });

    const response = await server.inject({
      method: "POST",
      url: "/api/message",
      payload: {
        agent: "mock",
        message: "Remind me to check the charger tomorrow",
        mode: "capture_only"
      }
    });

    await server.close();

    expect(sendMessage).not.toHaveBeenCalled();
    expect(response.statusCode).toBe(200);
    expect(response.json()).toMatchObject({
      reply: "Captured for later review.",
      status: "queued",
      mode: "capture_only",
      connector: "relay",
      warnings: ["Capture-only mode did not call the mock connector."]
    });
    expect(response.json().queuedActions).toHaveLength(1);
    expect(response.json().queuedActions[0]).toMatchObject({
      type: "reminder",
      summary: "Check the charger tomorrow",
      requiresConfirmation: true,
      status: "queued"
    });
  });

  it("allows capture-only notes even when the requested connector is unavailable", async () => {
    const server = Fastify({ logger: false });

    registerMessageRoute(server, {});

    const response = await server.inject({
      method: "POST",
      url: "/api/message",
      payload: {
        agent: "openclaw",
        message: "Capture this voice note from the car",
        mode: "capture_only"
      }
    });

    await server.close();

    expect(response.statusCode).toBe(200);
    expect(response.json()).toMatchObject({
      reply: "Captured for later review.",
      status: "queued",
      mode: "capture_only",
      connector: "relay",
      warnings: ["Capture-only mode did not call the openclaw connector."]
    });
    expect(response.json().queuedActions[0]).toMatchObject({
      type: "note",
      summary: "This voice note from the car"
    });
  });

  it("returns a clear transcribe error when Voxtral is not configured", async () => {
    const previousApiKey = process.env.MISTRAL_API_KEY;
    delete process.env.MISTRAL_API_KEY;
    const server = buildServer({ logger: false });

    const response = await server.inject({
      method: "POST",
      url: "/api/transcribe",
      headers: {
        "content-type": "audio/mp4"
      },
      payload: Buffer.from("not real audio")
    });

    await server.close();
    if (previousApiKey) {
      process.env.MISTRAL_API_KEY = previousApiKey;
    }

    expect(response.statusCode).toBe(503);
    expect(response.json()).toMatchObject({
      error: "Voxtral transcription is not configured."
    });
    expect(response.json().requestId).toMatch(/^req_/);
  });
});
