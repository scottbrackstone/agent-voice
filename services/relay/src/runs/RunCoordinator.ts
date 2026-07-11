import { EventEmitter } from "node:events";
import type { RelayInbox, MessageRun } from "../db/inbox.js";
import type {
  HermesApi,
  HermesStreamEvent,
  PresentationState
} from "../connectors/HermesApiClient.js";
import type { AgentMode } from "../types/agent.js";
import type { PushNotifier } from "../push/PushNotifier.js";
import { derivePresentation } from "../presentation/derivePresentation.js";
import type { FastifyBaseLogger } from "fastify";

export type RunEvent = {
  sequence: number;
  type: "run.started" | "reply.delta" | "tool.progress" | "presentation" |
    "reply.completed" | "run.failed" | "run.cancelled";
  data: Record<string, unknown>;
  timestamp: string;
};

export class RunCoordinator {
  private readonly events = new EventEmitter();
  private readonly eventBuffers = new Map<string, RunEvent[]>();
  private readonly controllers = new Map<string, AbortController>();

  constructor(
    private readonly store: RelayInbox,
    private readonly hermes: HermesApi,
    private readonly push: PushNotifier,
    private readonly logger?: Pick<FastifyBaseLogger, "warn">
  ) {}

  start(input: {
    sessionId: string;
    clientMessageId: string;
    message: string;
    mode: AgentMode;
  }): { run: MessageRun; created: boolean } {
    const result = this.store.createRun(input);
    if (result.created) {
      const controller = new AbortController();
      this.controllers.set(result.run.id, controller);
      queueMicrotask(() => {
        void this.execute(result.run, input.message, input.mode, controller.signal);
      });
    }
    return result;
  }

  getRun(runId: string): MessageRun | null {
    return this.store.getRun(runId);
  }

  bufferedEvents(runId: string, afterSequence = 0): RunEvent[] {
    return (this.eventBuffers.get(runId) ?? []).filter((event) => event.sequence > afterSequence);
  }

  subscribe(runId: string, handler: (event: RunEvent) => void): () => void {
    this.events.on(runId, handler);
    return () => this.events.off(runId, handler);
  }

  cancel(runId: string): MessageRun | null {
    const run = this.store.getRun(runId);
    if (!run || !["queued", "running"].includes(run.status)) return run;
    this.controllers.get(runId)?.abort();
    const updated = this.store.updateRun(runId, { status: "cancelled" });
    this.emit(runId, "run.cancelled", { runId });
    return updated;
  }

  close(): void {
    for (const controller of this.controllers.values()) controller.abort();
    this.controllers.clear();
    this.events.removeAllListeners();
  }

  private async execute(
    run: MessageRun,
    message: string,
    mode: AgentMode,
    signal: AbortSignal
  ): Promise<void> {
    this.store.updateRun(run.id, { status: "running" });
    this.emit(run.id, "run.started", {
      runId: run.id,
      threadId: run.sessionId,
      clientMessageId: run.clientMessageId
    });
    this.emit(run.id, "presentation", {
      presentation: {
        mood: "focused",
        intensity: 0.35,
        state: "thinking",
        motionHint: "think"
      }
    });

    try {
      const result = await this.hermes.streamMessage({
        sessionId: run.sessionId,
        message,
        mode,
        idempotencyKey: run.clientMessageId,
        signal,
        onEvent: (event) => this.onHermesEvent(run.id, event)
      });
      if (signal.aborted) return;
      const presentation = derivePresentation(result.text, result.presentation);
      const updated = this.store.updateRun(run.id, {
        status: "completed",
        replyText: result.text,
        presentation
      });
      this.emit(run.id, "presentation", { presentation });
      this.emit(run.id, "reply.completed", {
        runId: run.id,
        threadId: run.sessionId,
        text: result.text,
        presentation
      });
      try {
        await this.push.notify({
          threadId: run.sessionId,
          messageId: updated?.id ?? run.id,
          title: "Solara",
          body: result.text || "A response is ready.",
          source: "hermes"
        });
      } catch (error) {
        this.logger?.warn(
          { error, runId: run.id, threadId: run.sessionId },
          "Hermes reply completed but push delivery failed"
        );
      }
    } catch (error) {
      if (signal.aborted) return;
      const messageText = error instanceof Error ? error.message : "Hermes run failed.";
      this.store.updateRun(run.id, { status: "failed", error: messageText });
      this.emit(run.id, "run.failed", {
        runId: run.id,
        threadId: run.sessionId,
        error: messageText
      });
    } finally {
      this.controllers.delete(run.id);
    }
  }

  private onHermesEvent(runId: string, event: HermesStreamEvent): void {
    if (event.type === "delta") {
      this.emit(runId, "reply.delta", { text: event.text });
    } else if (event.type === "tool.progress") {
      this.emit(runId, "tool.progress", event.data);
      this.emit(runId, "presentation", {
        presentation: {
          mood: "focused",
          intensity: 0.55,
          state: "tool",
          motionHint: "think"
        }
      });
    } else {
      this.emit(runId, "presentation", { presentation: event.presentation });
    }
  }

  private emit(runId: string, type: RunEvent["type"], data: Record<string, unknown>): void {
    const buffer = this.eventBuffers.get(runId) ?? [];
    const event: RunEvent = {
      sequence: (buffer.at(-1)?.sequence ?? 0) + 1,
      type,
      data,
      timestamp: new Date().toISOString()
    };
    buffer.push(event);
    if (buffer.length > 500) buffer.splice(0, buffer.length - 500);
    this.eventBuffers.set(runId, buffer);
    this.events.emit(runId, event);
  }
}

export const DEFAULT_PRESENTATION: PresentationState = {
  mood: "neutral",
  intensity: 0,
  state: "idle",
  motionHint: "idle"
};
