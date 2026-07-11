package com.agentvoice.app.storage

import android.content.Context
import com.agentvoice.app.model.AgentMode
import com.agentvoice.app.model.AgentResponse
import com.agentvoice.app.model.AgentStatus
import com.agentvoice.app.model.ChatMessage
import com.agentvoice.app.model.ChatMessageRole
import com.agentvoice.app.model.ChatMessageStatus
import com.agentvoice.app.model.ChatThread
import com.agentvoice.app.model.ConnectorType
import com.agentvoice.app.model.ConversationRecord
import com.agentvoice.app.model.RelayInboxMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ConversationRepository(context: Context) {
    private val database = AgentVoiceDatabase.get(context)
    private val dao = database.conversationDao()
    private val chatDao = database.chatDao()

    val recentConversations: Flow<List<ConversationRecord>> =
        dao.observeRecent().map { entities -> entities.map { it.toRecord() } }

    val chatThreads: Flow<List<ChatThread>> =
        chatDao.observeThreads().map { entities -> entities.map { it.toModel() } }

    fun messagesForThread(threadId: String): Flow<List<ChatMessage>> =
        chatDao.observeMessages(threadId).map { entities -> entities.map { it.toModel() } }

    suspend fun ensureDefaultThread(
        connector: ConnectorType,
        mode: AgentMode
    ): ChatThread {
        val existing = chatDao.getMostRecentThread()
        if (existing != null) {
            return existing.toModel()
        }

        return createThread(
            title = DEFAULT_THREAD_TITLE,
            connector = connector,
            mode = mode
        )
    }

    suspend fun createThread(
        title: String,
        connector: ConnectorType,
        mode: AgentMode
    ): ChatThread {
        val now = System.currentTimeMillis()
        val entity = ChatThreadEntity(
            id = UUID.randomUUID().toString(),
            remoteId = null,
            title = title.trim().ifBlank { DEFAULT_THREAD_TITLE },
            connector = connector.value,
            mode = mode.wireValue,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        chatDao.upsertThread(entity)
        return entity.toModel()
    }

    suspend fun getThread(threadId: String): ChatThread? =
        chatDao.getThread(threadId)?.toModel()

    suspend fun renameThread(threadId: String, title: String) {
        val cleanedTitle = title.trim().ifBlank { DEFAULT_THREAD_TITLE }
        chatDao.renameThread(threadId, cleanedTitle, System.currentTimeMillis())
    }

    suspend fun updateThreadConnector(threadId: String, connector: ConnectorType) {
        chatDao.updateThreadConnector(threadId, connector.value, System.currentTimeMillis())
    }

    suspend fun updateThreadMode(threadId: String, mode: AgentMode) {
        chatDao.updateThreadMode(threadId, mode.wireValue, System.currentTimeMillis())
    }

    suspend fun deleteThreadLocally(threadId: String): Boolean {
        val thread = chatDao.getThread(threadId) ?: return false
        val now = System.currentTimeMillis()
        chatDao.upsertDeletedThread(
            DeletedThreadEntity(
                threadId = thread.id,
                remoteId = thread.remoteId,
                deletedAtMillis = now
            )
        )
        chatDao.deleteThread(threadId)
        return true
    }

    suspend fun saveUserMessage(threadId: String, text: String) {
        val now = System.currentTimeMillis()
        chatDao.insertMessage(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                threadId = threadId,
                role = ChatMessageRole.User.wireValue,
                text = text,
                status = ChatMessageStatus.Sent.wireValue,
                requestId = null,
                queuedActionsSummary = "",
                error = null,
                timestampMillis = now
            )
        )
        autoTitleThreadIfNeeded(threadId, text, now)
        chatDao.touchThread(threadId, now)
    }

    suspend fun saveAssistantResponse(threadId: String, response: AgentResponse) {
        val now = System.currentTimeMillis()
        chatDao.insertMessage(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                threadId = threadId,
                role = ChatMessageRole.Assistant.wireValue,
                text = response.reply,
                status = ChatMessageStatus.fromAgentStatus(response.status).wireValue,
                requestId = response.requestId,
                queuedActionsSummary = responseDetails(response),
                error = null,
                timestampMillis = now
            )
        )
        chatDao.updateThreadConnector(threadId, response.connector.value, now)
        chatDao.updateThreadMode(threadId, response.mode.wireValue, now)
    }

    suspend fun saveAssistantFailure(
        threadId: String,
        connector: ConnectorType,
        mode: AgentMode,
        error: String
    ) {
        val now = System.currentTimeMillis()
        chatDao.insertMessage(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                threadId = threadId,
                role = ChatMessageRole.Assistant.wireValue,
                text = error,
                status = ChatMessageStatus.Failed.wireValue,
                requestId = null,
                queuedActionsSummary = "",
                error = error,
                timestampMillis = now
            )
        )
        chatDao.updateThreadConnector(threadId, connector.value, now)
        chatDao.updateThreadMode(threadId, mode.wireValue, now)
    }

    suspend fun saveIncomingRelayMessages(
        threadId: String,
        messages: List<RelayInboxMessage>
    ): List<ChatMessage> {
        if (messages.isEmpty()) {
            return emptyList()
        }

        val inserted = mutableListOf<ChatMessage>()
        messages
            .filter { it.conversationId == threadId && it.direction == "outgoing" }
            .forEach { message ->
                if (chatDao.getMessage(message.id) != null) {
                    return@forEach
                }

                val entity = ChatMessageEntity(
                    id = message.id,
                    threadId = threadId,
                    role = ChatMessageRole.Assistant.wireValue,
                    text = message.content,
                    status = ChatMessageStatus.fromWireValue(message.status).wireValue,
                    requestId = null,
                    queuedActionsSummary = message.metadataSummary,
                    error = null,
                    timestampMillis = message.timestampMillis
                )
                chatDao.insertMessage(entity)
                inserted += entity.toModel()
            }

        if (inserted.isNotEmpty()) {
            chatDao.touchThread(threadId, inserted.maxOf { it.timestampMillis })
        }

        return inserted
    }

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
        chatDao.clearMessages()
        chatDao.clearThreads()
        chatDao.clearDeletedThreads()
    }

    private suspend fun autoTitleThreadIfNeeded(threadId: String, text: String, now: Long) {
        val thread = chatDao.getThread(threadId) ?: return
        if (thread.title != DEFAULT_THREAD_TITLE) {
            return
        }

        chatDao.renameThread(
            threadId = threadId,
            title = titleFromMessage(text),
            updatedAtMillis = now
        )
    }

    private fun titleFromMessage(text: String): String {
        val normalized = text
            .trim()
            .replace(Regex("\\s+"), " ")

        return normalized
            .take(MAX_TITLE_LENGTH)
            .ifBlank { DEFAULT_THREAD_TITLE }
    }

    private fun responseDetails(response: AgentResponse): String {
        val queuedActions = response.queuedActions.map { action ->
            "${action.type.label}: ${action.summary}"
        }
        val warnings = response.warnings.map { warning -> "Warning: $warning" }
        return (queuedActions + warnings).joinToString(separator = "\n")
    }

    companion object {
        const val DEFAULT_THREAD_TITLE = "New chat"
        private const val MAX_TITLE_LENGTH = 42
    }
}
