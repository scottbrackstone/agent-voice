import { randomUUID } from "node:crypto";
import { mkdirSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, resolve } from "node:path";
import { EventEmitter } from "node:events";

type DatabaseSyncInstance = {
  exec(sql: string): void;
  prepare(sql: string): {
    run: (...params: unknown[]) => unknown;
    get: (...params: unknown[]) => unknown;
    all: (...params: unknown[]) => unknown[];
  };
  close(): void;
};

type DatabaseSyncConstructor = new (location?: string) => DatabaseSyncInstance;

const require = createRequire(import.meta.url);
const { DatabaseSync } = require("node:sqlite") as {
  DatabaseSync: DatabaseSyncConstructor;
};

export type InboxDirection = "incoming" | "outgoing";
export type InboxStatus = "pending" | "delivered" | "read";

export type InboxMessage = {
  id: string;
  conversationId: string;
  externalEventId?: string;
  direction: InboxDirection;
  content: string;
  source: string;
  status: InboxStatus;
  timestamp: string;
  metadata: Record<string, unknown>;
};

export type CreateInboxMessageInput = {
  conversationId: string;
  externalEventId?: string;
  direction: InboxDirection;
  content: string;
  source?: string;
  status?: InboxStatus;
  timestamp?: string;
  metadata?: Record<string, unknown>;
};

type MessageRow = {
  id: string;
  conversation_id: string;
  external_event_id?: string | null;
  direction: InboxDirection;
  content: string;
  source: string;
  status: InboxStatus;
  timestamp: string;
  metadata: string | null;
};

export type RegisteredThread = {
  sessionId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
};

export type MessageRunStatus = "queued" | "running" | "completed" | "failed" | "cancelled";

export type MessageRun = {
  id: string;
  sessionId: string;
  clientMessageId: string;
  status: MessageRunStatus;
  replyText: string;
  error?: string;
  presentation?: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
};

type RunRow = {
  id: string;
  session_id: string;
  client_message_id: string;
  status: MessageRunStatus;
  reply_text: string;
  error: string | null;
  presentation: string | null;
  created_at: string;
  updated_at: string;
};

export class RelayInbox {
  private readonly db: DatabaseSyncInstance;
  private readonly events = new EventEmitter();

  constructor(path = process.env.AGENTVOICE_INBOX_DB_PATH ?? "data/agentvoice-inbox.sqlite") {
    const dbPath = path === ":memory:" ? path : resolve(path);
    if (dbPath !== ":memory:") {
      mkdirSync(dirname(dbPath), { recursive: true });
    }
    this.db = new DatabaseSync(dbPath);
    this.migrate();
  }

  createMessage(input: CreateInboxMessageInput): InboxMessage {
    const message: InboxMessage = {
      id: `msg_${randomUUID()}`,
      conversationId: input.conversationId,
      externalEventId: input.externalEventId,
      direction: input.direction,
      content: input.content,
      source: input.source ?? (input.direction === "incoming" ? "user" : "hermes"),
      status: input.status ?? "pending",
      timestamp: input.timestamp ?? new Date().toISOString(),
      metadata: input.metadata ?? {}
    };

    this.db.prepare(
      `
      INSERT INTO messages (
        id, conversation_id, external_event_id, direction, content, source, status, timestamp, metadata
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      `
    ).run(
      message.id,
      message.conversationId,
      message.externalEventId ?? null,
      message.direction,
      message.content,
      message.source,
      message.status,
      message.timestamp,
      JSON.stringify(message.metadata)
    );

    this.events.emit(message.conversationId, message);
    return message;
  }

  findByExternalEventId(externalEventId: string): InboxMessage | null {
    const row = this.db.prepare(
      "SELECT * FROM messages WHERE external_event_id = ? LIMIT 1"
    ).get(externalEventId) as MessageRow | undefined;
    return row ? rowToMessage(row) : null;
  }

  registerThread(sessionId: string, title: string): RegisteredThread {
    const now = new Date().toISOString();
    this.db.prepare(
      `
      INSERT INTO agentvoice_threads (session_id, title, created_at, updated_at)
      VALUES (?, ?, ?, ?)
      ON CONFLICT(session_id) DO UPDATE SET title = excluded.title, updated_at = excluded.updated_at
      `
    ).run(sessionId, title, now, now);
    return this.getRegisteredThread(sessionId)!;
  }

  getRegisteredThread(sessionId: string): RegisteredThread | null {
    const row = this.db.prepare(
      "SELECT * FROM agentvoice_threads WHERE session_id = ? LIMIT 1"
    ).get(sessionId) as {
      session_id: string;
      title: string;
      created_at: string;
      updated_at: string;
    } | undefined;
    return row ? {
      sessionId: row.session_id,
      title: row.title,
      createdAt: row.created_at,
      updatedAt: row.updated_at
    } : null;
  }

  listRegisteredThreads(): RegisteredThread[] {
    const rows = this.db.prepare(
      "SELECT * FROM agentvoice_threads ORDER BY updated_at DESC"
    ).all() as Array<{
      session_id: string;
      title: string;
      created_at: string;
      updated_at: string;
    }>;
    return rows.map((row) => ({
      sessionId: row.session_id,
      title: row.title,
      createdAt: row.created_at,
      updatedAt: row.updated_at
    }));
  }

  createRun(input: {
    sessionId: string;
    clientMessageId: string;
  }): { run: MessageRun; created: boolean } {
    const existing = this.getRunByClientMessageId(input.clientMessageId);
    if (existing) return { run: existing, created: false };
    const now = new Date().toISOString();
    const id = `run_${randomUUID()}`;
    this.db.prepare(
      `
      INSERT INTO message_runs (
        id, session_id, client_message_id, status, reply_text, created_at, updated_at
      ) VALUES (?, ?, ?, 'queued', '', ?, ?)
      `
    ).run(id, input.sessionId, input.clientMessageId, now, now);
    return { run: this.getRun(id)!, created: true };
  }

  getRun(runId: string): MessageRun | null {
    const row = this.db.prepare("SELECT * FROM message_runs WHERE id = ? LIMIT 1")
      .get(runId) as RunRow | undefined;
    return row ? rowToRun(row) : null;
  }

  getRunByClientMessageId(clientMessageId: string): MessageRun | null {
    const row = this.db.prepare(
      "SELECT * FROM message_runs WHERE client_message_id = ? LIMIT 1"
    ).get(clientMessageId) as RunRow | undefined;
    return row ? rowToRun(row) : null;
  }

  updateRun(
    runId: string,
    patch: {
      status: MessageRunStatus;
      replyText?: string;
      error?: string;
      presentation?: Record<string, unknown>;
    }
  ): MessageRun | null {
    const current = this.getRun(runId);
    if (!current) return null;
    const now = new Date().toISOString();
    this.db.prepare(
      `
      UPDATE message_runs
      SET status = ?, reply_text = ?, error = ?, presentation = ?, updated_at = ?
      WHERE id = ?
      `
    ).run(
      patch.status,
      patch.replyText ?? current.replyText,
      patch.error ?? null,
      JSON.stringify(patch.presentation ?? current.presentation ?? {}),
      now,
      runId
    );
    return this.getRun(runId);
  }

  upsertDeviceToken(installationId: string, token: string): void {
    this.db.prepare(
      `
      INSERT INTO device_tokens (installation_id, token, platform, updated_at)
      VALUES (?, ?, 'android', ?)
      ON CONFLICT(installation_id) DO UPDATE SET token = excluded.token, updated_at = excluded.updated_at
      `
    ).run(installationId, token, new Date().toISOString());
  }

  removeDeviceToken(installationId: string): boolean {
    const result = this.db.prepare(
      "DELETE FROM device_tokens WHERE installation_id = ?"
    ).run(installationId) as { changes?: number };
    return Number(result.changes ?? 0) > 0;
  }

  listDeviceTokens(): string[] {
    const rows = this.db.prepare("SELECT token FROM device_tokens ORDER BY updated_at DESC")
      .all() as Array<{ token: string }>;
    return rows.map((row) => row.token);
  }

  listMessages(
    conversationId: string,
    options: { since?: string; limit?: number; pendingOnly?: boolean } = {}
  ): InboxMessage[] {
    const since = options.since ?? thirtyDaysAgo();
    const limit = clampLimit(options.limit);
    const statusClause = options.pendingOnly ? "AND status = 'pending'" : "";
    const rows = this.db.prepare(
      `
      SELECT * FROM messages
      WHERE conversation_id = ?
      AND direction = 'outgoing'
      AND timestamp > ?
      ${statusClause}
      ORDER BY timestamp ASC
      LIMIT ?
      `
    ).all(conversationId, since, limit) as MessageRow[];

    return rows.map(rowToMessage);
  }

  history(conversationId: string, options: { before?: string; limit?: number } = {}): InboxMessage[] {
    const before = options.before ?? new Date().toISOString();
    const limit = clampLimit(options.limit);
    const rows = this.db.prepare(
      `
      SELECT * FROM messages
      WHERE conversation_id = ?
      AND timestamp <= ?
      ORDER BY timestamp DESC
      LIMIT ?
      `
    ).all(conversationId, before, limit) as MessageRow[];

    return rows.reverse().map(rowToMessage);
  }

  hasConversation(conversationId: string): boolean {
    const row = this.db.prepare(
      "SELECT 1 AS exists_flag FROM messages WHERE conversation_id = ? LIMIT 1"
    ).get(conversationId) as { exists_flag?: number } | undefined;
    return Boolean(row?.exists_flag);
  }

  ackMessage(id: string): boolean {
    const result = this.db.prepare(
      "UPDATE messages SET status = 'delivered' WHERE id = ?"
    ).run(id) as { changes?: number };
    return Number(result.changes ?? 0) > 0;
  }

  onMessage(conversationId: string, handler: (message: InboxMessage) => void): () => void {
    this.events.on(conversationId, handler);
    return () => this.events.off(conversationId, handler);
  }

  close(): void {
    this.db.close();
  }

  private migrate(): void {
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS messages (
        id TEXT PRIMARY KEY,
        conversation_id TEXT NOT NULL,
        external_event_id TEXT,
        direction TEXT NOT NULL,
        content TEXT NOT NULL,
        source TEXT NOT NULL DEFAULT 'user',
        status TEXT NOT NULL DEFAULT 'pending',
        timestamp TEXT NOT NULL,
        metadata TEXT DEFAULT '{}',
        created_at TEXT DEFAULT (datetime('now'))
      );
      CREATE INDEX IF NOT EXISTS idx_messages_conv_time
        ON messages(conversation_id, timestamp);
      CREATE INDEX IF NOT EXISTS idx_messages_status
        ON messages(conversation_id, status);
      CREATE TABLE IF NOT EXISTS agentvoice_threads (
        session_id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL
      );
      CREATE TABLE IF NOT EXISTS message_runs (
        id TEXT PRIMARY KEY,
        session_id TEXT NOT NULL,
        client_message_id TEXT NOT NULL UNIQUE,
        status TEXT NOT NULL,
        reply_text TEXT NOT NULL DEFAULT '',
        error TEXT,
        presentation TEXT DEFAULT '{}',
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL
      );
      CREATE INDEX IF NOT EXISTS idx_message_runs_session
        ON message_runs(session_id, created_at);
      CREATE TABLE IF NOT EXISTS device_tokens (
        installation_id TEXT PRIMARY KEY,
        token TEXT NOT NULL UNIQUE,
        platform TEXT NOT NULL,
        updated_at TEXT NOT NULL
      );
    `);
    this.ensureMessagesExternalEventColumn();
    this.db.exec(
      "CREATE UNIQUE INDEX IF NOT EXISTS idx_messages_external_event " +
      "ON messages(external_event_id) WHERE external_event_id IS NOT NULL"
    );
  }

  private ensureMessagesExternalEventColumn(): void {
    const columns = this.db.prepare("PRAGMA table_info(messages)").all() as Array<{ name?: string }>;
    if (!columns.some((column) => column.name === "external_event_id")) {
      this.db.exec("ALTER TABLE messages ADD COLUMN external_event_id TEXT");
    }
  }
}

export function messageToWire(message: InboxMessage): Record<string, unknown> {
  return {
    id: message.id,
    direction: message.direction,
    content: message.content,
    source: message.source,
    status: message.status,
    timestamp: message.timestamp,
    metadata: message.metadata
  };
}

function rowToMessage(row: MessageRow): InboxMessage {
  return {
    id: row.id,
    conversationId: row.conversation_id,
    externalEventId: row.external_event_id ?? undefined,
    direction: row.direction,
    content: row.content,
    source: row.source,
    status: row.status,
    timestamp: row.timestamp,
    metadata: parseMetadata(row.metadata)
  };
}

function rowToRun(row: RunRow): MessageRun {
  return {
    id: row.id,
    sessionId: row.session_id,
    clientMessageId: row.client_message_id,
    status: row.status,
    replyText: row.reply_text,
    error: row.error ?? undefined,
    presentation: parseMetadata(row.presentation),
    createdAt: row.created_at,
    updatedAt: row.updated_at
  };
}

function parseMetadata(metadata: string | null): Record<string, unknown> {
  if (!metadata) {
    return {};
  }
  try {
    const parsed = JSON.parse(metadata);
    return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
}

function clampLimit(limit: number | undefined): number {
  if (!Number.isFinite(limit)) {
    return 20;
  }
  const numericLimit = limit ?? 20;
  return Math.max(1, Math.min(100, Math.trunc(numericLimit)));
}

function thirtyDaysAgo(): string {
  return new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString();
}
