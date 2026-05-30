package com.agentvoice.app.storage

import android.content.Context
import com.agentvoice.app.model.AgentMode
import com.agentvoice.app.model.AgentResponse
import com.agentvoice.app.model.AgentStatus
import com.agentvoice.app.model.ConnectorType
import com.agentvoice.app.model.ConversationRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ConversationRepository(context: Context) {
    private val dao = AgentVoiceDatabase.get(context).conversationDao()

    val recentConversations: Flow<List<ConversationRecord>> =
        dao.observeRecent().map { entities -> entities.map { it.toRecord() } }

    suspend fun saveResponse(transcript: String, response: AgentResponse) {
        dao.insert(
            ConversationEntity(
                id = UUID.randomUUID().toString(),
                transcript = transcript,
                reply = response.reply,
                mode = response.mode.wireValue,
                connector = response.connector.value,
                status = response.status.wireValue,
                queuedActionsSummary = response.queuedActions.joinToString(separator = "\n") {
                    "${it.type.label}: ${it.summary}"
                },
                timestampMillis = System.currentTimeMillis(),
                error = null
            )
        )
    }

    suspend fun saveFailure(
        transcript: String,
        mode: AgentMode,
        connector: ConnectorType,
        error: String
    ) {
        dao.insert(
            ConversationEntity(
                id = UUID.randomUUID().toString(),
                transcript = transcript,
                reply = "",
                mode = mode.wireValue,
                connector = connector.value,
                status = AgentStatus.Failed.wireValue,
                queuedActionsSummary = "",
                timestampMillis = System.currentTimeMillis(),
                error = error
            )
        )
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }
}

