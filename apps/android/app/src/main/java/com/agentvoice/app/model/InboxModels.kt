package com.agentvoice.app.model

data class RelayInboxMessage(
    val id: String,
    val conversationId: String,
    val direction: String,
    val content: String,
    val source: String,
    val status: String,
    val timestampMillis: Long,
    val metadataSummary: String
)
