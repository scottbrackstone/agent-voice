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
  direction: InboxDirection;
  content: string;
  source: string;
  status: InboxStatus;
  timestamp: string;
  metadata: Record<string, unknown>;
};

export type CreateInboxMessageInput = {
  conversationId: string;
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
  direction: InboxDirection;
  content: string;
  source: string;
  status: InboxStatus;
  timestamp: string;
  metadata: string | null;
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
        id, conversation_id, direction, content, source, status, timestamp, metadata
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      `
    ).run(
      message.id,
      message.conversationId,
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
    `);
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
    direction: row.direction,
    content: row.content,
    source: row.source,
    status: row.status,
    timestamp: row.timestamp,
    metadata: parseMetadata(row.metadata)
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
