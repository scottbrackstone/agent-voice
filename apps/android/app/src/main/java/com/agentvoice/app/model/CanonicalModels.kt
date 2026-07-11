package com.agentvoice.app.model

data class CanonicalThread(
    val id: String,
    val title: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val messageCount: Int
)

data class CanonicalMessage(
    val id: String,
    val threadId: String,
    val role: String,
    val content: String,
    val timestampMillis: Long,
    val finishReason: String?,
    val toolName: String?
)

data class MessageRunStart(
    val runId: String,
    val threadId: String,
    val clientMessageId: String,
    val status: String,
    val duplicate: Boolean
)

data class MessageRunSnapshot(
    val id: String,
    val sessionId: String,
    val clientMessageId: String,
    val status: String,
    val replyText: String,
    val error: String?,
    val presentation: AgentPresentation
)

data class AgentPresentation(
    val mood: String = "neutral",
    val intensity: Float = 0f,
    val state: String = "idle",
    val motionHint: String = "idle"
)

sealed interface RunStreamEvent {
    data class Started(val runId: String) : RunStreamEvent
    data class Delta(val text: String) : RunStreamEvent
    data class ToolProgress(val label: String, val status: String) : RunStreamEvent
    data class Presentation(val value: AgentPresentation) : RunStreamEvent
    data class Completed(
        val text: String,
        val presentation: AgentPresentation
    ) : RunStreamEvent
    data class Failed(val error: String) : RunStreamEvent
    data object Cancelled : RunStreamEvent
}

