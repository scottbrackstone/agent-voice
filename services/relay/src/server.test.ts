import Fastify from "fastify";
import { describe, expect, it, vi } from "vitest";
import type { AgentConnector } from "./connectors/AgentConnector.js";
import { registerMessageRoute } from "./routes/message.js";
import { buildServer } from "./server.js";

describe("AgentVoice relay", () => {
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
});

