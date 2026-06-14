import type { AgentConnector, AgentConnectorInput } from "./AgentConnector.js";
import type { AgentResponse } from "../types/agent.js";

export type HermesConfig = {
  enabled: boolean;
  baseUrl?: string;
  token?: string;
  model: string;
  user: string;
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
const DEFAULT_MODEL = "hermes";
const DEFAULT_USER = "agentvoice-mobile";

export class HermesConnector implements AgentConnector {
  readonly name = "hermes" as const;

  constructor(
    private readonly config: HermesConfig = readHermesConfig(),
    private readonly fetchImpl: FetchLike = globalThis.fetch as FetchLike
  ) {}

  async sendMessage(input: AgentConnectorInput): Promise<AgentResponse> {
    if (!this.config.enabled) {
      return this.failure(input, "Hermes connector is disabled.", [
        "Set HERMES_ENABLED=true after configuring the Hermes chat endpoint."
      ]);
    }

    const baseUrl = this.normalizedBaseUrl();
    if (!baseUrl) {
      return this.failure(input, "Hermes connector is missing a valid base URL.", [
        "Set HERMES_BASE_URL to the base URL of the Hermes chat service."
      ]);
    }

    return await this.sendChatCompletion(input, baseUrl);
  }

  private normalizedBaseUrl(): URL | null {
    if (!this.config.baseUrl) {
      return null;
    }

    try {
      const withProtocol = /^https?:\/\//i.test(this.config.baseUrl)
        ? this.config.baseUrl
        : `http://${this.config.baseUrl}`;
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
    baseUrl: URL
  ): Promise<AgentResponse> {
    const basePath = baseUrl.pathname.replace(/\/+$/, "");
    const completionsUrl = new URL(`${basePath}/v1/chat/completions`, baseUrl);
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), this.config.timeoutMs);

    try {
      const headers: Record<string, string> = {
        "Content-Type": "application/json"
      };
      if (this.config.token) {
        headers.Authorization = `Bearer ${this.config.token}`;
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
              content: buildAgentVoiceInstruction(input.mode)
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
        return this.failure(input, `Hermes returned HTTP ${response.status}.`, [
          truncateForWarning(body)
        ]);
      }

      try {
        return this.success(input, extractAssistantReply(body));
      } catch (error) {
        return this.failure(input, "Hermes response was not a chat completion.", [
          errorMessage(error),
          "Confirm Hermes exposes an OpenAI-compatible /v1/chat/completions endpoint."
        ]);
      }
    } catch (error) {
      return this.failure(input, "Unable to reach Hermes.", [errorMessage(error)]);
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

export function readHermesConfig(env: NodeJS.ProcessEnv = process.env): HermesConfig {
  return {
    enabled: env.HERMES_ENABLED === "true",
    baseUrl: emptyToUndefined(env.HERMES_BASE_URL),
    token: emptyToUndefined(env.HERMES_TOKEN),
    model: emptyToUndefined(env.HERMES_MODEL) ?? DEFAULT_MODEL,
    user: emptyToUndefined(env.HERMES_USER) ?? DEFAULT_USER,
    timeoutMs: parseTimeout(env.HERMES_TIMEOUT_MS)
  };
}

function extractAssistantReply(body: string): string {
  try {
    const parsed = JSON.parse(body) as OpenAiChatCompletionResponse;
    const content = parsed.choices?.[0]?.message?.content;
    if (typeof content === "string" && content.trim()) {
      return content.trim();
    }
  } catch (error) {
    throw new Error("Hermes response was not valid JSON.", { cause: error });
  }

  throw new Error("Hermes response did not include assistant text.");
}

function buildAgentVoiceInstruction(mode: AgentConnectorInput["mode"]): string {
  return [
    `AgentVoice mode: ${mode}.`,
    "Reply with concise plain text suitable for text-to-speech.",
    "AgentVoice follows Capture now. Confirm later.",
    "Do not perform destructive, purchase, payment, or external-send actions automatically from a mobile voice request.",
    "When an action needs caution, draft or describe it for later review."
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
