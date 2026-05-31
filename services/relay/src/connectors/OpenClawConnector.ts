import type { AgentConnector, AgentConnectorInput } from "./AgentConnector.js";
import type { AgentResponse } from "../types/agent.js";

export type OpenClawConfig = {
  enabled: boolean;
  gatewayUrl?: string;
  token?: string;
  timeoutMs: number;
};

type FetchLike = (
  input: string | URL,
  init?: {
    method?: string;
    headers?: Record<string, string>;
    signal?: AbortSignal;
  }
) => Promise<{
  ok: boolean;
  status: number;
  text(): Promise<string>;
}>;

type ProbeResult =
  | { kind: "companion-events-api"; detail: string }
  | { kind: "reachable-unknown"; detail: string }
  | { kind: "unreachable"; detail: string };

const DEFAULT_TIMEOUT_MS = 10_000;

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

    const probe = await this.probeKnownCompanionApi(gatewayUrl);

    return this.failure(input, "OpenClaw message protocol is not implemented yet.", [
      probe.detail,
      "The local OpenCLAW evidence only documents GET /events and POST /feedback.",
      "Those endpoints cannot carry an AgentVoice user message or return an AgentResponse.",
      "Provide the OpenClaw Gateway message endpoint, auth method, request shape, response shape, and timeout/error behavior."
    ]);
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

  private async probeKnownCompanionApi(gatewayUrl: URL): Promise<ProbeResult> {
    const basePath = gatewayUrl.pathname.replace(/\/+$/, "");
    const eventsUrl = new URL(`${basePath}/events`, gatewayUrl);
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), this.config.timeoutMs);

    try {
      const headers: Record<string, string> = {};
      if (this.config.token) {
        headers.Authorization = `Bearer ${this.config.token}`;
      }

      const response = await this.fetchImpl(eventsUrl, {
        method: "GET",
        headers,
        signal: controller.signal
      });
      const body = await response.text();

      if (response.ok && looksLikeCompanionEventsResponse(body)) {
        return {
          kind: "companion-events-api",
          detail: `Gateway responded at ${eventsUrl.toString()} with the known companion /events shape.`
        };
      }

      return {
        kind: "reachable-unknown",
        detail: `Gateway was reachable at ${eventsUrl.toString()} but did not expose the documented companion /events response; HTTP ${response.status}.`
      };
    } catch (error) {
      return {
        kind: "unreachable",
        detail: `Could not verify ${eventsUrl.toString()}: ${errorMessage(error)}`
      };
    } finally {
      clearTimeout(timeout);
    }
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
    timeoutMs: parseTimeout(env.OPENCLAW_TIMEOUT_MS)
  };
}

function looksLikeCompanionEventsResponse(body: string): boolean {
  try {
    const parsed = JSON.parse(body) as { events?: unknown };
    return Array.isArray(parsed.events);
  } catch {
    return false;
  }
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

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "unknown error";
}
