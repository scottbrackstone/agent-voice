import Fastify from "fastify";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { AgentConnector } from "./connectors/AgentConnector.js";
import { HermesApiError } from "./connectors/HermesApiClient.js";
import type {
  CreateHermesSessionInput,
  HermesApi,
  HermesSession
} from "./connectors/HermesApiClient.js";
import type { PushNotifier } from "./push/PushNotifier.js";
import { registerMessageRoute } from "./routes/message.js";
import { buildServer } from "./server.js";

describe("AgentVoice relay", () => {
  beforeEach(() => {
    process.env.AGENTVOICE_INBOX_DB_PATH = ":memory:";
    delete process.env.AGENTVOICE_RELAY_TOKEN;
    delete process.env.AGENTVOICE_WEBHOOK_TOKEN;
    delete process.env.AGENTVOICE_FIREBASE_ENABLED;
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

  it("creates, lists, renames, and reads canonical Hermes threads", async () => {
    const hermes = createFakeHermesApi();
    const server = buildServer({ logger: false, hermesApi: hermes });

    const created = await server.inject({
      method: "POST",
      url: "/api/threads",
      payload: { title: "Gym planning" }
    });
    expect(created.statusCode).toBe(201);
    const threadId = created.json().id as string;

    const list = await server.inject({ method: "GET", url: "/api/threads" });
    expect(list.json().threads).toEqual([
      expect.objectContaining({ id: threadId, title: "Gym planning" })
    ]);

    const renamed = await server.inject({
      method: "PATCH",
      url: `/api/threads/${threadId}`,
      payload: { title: "Training block" }
    });
    expect(renamed.json()).toEqual({ id: threadId, title: "Training block" });

    const history = await server.inject({
      method: "GET",
      url: `/api/threads/${threadId}/messages`
    });
    expect(history.statusCode).toBe(200);
    expect(history.json()).toMatchObject({
      threadId,
      messages: [
        expect.objectContaining({ role: "assistant", content: "Canonical history" })
      ]
    });
    await server.close();
  });

  it("creates multiple untitled threads from the legacy New chat title", async () => {
    const hermes = createFakeHermesApi();
    const server = buildServer({ logger: false, hermesApi: hermes });

    const first = await server.inject({
      method: "POST",
      url: "/api/threads",
      payload: { title: "New chat" }
    });
    const second = await server.inject({
      method: "POST",
      url: "/api/threads",
      payload: { title: "New chat" }
    });

    expect(first.statusCode).toBe(201);
    expect(second.statusCode).toBe(201);
    expect(first.json()).toMatchObject({ title: "New chat", isUntitled: true, duplicate: false });
    expect(second.json()).toMatchObject({ title: "New chat", isUntitled: true, duplicate: false });
    expect(second.json().id).not.toBe(first.json().id);
    expect(hermes.createSession).toHaveBeenNthCalledWith(1, { id: undefined, title: undefined });
    expect(hermes.createSession).toHaveBeenNthCalledWith(2, { id: undefined, title: undefined });
    await server.close();
  });

  it("returns the same thread when a client create ID is retried", async () => {
    const hermes = createFakeHermesApi();
    const server = buildServer({ logger: false, hermesApi: hermes });
    const payload = { clientThreadId: "123e4567-e89b-12d3-a456-426614174000" };

    const first = await server.inject({ method: "POST", url: "/api/threads", payload });
    const retry = await server.inject({ method: "POST", url: "/api/threads", payload });

    expect(first.statusCode).toBe(201);
    expect(retry.statusCode).toBe(200);
    expect(retry.json()).toMatchObject({
      id: first.json().id,
      isUntitled: true,
      duplicate: true
    });
    expect(hermes.getSession).toHaveBeenCalledWith(
      "agentvoice_123e4567-e89b-12d3-a456-426614174000"
    );
    await server.close();
  });

  it("returns a conflict and request ID for a duplicate custom title", async () => {
    const server = buildServer({ logger: false, hermesApi: createFakeHermesApi() });

    await server.inject({
      method: "POST",
      url: "/api/threads",
      payload: { title: "Gym planning" }
    });
    const conflict = await server.inject({
      method: "POST",
      url: "/api/threads",
      payload: { title: "Gym planning" }
    });

    expect(conflict.statusCode).toBe(409);
    expect(conflict.json()).toMatchObject({
      code: "invalid_title",
      detail: expect.stringContaining("already in use")
    });
    expect(conflict.json().requestId).toMatch(/^req_/);
    await server.close();
  });

  it("starts one idempotent streaming run and reports completion", async () => {
    const hermes = createFakeHermesApi();
    const notify = vi.fn<PushNotifier["notify"]>(async () => {});
    const server = buildServer({
      logger: false,
      hermesApi: hermes,
      pushNotifier: { notify }
    });
    const created = await server.inject({
      method: "POST",
      url: "/api/threads",
      payload: { title: "Streaming" }
    });
    const threadId = created.json().id as string;
    const payload = {
      clientMessageId: "client-message-1",
      content: "Tell me something",
      mode: "normal"
    };

    const first = await server.inject({
      method: "POST",
      url: `/api/threads/${threadId}/messages`,
      payload
    });
    const duplicate = await server.inject({
      method: "POST",
      url: `/api/threads/${threadId}/messages`,
      payload
    });
    expect(first.statusCode).toBe(202);
    expect(duplicate.statusCode).toBe(200);
    expect(duplicate.json()).toMatchObject({
      runId: first.json().runId,
      duplicate: true
    });

    await vi.waitFor(async () => {
      const run = await server.inject({
        method: "GET",
        url: `/api/runs/${first.json().runId}`
      });
      expect(run.json()).toMatchObject({
        status: "completed",
        replyText: "Streamed answer",
        presentation: { mood: "happy" }
      });
    });
    expect(hermes.streamMessage).toHaveBeenCalledTimes(1);
    expect(notify).toHaveBeenCalledTimes(1);
    await server.close();
  });

  it("registers devices and deduplicates webhook delivery events", async () => {
    process.env.AGENTVOICE_WEBHOOK_TOKEN = "test-token";
    const notify = vi.fn<PushNotifier["notify"]>(async () => {});
    const server = buildServer({
      logger: false,
      hermesApi: createFakeHermesApi(),
      pushNotifier: { notify }
    });
    const device = await server.inject({
      method: "POST",
      url: "/api/devices/register",
      payload: {
        installationId: "phone-1",
        fcmToken: "fcm-token",
        platform: "android"
      }
    });
    expect(device.statusCode).toBe(201);

    const payload = {
      eventId: "cron:job-1:run-1",
      conversationId: "session-1",
      content: "Scheduled ping",
      source: "cron"
    };
    const first = await server.inject({
      method: "POST",
      url: "/api/webhook/receive",
      headers: { "x-relay-token": "test-token" },
      payload
    });
    const duplicate = await server.inject({
      method: "POST",
      url: "/api/webhook/receive",
      headers: { "x-relay-token": "test-token" },
      payload
    });
    expect(first.statusCode).toBe(201);
    expect(first.json().duplicate).toBe(false);
    expect(duplicate.json()).toMatchObject({
      id: first.json().id,
      duplicate: true
    });
    expect(notify).toHaveBeenCalledTimes(1);
    await server.close();
  });

  it("keeps canonical completion successful when push delivery fails", async () => {
    const notify = vi.fn<PushNotifier["notify"]>(async () => {
      throw new Error("Firebase unavailable");
    });
    const server = buildServer({
      logger: false,
      hermesApi: createFakeHermesApi(),
      pushNotifier: { notify }
    });
    const created = await server.inject({
      method: "POST",
      url: "/api/threads",
      payload: { title: "Push isolation" }
    });
    const started = await server.inject({
      method: "POST",
      url: `/api/threads/${created.json().id}/messages`,
      payload: {
        clientMessageId: "client-message-push-failure",
        content: "Complete even if push is down",
        mode: "normal"
      }
    });

    await vi.waitFor(async () => {
      const run = await server.inject({
        method: "GET",
        url: `/api/runs/${started.json().runId}`
      });
      expect(run.json().status).toBe("completed");
    });
    await server.close();
  });

  it("accepts a stored webhook message when push delivery fails", async () => {
    process.env.AGENTVOICE_WEBHOOK_TOKEN = "test-token";
    const server = buildServer({
      logger: false,
      hermesApi: createFakeHermesApi(),
      pushNotifier: {
        notify: async () => {
          throw new Error("Firebase unavailable");
        }
      }
    });
    const response = await server.inject({
      method: "POST",
      url: "/api/webhook/receive",
      headers: { "x-relay-token": "test-token" },
      payload: {
        eventId: "cron:push-failure",
        conversationId: "session-push-failure",
        content: "Stored before push",
        source: "cron"
      }
    });

    expect(response.statusCode).toBe(201);
    expect(response.json()).toMatchObject({ duplicate: false, status: "queued" });
    await server.close();
  });
});

function createFakeHermesApi(): HermesApi & { streamMessage: ReturnType<typeof vi.fn> } {
  const sessions = new Map<string, HermesSession>();
  let nextId = 1;
  return {
    enabled: true,
    createSession: vi.fn(async (input: CreateHermesSessionInput = {}) => {
      const id = input.id ?? `session-${nextId++}`;
      if (sessions.has(id)) {
        throw new HermesApiError(
          `Session already exists: ${id}`,
          409,
          `Session already exists: ${id}`,
          "session_exists"
        );
      }
      const titleConflict = input.title
        ? [...sessions.values()].find((session) => session.title === input.title)
        : undefined;
      if (titleConflict) {
        const detail = `Title '${input.title}' is already in use by session ${titleConflict.id}`;
        throw new HermesApiError(detail, 400, detail, "invalid_title");
      }
      const session = {
        id,
        title: input.title ?? "New chat",
        isUntitled: input.title == null,
        startedAt: 1_720_000_000,
        messageCount: 0
      };
      sessions.set(session.id, session);
      return session;
    }),
    getSession: vi.fn(async (sessionId: string) => sessions.get(sessionId)!),
    listSessions: vi.fn(async () => [...sessions.values()]),
    renameSession: vi.fn(async (sessionId: string, title: string) => {
      const current = sessions.get(sessionId)!;
      const updated = { ...current, title, isUntitled: false };
      sessions.set(sessionId, updated);
      return updated;
    }),
    listMessages: vi.fn(async (sessionId: string) => [{
      id: "message-1",
      sessionId,
      role: "assistant",
      content: "Canonical history",
      timestamp: 1_720_000_001
    }]),
    streamMessage: vi.fn(async (input) => {
      input.onEvent({ type: "delta", text: "Streamed " });
      input.onEvent({ type: "delta", text: "answer" });
      input.onEvent({
        type: "presentation",
        presentation: {
          mood: "happy",
          intensity: 0.7,
          state: "success",
          motionHint: "celebrate"
        }
      });
      return {
        text: "Streamed answer",
        presentation: {
          mood: "happy" as const,
          intensity: 0.7,
          state: "success" as const,
          motionHint: "celebrate" as const
        }
      };
    })
  };
}
