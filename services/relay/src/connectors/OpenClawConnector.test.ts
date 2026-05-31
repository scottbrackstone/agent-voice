import { describe, expect, it } from "vitest";
import { OpenClawConnector, readOpenClawConfig } from "./OpenClawConnector.js";
import type { AgentConnectorInput } from "./AgentConnector.js";

const input: AgentConnectorInput = {
  agent: "openclaw",
  message: "What should I work on next?",
  mode: "normal",
  requestId: "req_test"
};

describe("OpenClawConnector", () => {
  it("returns a clear failure when disabled", async () => {
    const connector = new OpenClawConnector({
      enabled: false,
      model: "openclaw",
      user: "agentvoice-mobile",
      timeoutMs: 1000
    });

    const response = await connector.sendMessage(input);

    expect(response).toMatchObject({
      reply: "OpenClaw connector is disabled.",
      status: "failed",
      mode: "normal",
      queuedActions: [],
      connector: "openclaw",
      requestId: "req_test"
    });
    expect(response.warnings[0]).toContain("OPENCLAW_ENABLED=true");
  });

  it("returns a clear failure when enabled without a valid gateway URL", async () => {
    const connector = new OpenClawConnector({
      enabled: true,
      gatewayUrl: "not a url with spaces",
      model: "openclaw",
      user: "agentvoice-mobile",
      timeoutMs: 1000
    });

    const response = await connector.sendMessage(input);

    expect(response.reply).toBe("OpenClaw connector is missing a valid Gateway URL.");
    expect(response.status).toBe("failed");
    expect(response.warnings[0]).toContain("OPENCLAW_GATEWAY_URL");
  });

  it("sends a message to OpenClaw chat completions and maps the reply", async () => {
    const requests: Array<{ url: string; body?: string; headers?: Record<string, string> }> = [];
    const connector = new OpenClawConnector(
      {
        enabled: true,
        gatewayUrl: "http://openclaw.local:8080",
        token: "test-token",
        model: "openclaw/default",
        user: "agentvoice-test",
        sessionKey: "agentvoice-session",
        agentId: "main",
        timeoutMs: 1000
      },
      async (url, init) => {
        requests.push({
          url: url.toString(),
          body: init?.body,
          headers: init?.headers
        });
        expect(init?.method).toBe("POST");
        expect(init?.headers?.Authorization).toBe("Bearer test-token");
        expect(init?.headers?.["Content-Type"]).toBe("application/json");
        expect(init?.headers?.["x-openclaw-session-key"]).toBe("agentvoice-session");
        expect(init?.headers?.["x-openclaw-agent-id"]).toBe("main");
        return {
          ok: true,
          status: 200,
          text: async () =>
            JSON.stringify({
              choices: [{ message: { content: "Here is the OpenClaw reply." } }]
            })
        };
      }
    );

    const response = await connector.sendMessage(input);
    const body = JSON.parse(requests[0].body ?? "{}");

    expect(requests[0].url).toBe("http://openclaw.local:8080/v1/chat/completions");
    expect(body).toMatchObject({
      model: "openclaw/default",
      user: "agentvoice-test",
      messages: [
        { role: "system" },
        { role: "user", content: "What should I work on next?" }
      ]
    });
    expect(body.messages[0].content).toContain("Capture now. Confirm later.");
    expect(response).toMatchObject({
      reply: "Here is the OpenClaw reply.",
      status: "completed",
      mode: "normal",
      queuedActions: [],
      warnings: [],
      connector: "openclaw",
      requestId: "req_test"
    });
  });

  it("returns a clear failure when OpenClaw returns an HTTP error", async () => {
    const connector = new OpenClawConnector(
      {
        enabled: true,
        gatewayUrl: "http://openclaw.local:8080",
        model: "openclaw",
        user: "agentvoice-test",
        timeoutMs: 1000
      },
      async () => ({
        ok: false,
        status: 401,
        text: async () => JSON.stringify({ error: { message: "missing auth" } })
      })
    );

    const response = await connector.sendMessage(input);

    expect(response).toMatchObject({
      reply: "OpenClaw Gateway returned HTTP 401.",
      status: "failed",
      connector: "openclaw"
    });
    expect(response.warnings.join("\n")).toContain("missing auth");
  });

  it("returns a clear failure when the Gateway does not return chat completion JSON", async () => {
    const connector = new OpenClawConnector(
      {
        enabled: true,
        gatewayUrl: "http://openclaw.local:8080",
        model: "openclaw",
        user: "agentvoice-test",
        timeoutMs: 1000
      },
      async () => ({
        ok: true,
        status: 200,
        text: async () => "<!doctype html><title>OpenClaw Control</title>"
      })
    );

    const response = await connector.sendMessage(input);

    expect(response).toMatchObject({
      reply: "OpenClaw Gateway response was not a chat completion.",
      status: "failed",
      connector: "openclaw"
    });
    expect(response.warnings.join("\n")).toContain("chatCompletions.enabled");
  });

  it("parses environment config safely", () => {
    expect(
      readOpenClawConfig({
        OPENCLAW_ENABLED: "true",
        OPENCLAW_GATEWAY_URL: " http://localhost:8080 ",
        OPENCLAW_TOKEN: " secret ",
        OPENCLAW_MODEL: " openclaw/main ",
        OPENCLAW_USER: " agentvoice-user ",
        OPENCLAW_SESSION_KEY: " agentvoice-session ",
        OPENCLAW_AGENT_ID: " main ",
        OPENCLAW_TIMEOUT_MS: "2500"
      })
    ).toEqual({
      enabled: true,
      gatewayUrl: "http://localhost:8080",
      token: "secret",
      model: "openclaw/main",
      user: "agentvoice-user",
      sessionKey: "agentvoice-session",
      agentId: "main",
      timeoutMs: 2500
    });

    expect(readOpenClawConfig({ OPENCLAW_TIMEOUT_MS: "10" }).timeoutMs).toBe(30_000);
  });
});

