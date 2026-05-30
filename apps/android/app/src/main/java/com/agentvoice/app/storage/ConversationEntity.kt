package com.agentvoice.app.storage

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.agentvoice.app.model.AgentMode
import com.agentvoice.app.model.AgentStatus
import com.agentvoice.app.model.ConnectorType
import com.agentvoice.app.model.ConversationRecord

@Entity(tableName = "conversation_interactions")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val transcript: String,
    val reply: String,
    val mode: String,
    val connector: String,
    val status: String,
    val queuedActionsSummary: String,
    val timestampMillis: Long,
    val error: String?
) {
    fun toRecord(): ConversationRecord =
        ConversationRecord(
            id = id,
            transcript = transcript,
            reply = reply,
            mode = AgentMode.fromWireValue(mode),
            connector = ConnectorType.fromValue(connector),
            status = AgentStatus.fromWireValue(status),
            queuedActionsSummary = queuedActionsSummary,
            timestampMillis = timestampMillis,
            error = error
        )
}

