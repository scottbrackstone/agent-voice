import type { AgentConnector, AgentConnectorInput } from "./AgentConnector.js";
import type { AgentResponse } from "../types/agent.js";

export type OpenClawConfig = {
  enabled: boolean;
  gatewayUrl?: string;
  token?: string;
  model: string;
  user: string;
  sessionKey?: string;
  agentId?: string;
  timeoutMs: number;
};

type FetchLike = (
  input: string | URL,
  init?: {
    method?: string;
    headers?: Record<string, string>;
    body?: string;
    signal?: AbortSignal;
  }
) => Promise<{
  ok: boolean;
  status: number;
  text(): Promise<string>;
}>;

type OpenAiChatCompletionResponse = {
  choices?: Array<{
    message?: {
      content?: unknown;
    };
  }>;
};

const DEFAULT_TIMEOUT_MS = 30_000;
const DEFAULT_MODEL = "openclaw";
const DEFAULT_USER = "agentvoice-mobile";

export class OpenClawConnector implements AgentConnector {
  readonly name = "openclaw" as const;

  constructor(
    private readonly config: OpenClawConfig = readOpenClawConfig(),
    private readonly fetchImpl: FetchLike = globalThis.fetch as FetchLike
  ) {}

  async sendMessage(input: AgentConnectorInput): Promise<AgentResponse> {
    if (!this.config.enabled) {
      return this.failure(input, "OpenClaw connector is disabled.", [
        "Set OPENCLAW_ENABLED=true only after an OpenClaw message Gateway is available."
      ]);
    }

    const gatewayUrl = this.normalizedGatewayUrl();

    if (!gatewayUrl) {
      return this.failure(input, "OpenClaw connector is missing a valid Gateway URL.", [
        "Set OPENCLAW_GATEWAY_URL to the base URL of the OpenClaw Gateway."
      ]);
    }

    return await this.sendChatCompletion(input, gatewayUrl);
  }

  private normalizedGatewayUrl(): URL | null {
    if (!this.config.gatewayUrl) {
      return null;
    }

    try {
      const withProtocol = /^https?:\/\//i.test(this.config.gatewayUrl)
        ? this.config.gatewayUrl
        : `http://${this.config.gatewayUrl}`;
      const url = new URL(withProtocol);
      url.pathname = url.pathname.replace(/\/+$/, "");
      url.search = "";
      url.hash = "";
      return url;
    } catch {
      return null;
    }
  }

  private async sendChatCompletion(
    input: AgentConnectorInput,
    gatewayUrl: URL
  ): Promise<AgentResponse> {
    const basePath = gatewayUrl.pathname.replace(/\/+$/, "");
    const completionsUrl = new URL(`${basePath}/v1/chat/completions`, gatewayUrl);
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), this.config.timeoutMs);

    try {
      const headers: Record<string, string> = {
        "Content-Type": "application/json"
      };
      if (this.config.token) {
        headers.Authorization = `Bearer ${this.config.token}`;
      }
      if (this.config.sessionKey) {
        headers["x-openclaw-session-key"] = this.config.sessionKey;
      }
      if (this.config.agentId) {
        headers["x-openclaw-agent-id"] = this.config.agentId;
      }

      const response = await this.fetchImpl(completionsUrl, {
        method: "POST",
        headers,
        body: JSON.stringify({
          model: this.config.model,
          user: input.conversationId ?? this.config.user,
          messages: [
            {
              role: "system",
              content: buildAgentVoiceSafetyInstruction(input.mode)
            },
            {
              role: "user",
              content: input.message
            }
          ]
        }),
        signal: controller.signal
      });
      const body = await response.text();

      if (!response.ok) {
        return this.failure(input, `OpenClaw Gateway returned HTTP ${response.status}.`, [
          truncateForWarning(body)
        ]);
      }

      try {
        return this.success(input, extractOpenClawReply(body));
      } catch (error) {
        return this.failure(input, "OpenClaw Gateway response was not a chat completion.", [
          errorMessage(error),
          "Confirm gateway.http.endpoints.chatCompletions.enabled is true and the OpenClaw Gateway has been restarted."
        ]);
      }
    } catch (error) {
      return this.failure(input, "Unable to reach OpenClaw Gateway.", [errorMessage(error)]);
    } finally {
      clearTimeout(timeout);
    }
  }

  private success(input: AgentConnectorInput, reply: string): AgentResponse {
    return {
      reply,
      status: "completed",
      mode: input.mode,
      queuedActions: [],
      warnings: [],
      connector: this.name,
      requestId: input.requestId
    };
  }

  private failure(input: AgentConnectorInput, reply: string, warnings: string[]): AgentResponse {
    return {
      reply,
      status: "failed",
      mode: input.mode,
      queuedActions: [],
      warnings,
      connector: this.name,
      requestId: input.requestId
    };
  }
}

export function readOpenClawConfig(env: NodeJS.ProcessEnv = process.env): OpenClawConfig {
  return {
    enabled: env.OPENCLAW_ENABLED === "true",
    gatewayUrl: emptyToUndefined(env.OPENCLAW_GATEWAY_URL),
    token: emptyToUndefined(env.OPENCLAW_TOKEN),
    model: emptyToUndefined(env.OPENCLAW_MODEL) ?? DEFAULT_MODEL,
    user: emptyToUndefined(env.OPENCLAW_USER) ?? DEFAULT_USER,
    sessionKey: emptyToUndefined(env.OPENCLAW_SESSION_KEY),
    agentId: emptyToUndefined(env.OPENCLAW_AGENT_ID),
    timeoutMs: parseTimeout(env.OPENCLAW_TIMEOUT_MS)
  };
}

function extractOpenClawReply(body: string): string {
  try {
    const parsed = JSON.parse(body) as OpenAiChatCompletionResponse;
    const content = parsed.choices?.[0]?.message?.content;
    if (typeof content === "string" && content.trim()) {
      return content.trim();
    }
  } catch (error) {
    throw new Error("OpenClaw Gateway response was not valid JSON.", { cause: error });
  }

  throw new Error("OpenClaw Gateway response did not include assistant text.");
}

function buildAgentVoiceSafetyInstruction(mode: AgentConnectorInput["mode"]): string {
  return [
    `AgentVoice mode: ${mode}.`,
    "Reply with concise plain text suitable for text-to-speech.",
    "AgentVoice follows Capture now. Confirm later.",
    "Do not send messages, delete data, make purchases, make payments, or perform destructive actions automatically from a mobile voice request.",
    "When an action is risky, draft it or describe it for later review."
  ].join(" ");
}

function parseTimeout(value: string | undefined): number {
  if (!value) {
    return DEFAULT_TIMEOUT_MS;
  }

  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed < 500) {
    return DEFAULT_TIMEOUT_MS;
  }

  return parsed;
}

function emptyToUndefined(value: string | undefined): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}

function truncateForWarning(value: string): string {
  const trimmed = value.trim();
  return trimmed.length > 500 ? `${trimmed.slice(0, 500)}...` : trimmed;
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "unknown error";
}
