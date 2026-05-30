package com.agentvoice.app.model

data class ConversationRecord(
    val id: String,
    val transcript: String,
    val reply: String,
    val mode: AgentMode,
    val connector: ConnectorType,
    val status: AgentStatus,
    val queuedActionsSummary: String,
    val timestampMillis: Long,
    val error: String?
)

