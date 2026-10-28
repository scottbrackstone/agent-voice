import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { buildServer } from "../server.js";
import { isLoopbackHost, tokensMatch } from "./relayAuth.js";

const MESSAGES_URL = "/api/messages?conversationId=auth-test";

describe("relay token auth", () => {
  beforeEach(() => {
    process.env.AGENTVOICE_INBOX_DB_PATH = ":memory:";
    delete process.env.AGENTVOICE_RELAY_TOKEN;
    delete process.env.AGENTVOICE_WEBHOOK_TOKEN;
    delete process.env.AGENTVOICE_FIREBASE_ENABLED;
  });

  afterEach(() => {
    delete process.env.AGENTVOICE_RELAY_TOKEN;
    delete process.env.AGENTVOICE_WEBHOOK_TOKEN;
  });

  it("leaves routes open when no relay token is configured", async () => {
    const server = buildServer({ logger: false });
    const response = await server.inject({ method: "GET", url: MESSAGES_URL });
    await server.close();

    expect(response.statusCode).toBe(200);
  });

  it("rejects missing or wrong bearer tokens once a relay token is configured", async () => {
    process.env.AGENTVOICE_RELAY_TOKEN = "relay-secret";
    const server = buildServer({ logger: false });

    const missing = await server.inject({ method: "GET", url: MESSAGES_URL });
    const wrong = await server.inject({
      method: "GET",
      url: MESSAGES_URL,
      headers: { authorization: "Bearer not-the-secret" }
    });
    const notBearer = await server.inject({
      method: "GET",
      url: MESSAGES_URL,
      headers: { authorization: "relay-secret" }
    });
    await server.close();

    expect(missing.statusCode).toBe(401);
    expect(wrong.statusCode).toBe(401);
    expect(notBearer.statusCode).toBe(401);
  });

  it("accepts the configured bearer token", async () => {
    process.env.AGENTVOICE_RELAY_TOKEN = "relay-secret";
    const server = buildServer({ logger: false });
    const response = await server.inject({
      method: "GET",
      url: MESSAGES_URL,
      headers: { authorization: "Bearer relay-secret" }
    });
    await server.close();

    expect(response.statusCode).toBe(200);
  });

  it("keeps health open and lets the webhook use its own token", async () => {
    process.env.AGENTVOICE_RELAY_TOKEN = "relay-secret";
    process.env.AGENTVOICE_WEBHOOK_TOKEN = "webhook-secret";
    const server = buildServer({ logger: false });

    const health = await server.inject({ method: "GET", url: "/health" });
    const webhook = await server.inject({
      method: "POST",
      url: "/api/webhook/receive",
      headers: { "x-relay-token": "webhook-secret" },
      payload: { conversationId: "auth-test", content: "hello" }
    });
    const webhookWrongToken = await server.inject({
      method: "POST",
      url: "/api/webhook/receive",
      headers: { "x-relay-token": "webhook-secre" },
      payload: { conversationId: "auth-test", content: "hello" }
    });
    await server.close();

    expect(health.statusCode).toBe(200);
    expect(webhook.statusCode).toBe(201);
    expect(webhookWrongToken.statusCode).toBe(401);
  });

  it("compares tokens safely", () => {
    expect(tokensMatch("abc", "abc")).toBe(true);
    expect(tokensMatch("abd", "abc")).toBe(false);
    expect(tokensMatch("ab", "abc")).toBe(false);
    expect(tokensMatch(undefined, "abc")).toBe(false);
    expect(tokensMatch(["abc"], "abc")).toBe(false);
  });

  it("recognises loopback hosts", () => {
    expect(isLoopbackHost("127.0.0.1")).toBe(true);
    expect(isLoopbackHost("::1")).toBe(true);
    expect(isLoopbackHost("0.0.0.0")).toBe(false);
    expect(isLoopbackHost("192.168.1.20")).toBe(false);
  });
});
