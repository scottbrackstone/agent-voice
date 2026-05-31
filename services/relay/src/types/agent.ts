import { z } from "zod";

export const AgentNameSchema = z.enum(["mock", "openclaw", "webhook", "hermes", "relay"]);

export const AgentModeSchema = z.enum(["normal", "mobile", "capture_only", "review_required"]);

export const QueuedActionTypeSchema = z.enum([
  "draft_message",
  "draft_email",
  "reminder",
  "note",
  "task",
  "unknown"
]);

export const QueuedActionStatusSchema = z.enum(["queued", "confirmed", "cancelled", "blocked"]);

export const AgentStatusSchema = z.enum(["completed", "queued", "failed", "partial"]);

export const AgentMessageRequestSchema = z.object({
  agent: AgentNameSchema.default("mock"),
  message: z.string().trim().min(1).max(8000),
  mode: AgentModeSchema.default("normal"),
  conversationId: z.string().trim().min(1).optional(),
  clientTimestamp: z.string().datetime().optional()
});

export const QueuedActionSchema = z.object({
  id: z.string().min(1),
  type: QueuedActionTypeSchema,
  summary: z.string().min(1),
  requiresConfirmation: z.boolean(),
  status: QueuedActionStatusSchema
});

export const AgentResponseSchema = z.object({
  reply: z.string(),
  status: AgentStatusSchema,
  mode: AgentModeSchema,
  queuedActions: z.array(QueuedActionSchema),
  warnings: z.array(z.string()),
  connector: AgentNameSchema,
  requestId: z.string().min(1)
});

export type AgentName = z.infer<typeof AgentNameSchema>;
export type AgentMode = z.infer<typeof AgentModeSchema>;
export type QueuedActionType = z.infer<typeof QueuedActionTypeSchema>;
export type QueuedAction = z.infer<typeof QueuedActionSchema>;
export type AgentMessageRequest = z.infer<typeof AgentMessageRequestSchema>;
export type AgentResponse = z.infer<typeof AgentResponseSchema>;

