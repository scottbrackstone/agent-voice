import type { AgentMode } from "../types/agent.js";
import { readHermesConfig, type HermesConfig } from "./HermesConnector.js";

export type HermesSession = {
  id: string;
  title: string;
  isUntitled: boolean;
  startedAt?: number;
  endedAt?: number;
  messageCount?: number;
};

export type CreateHermesSessionInput = {
  id?: string;
  title?: string;
};

export type HermesHistoryMessage = {
  id: string;
  sessionId: string;
  role: string;
  content: string;
  timestamp: number;
  finishReason?: string;
  toolName?: string;
};

export type PresentationState = {
  mood: "neutral" | "happy" | "excited" | "concerned" | "focused" | "calm" | "playful";
  intensity: number;
  state: "idle" | "listening" | "thinking" | "tool" | "speaking" | "success" | "error";
  motionHint: "idle" | "listen" | "think" | "speak" | "celebrate" | "reassure";
};

export type HermesStreamEvent =
  | { type: "delta"; text: string }
  | { type: "tool.progress"; data: Record<string, unknown> }
  | { type: "presentation"; presentation: PresentationState };

export type StreamMessageInput = {
  sessionId: string;
  message: string;
  mode: AgentMode;
  idempotencyKey: string;
  signal?: AbortSignal;
  onEvent(event: HermesStreamEvent): void;
};

export interface HermesApi {
  readonly enabled: boolean;
  createSession(input?: CreateHermesSessionInput): Promise<HermesSession>;
  getSession(sessionId: string): Promise<HermesSession>;
  listSessions(): Promise<HermesSession[]>;
  renameSession(sessionId: string, title: string): Promise<HermesSession>;
  listMessages(sessionId: string, options?: { before?: number; limit?: number }): Promise<HermesHistoryMessage[]>;
  streamMessage(input: StreamMessageInput): Promise<{ text: string; presentation: PresentationState }>;
}

const NEUTRAL_PRESENTATION: PresentationState = {
  mood: "neutral",
  intensity: 0,
  state: "success",
  motionHint: "idle"
};

export class HermesApiClient implements HermesApi {
  readonly enabled: boolean;

  constructor(
    private readonly config: HermesConfig = readHermesConfig(),
    private readonly fetchImpl: typeof globalThis.fetch = globalThis.fetch
  ) {
    this.enabled = config.enabled;
  }

  async createSession(input: CreateHermesSessionInput = {}): Promise<HermesSession> {
    const body: Record<string, string> = {};
    if (input.id) body.id = input.id;
    if (input.title) body.title = input.title;
    const json = await this.requestJson("/api/sessions", {
      method: "POST",
      body: JSON.stringify(body)
    });
    return parseSession(json);
  }

  async getSession(sessionId: string): Promise<HermesSession> {
    const json = await this.requestJson(`/api/sessions/${encodeURIComponent(sessionId)}`, {
      method: "GET"
    });
    return parseSession(json);
  }

  async listSessions(): Promise<HermesSession[]> {
    const json = await this.requestJson("/api/sessions", { method: "GET" });
    const record = asRecord(json);
    const values = Array.isArray(record.data)
      ? record.data
      : Array.isArray(record.sessions)
        ? record.sessions
        : [];
    return values.map(parseSession);
  }

  async renameSession(sessionId: string, title: string): Promise<HermesSession> {
    const json = await this.requestJson(`/api/sessions/${encodeURIComponent(sessionId)}`, {
      method: "PATCH",
      body: JSON.stringify({ title })
    });
    return parseSession(json);
  }

  async listMessages(
    sessionId: string,
    options: { before?: number; limit?: number } = {}
  ): Promise<HermesHistoryMessage[]> {
    const query = new URLSearchParams();
    if (options.before != null) query.set("before", String(options.before));
    if (options.limit != null) query.set("limit", String(options.limit));
    const suffix = query.size ? `?${query.toString()}` : "";
    const json = await this.requestJson(
      `/api/sessions/${encodeURIComponent(sessionId)}/messages${suffix}`,
      { method: "GET" }
    );
    const record = asRecord(json);
    const values = Array.isArray(record.data) ? record.data : [];
    return values.map((value) => parseHistoryMessage(value, sessionId));
  }

  async streamMessage(input: StreamMessageInput): Promise<{
    text: string;
    presentation: PresentationState;
  }> {
    this.assertEnabled();
    const url = this.urlFor("/v1/chat/completions");
    const headers = this.headers({
      "Idempotency-Key": input.idempotencyKey,
      "X-Hermes-Session-Id": input.sessionId,
      "X-Hermes-Session-Key": `agentvoice:${input.sessionId}`,
      "X-Hermes-Client-Capabilities": "presentation-v1"
    });
    const timeoutSignal = AbortSignal.timeout(this.config.timeoutMs);
    const requestSignal = input.signal
      ? AbortSignal.any([input.signal, timeoutSignal])
      : timeoutSignal;
    let response: Response;
    try {
      response = await this.fetchImpl(url, {
        method: "POST",
        headers,
        body: JSON.stringify({
          model: this.config.model,
          stream: true,
          user: input.sessionId,
          messages: [
            {
              role: "system",
              content: buildAgentVoiceInstruction(input.mode, input.sessionId)
            },
            { role: "user", content: input.message }
          ]
        }),
        signal: requestSignal
      });
    } catch (error) {
      throw streamRequestError(error, timeoutSignal, input.signal, this.config.timeoutMs);
    }

    if (!response.ok) {
      throw new HermesApiError(
        `Hermes returned HTTP ${response.status}.`,
        response.status,
        (await response.text()).trim().slice(0, 500)
      );
    }
    if (!response.body) {
      throw new HermesApiError("Hermes streaming response had no body.", 502);
    }

    let text = "";
    let presentation = NEUTRAL_PRESENTATION;
    let buffer = "";
    let eventName = "message";
    const decoder = new TextDecoder();

    try {
      for await (const chunk of response.body) {
        buffer += decoder.decode(chunk, { stream: true });
        while (true) {
          const separator = buffer.indexOf("\n\n");
          if (separator < 0) break;
          const block = buffer.slice(0, separator).replace(/\r/g, "");
          buffer = buffer.slice(separator + 2);
          eventName = "message";
          const dataLines: string[] = [];
          for (const line of block.split("\n")) {
            if (line.startsWith("event:")) eventName = line.slice(6).trim();
            if (line.startsWith("data:")) dataLines.push(line.slice(5).trimStart());
          }
          const data = dataLines.join("\n");
          if (!data || data === "[DONE]") continue;

          const parsed = safeJson(data);
          if (eventName === "hermes.tool.progress") {
            const tool = asRecord(parsed);
            input.onEvent({ type: "tool.progress", data: tool });
            continue;
          }
          if (eventName === "hermes.presentation") {
            presentation = parsePresentation(parsed);
            input.onEvent({ type: "presentation", presentation });
            continue;
          }

          const record = asRecord(parsed);
          if (record.presentation) {
            presentation = parsePresentation(record.presentation);
            input.onEvent({ type: "presentation", presentation });
          }
          const delta = extractDelta(record);
          if (delta) {
            text += delta;
            input.onEvent({ type: "delta", text: delta });
          }
        }
      }
    } catch (error) {
      throw streamRequestError(error, timeoutSignal, input.signal, this.config.timeoutMs);
    }

    return { text: text.trim(), presentation };
  }

  private async requestJson(
    path: string,
    init: { method: string; body?: string }
  ): Promise<unknown> {
    this.assertEnabled();
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), this.config.timeoutMs);
    try {
      const response = await this.fetchImpl(this.urlFor(path), {
        ...init,
        headers: this.headers(),
        signal: controller.signal
      });
      const body = await response.text();
      if (!response.ok) {
        const upstreamError = parseHermesErrorBody(body);
        throw new HermesApiError(
          upstreamError.message ?? `Hermes returned HTTP ${response.status}.`,
          response.status,
          upstreamError.message ?? body.trim().slice(0, 500),
          upstreamError.code
        );
      }
      return safeJson(body);
    } catch (error) {
      if (error instanceof HermesApiError) throw error;
      if (controller.signal.aborted) {
        throw new HermesApiError(
          `Hermes did not respond within ${Math.max(1, Math.round(this.config.timeoutMs / 1000))} seconds.`,
          504,
          undefined,
          "hermes_timeout"
        );
      }
      throw new HermesApiError(
        "Unable to reach Hermes.",
        502,
        error instanceof Error ? error.message : undefined,
        "hermes_unreachable"
      );
    } finally {
      clearTimeout(timeout);
    }
  }

  private headers(extra: Record<string, string> = {}): Record<string, string> {
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...extra
    };
    if (this.config.token) headers.Authorization = `Bearer ${this.config.token}`;
    return headers;
  }

  private urlFor(path: string): string {
    const raw = this.config.baseUrl?.trim();
    if (!raw) throw new HermesApiError("Hermes base URL is not configured.", 503);
    const withProtocol = /^https?:\/\//i.test(raw) ? raw : `http://${raw}`;
    const base = new URL(withProtocol);
    const basePath = base.pathname.replace(/\/+$/, "");
    return new URL(`${basePath}${path}`, base).toString();
  }

  private assertEnabled(): void {
    if (!this.enabled) throw new HermesApiError("Hermes connector is disabled.", 503);
  }
}

export class HermesApiError extends Error {
  constructor(
    message: string,
    readonly statusCode: number,
    readonly detail?: string,
    readonly code?: string
  ) {
    super(message);
  }
}

function parseSession(value: unknown): HermesSession {
  const root = asRecord(value);
  const record = root.session && typeof root.session === "object" ? asRecord(root.session) : root;
  const id = stringValue(record.id) ?? stringValue(record.session_id);
  if (!id) throw new HermesApiError("Hermes session response did not include an ID.", 502);
  const title = stringValue(record.title);
  return {
    id,
    title: title ?? "New chat",
    isUntitled: title == null,
    startedAt: numberValue(record.started_at),
    endedAt: numberValue(record.ended_at),
    messageCount: numberValue(record.message_count)
  };
}

function parseHermesErrorBody(value: string): { message?: string; code?: string } {
  try {
    const root = asRecord(JSON.parse(value));
    const nested = asRecord(root.error);
    return {
      message: stringValue(nested.message) ?? stringValue(root.message),
      code: stringValue(nested.code) ?? stringValue(root.code)
    };
  } catch {
    return {};
  }
}

function parseHistoryMessage(value: unknown, fallbackSessionId: string): HermesHistoryMessage {
  const record = asRecord(value);
  return {
    id: String(record.id ?? `${fallbackSessionId}:${record.timestamp ?? Date.now()}`),
    sessionId: stringValue(record.session_id) ?? fallbackSessionId,
    role: stringValue(record.role) ?? "system",
    content: stringValue(record.content) ?? "",
    timestamp: numberValue(record.timestamp) ?? Date.now() / 1000,
    finishReason: stringValue(record.finish_reason),
    toolName: stringValue(record.tool_name)
  };
}

function parsePresentation(value: unknown): PresentationState {
  const record = asRecord(value);
  const moods = ["neutral", "happy", "excited", "concerned", "focused", "calm", "playful"] as const;
  const states = ["idle", "listening", "thinking", "tool", "speaking", "success", "error"] as const;
  const motions = ["idle", "listen", "think", "speak", "celebrate", "reassure"] as const;
  return {
    mood: enumValue(record.mood, moods, "neutral"),
    intensity: Math.max(0, Math.min(1, numberValue(record.intensity) ?? 0)),
    state: enumValue(record.state, states, "success"),
    motionHint: enumValue(record.motionHint ?? record.motion_hint, motions, "idle")
  };
}

function extractDelta(record: Record<string, unknown>): string {
  const choices = Array.isArray(record.choices) ? record.choices : [];
  const choice = choices[0] ? asRecord(choices[0]) : {};
  const delta = asRecord(choice.delta);
  return stringValue(delta.content) ?? "";
}

function buildAgentVoiceInstruction(mode: AgentMode, sessionId: string): string {
  return [
    `AgentVoice mode: ${mode}.`,
    "Reply in clear text suitable for both chat and text-to-speech.",
    `When creating scheduled jobs for this client, set deliver to agentvoice:${sessionId}.`,
    "Capture now. Confirm later: risky external actions require explicit confirmation."
  ].join(" ");
}

function streamRequestError(
  error: unknown,
  timeoutSignal: AbortSignal,
  callerSignal: AbortSignal | undefined,
  timeoutMs: number
): Error {
  if (timeoutSignal.aborted && !callerSignal?.aborted) {
    return new HermesApiError(
      `Hermes did not complete within ${Math.max(1, Math.round(timeoutMs / 1000))} seconds.`,
      504
    );
  }
  return error instanceof Error ? error : new HermesApiError("Hermes stream failed.", 502);
}

function safeJson(value: string): unknown {
  try {
    return JSON.parse(value);
  } catch {
    throw new HermesApiError("Hermes returned invalid JSON.", 502, value.slice(0, 240));
  }
}

function asRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" && !Array.isArray(value)
    ? value as Record<string, unknown>
    : {};
}

function stringValue(value: unknown): string | undefined {
  return typeof value === "string" && value.trim() ? value.trim() : undefined;
}

function numberValue(value: unknown): number | undefined {
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}

function enumValue<T extends string>(
  value: unknown,
  allowed: readonly T[],
  fallback: T
): T {
  return typeof value === "string" && allowed.includes(value as T) ? value as T : fallback;
}
