package com.agentvoice.app.model

data class ChatThread(
    val id: String,
    val remoteId: String?,
    val title: String,
    val connector: ConnectorType,
    val mode: AgentMode,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class ChatMessage(
    val id: String,
    val threadId: String,
    val role: ChatMessageRole,
    val text: String,
    val status: ChatMessageStatus,
    val requestId: String?,
    val queuedActionsSummary: String,
    val error: String?,
    val timestampMillis: Long
)

enum class ChatMessageRole(val wireValue: String, val label: String) {
    User("user", "You"),
    Assistant("assistant", "AgentVoice"),
    System("system", "System");

    companion object {
        fun fromWireValue(value: String): ChatMessageRole =
            entries.firstOrNull { it.wireValue == value } ?: System
    }
}

enum class ChatMessageStatus(val wireValue: String, val label: String) {
    Pending("pending", "pending"),
    Sending("sending", "sending"),
    Sent("sent", "sent"),
    Delivered("delivered", "delivered"),
    Read("read", "read"),
    Completed("completed", "done"),
    Queued("queued", "queued"),
    Failed("failed", "failed"),
    Partial("partial", "partial");

    companion object {
        fun fromWireValue(value: String): ChatMessageStatus =
            entries.firstOrNull { it.wireValue == value } ?: Failed

        fun fromAgentStatus(status: AgentStatus): ChatMessageStatus =
            when (status) {
                AgentStatus.Completed -> Completed
                AgentStatus.Queued -> Queued
                AgentStatus.Failed -> Failed
                AgentStatus.Partial -> Partial
            }
    }
}
