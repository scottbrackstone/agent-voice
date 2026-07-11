import { describe, expect, it } from "vitest";
import { HermesApiClient } from "./HermesApiClient.js";
import type { HermesConfig } from "./HermesConnector.js";

const config = (timeoutMs = 120_000): HermesConfig => ({
  enabled: true,
  baseUrl: "http://127.0.0.1:8642",
  token: "test-token",
  model: "test-model",
  user: "agentvoice-test",
  timeoutMs
});

describe("HermesApiClient", () => {
  it("scopes scheduled delivery to the active AgentVoice session", async () => {
    let capturedInit: RequestInit | undefined;
    const fetchImpl: typeof fetch = async (_input, init) => {
      capturedInit = init;
      return new Response(
        'data: {"choices":[{"delta":{"content":"Hello"}}]}\n\ndata: [DONE]\n\n',
        { status: 200, headers: { "Content-Type": "text/event-stream" } }
      );
    };
    const events: string[] = [];
    const client = new HermesApiClient(config(), fetchImpl);

    const result = await client.streamMessage({
      sessionId: "api_session_123",
      message: "Ping me later",
      mode: "normal",
      idempotencyKey: "client-message-1",
      onEvent: (event) => {
        if (event.type === "delta") events.push(event.text);
      }
    });

    const body = JSON.parse(String(capturedInit?.body)) as {
      messages: Array<{ role: string; content: string }>;
    };
    expect(body.messages[0]?.content).toContain("deliver to agentvoice:api_session_123");
    expect((capturedInit?.headers as Record<string, string>)["X-Hermes-Session-Id"])
      .toBe("api_session_123");
    expect(result.text).toBe("Hello");
    expect(events).toEqual(["Hello"]);
  });

  it("returns a clear error when a streaming run exceeds the configured timeout", async () => {
    const fetchImpl: typeof fetch = async (_input, init) =>
      await new Promise<Response>((_resolve, reject) => {
        init?.signal?.addEventListener("abort", () => reject(init.signal?.reason), { once: true });
      });
    const client = new HermesApiClient(config(10), fetchImpl);

    await expect(client.streamMessage({
      sessionId: "api_session_timeout",
      message: "Run a tool",
      mode: "normal",
      idempotencyKey: "client-message-timeout",
      onEvent: () => undefined
    })).rejects.toThrow("Hermes did not complete within 1 seconds.");
  });
});
