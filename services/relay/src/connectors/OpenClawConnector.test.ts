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
      timeoutMs: 1000
    });

    const response = await connector.sendMessage(input);

    expect(response.reply).toBe("OpenClaw connector is missing a valid Gateway URL.");
    expect(response.status).toBe("failed");
    expect(response.warnings[0]).toContain("OPENCLAW_GATEWAY_URL");
  });

  it("probes the known companion events API but still refuses to invent message protocol", async () => {
    const requestedUrls: string[] = [];
    const connector = new OpenClawConnector(
      {
        enabled: true,
        gatewayUrl: "http://openclaw.local:8080",
        token: "test-token",
        timeoutMs: 1000
      },
      async (url, init) => {
        requestedUrls.push(url.toString());
        expect(init?.headers?.Authorization).toBe("Bearer test-token");
        return {
          ok: true,
          status: 200,
          text: async () => JSON.stringify({ events: [] })
        };
      }
    );

    const response = await connector.sendMessage(input);

    expect(requestedUrls).toEqual(["http://openclaw.local:8080/events"]);
    expect(response).toMatchObject({
      reply: "OpenClaw message protocol is not implemented yet.",
      status: "failed",
      connector: "openclaw"
    });
    expect(response.warnings.join("\n")).toContain("known companion /events shape");
    expect(response.warnings.join("\n")).toContain("cannot carry an AgentVoice user message");
  });

  it("parses environment config safely", () => {
    expect(
      readOpenClawConfig({
        OPENCLAW_ENABLED: "true",
        OPENCLAW_GATEWAY_URL: " http://localhost:8080 ",
        OPENCLAW_TOKEN: " secret ",
        OPENCLAW_TIMEOUT_MS: "2500"
      })
    ).toEqual({
      enabled: true,
      gatewayUrl: "http://localhost:8080",
      token: "secret",
      timeoutMs: 2500
    });

    expect(readOpenClawConfig({ OPENCLAW_TIMEOUT_MS: "10" }).timeoutMs).toBe(10_000);
  });
});

