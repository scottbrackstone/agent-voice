package com.agentvoice.app.storage

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.agentvoice.app.model.AgentMode
import com.agentvoice.app.model.AvatarModels
import com.agentvoice.app.model.ChatBackground
import com.agentvoice.app.model.ChatMessage
import com.agentvoice.app.model.ChatMessageRole
import com.agentvoice.app.model.ChatMessageStatus
import com.agentvoice.app.model.ChatThread
import com.agentvoice.app.model.ConnectorType

@Entity(tableName = "chat_threads")
data class ChatThreadEntity(
    @PrimaryKey val id: String,
    val remoteId: String?,
    val title: String,
    val connector: String,
    val mode: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val backgroundId: String = ChatBackground.DEFAULT_ID,
    val avatarModelId: String = AvatarModels.SOLARA_STATIC
) {
    fun toModel(): ChatThread =
        ChatThread(
            id = id,
            remoteId = remoteId,
            title = title,
            connector = ConnectorType.fromValue(connector, fallback = ConnectorType.Hermes),
            mode = AgentMode.fromWireValue(mode),
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
            backgroundId = backgroundId,
            avatarModelId = avatarModelId
        )
}

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatThreadEntity::class,
            parentColumns = ["id"],
            childColumns = ["threadId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("threadId"),
        Index(value = ["threadId", "timestampMillis"])
    ]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val role: String,
    val text: String,
    val status: String,
    val requestId: String?,
    val queuedActionsSummary: String,
    val error: String?,
    val timestampMillis: Long
) {
    fun toModel(): ChatMessage =
        ChatMessage(
            id = id,
            threadId = threadId,
            role = ChatMessageRole.fromWireValue(role),
            text = text,
            status = ChatMessageStatus.fromWireValue(status),
            requestId = requestId,
            queuedActionsSummary = queuedActionsSummary,
            error = error,
            timestampMillis = timestampMillis
        )
}

@Entity(
    tableName = "deleted_chat_threads",
    indices = [
        Index("remoteId")
    ]
)
data class DeletedThreadEntity(
    @PrimaryKey val threadId: String,
    val remoteId: String?,
    val deletedAtMillis: Long
)
