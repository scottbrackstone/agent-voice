import { describe, expect, it, vi } from "vitest";
import { HermesConnector, readHermesConfig } from "./HermesConnector.js";

describe("HermesConnector", () => {
  it("returns a clear disabled response", async () => {
    const connector = new HermesConnector({
      enabled: false,
      model: "hermes",
      user: "agentvoice-test",
      timeoutMs: 30_000
    });

    const response = await connector.sendMessage({
      agent: "hermes",
      message: "Hello",
      mode: "normal",
      requestId: "req_test"
    });

    expect(response).toMatchObject({
      reply: "Hermes connector is disabled.",
      status: "failed",
      connector: "hermes",
      requestId: "req_test"
    });
  });

  it("sends OpenAI-compatible chat completions", async () => {
    const fetchImpl = vi.fn(async () => ({
      ok: true,
      status: 200,
      text: async () =>
        JSON.stringify({
          choices: [
            {
              message: {
                content: "Hermes heard you."
              }
            }
          ]
        })
    }));
    const connector = new HermesConnector(
      {
        enabled: true,
        baseUrl: "http://127.0.0.1:18888",
        token: "token",
        model: "hermes-agent",
        user: "agentvoice-test",
        timeoutMs: 30_000
      },
      fetchImpl
    );

    const response = await connector.sendMessage({
      agent: "hermes",
      message: "Hello Hermes",
      mode: "mobile",
      requestId: "req_test"
    });

    expect(response).toMatchObject({
      reply: "Hermes heard you.",
      status: "completed",
      connector: "hermes"
    });
    const [url, init] = fetchImpl.mock.calls[0] as unknown as [
      string | URL,
      {
        method?: string;
        headers?: Record<string, string>;
        body?: string;
      }
    ];
    expect(url.toString()).toBe("http://127.0.0.1:18888/v1/chat/completions");
    expect(init.method).toBe("POST");
    expect(init.headers).toMatchObject({
      Authorization: "Bearer token",
      "Content-Type": "application/json"
    });
    const body = JSON.parse(init.body ?? "{}");
    expect(body).toMatchObject({
      model: "hermes-agent",
      user: "agentvoice-test"
    });
    expect(body.messages[1]).toEqual({
      role: "user",
      content: "Hello Hermes"
    });
  });

  it("reads config from env", () => {
    expect(
      readHermesConfig({
        HERMES_ENABLED: "true",
        HERMES_BASE_URL: "http://127.0.0.1:18888",
        HERMES_TOKEN: "token",
        HERMES_MODEL: "hermes-agent",
        HERMES_USER: "scott",
        HERMES_TIMEOUT_MS: "45000"
      })
    ).toEqual({
      enabled: true,
      baseUrl: "http://127.0.0.1:18888",
      token: "token",
      model: "hermes-agent",
      user: "scott",
      timeoutMs: 45_000
    });
  });
});
