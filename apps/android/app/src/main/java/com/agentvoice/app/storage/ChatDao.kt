package com.agentvoice.app.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query(
        """
        SELECT * FROM chat_threads
        WHERE id NOT IN (SELECT threadId FROM deleted_chat_threads)
        AND (
            remoteId IS NULL OR remoteId NOT IN (
                SELECT remoteId FROM deleted_chat_threads WHERE remoteId IS NOT NULL
            )
        )
        ORDER BY updatedAtMillis DESC
        """
    )
    fun observeThreads(): Flow<List<ChatThreadEntity>>

    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY timestampMillis ASC")
    fun observeMessages(threadId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_threads WHERE id = :threadId LIMIT 1")
    suspend fun getThread(threadId: String): ChatThreadEntity?

    @Query("SELECT * FROM chat_messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessage(messageId: String): ChatMessageEntity?

    @Query("SELECT * FROM chat_threads ORDER BY updatedAtMillis DESC LIMIT 1")
    suspend fun getMostRecentThread(): ChatThreadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertThread(thread: ChatThreadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDeletedThread(entity: DeletedThreadEntity)

    @Query(
        """
        UPDATE chat_threads
        SET title = :title, updatedAtMillis = :updatedAtMillis
        WHERE id = :threadId
        """
    )
    suspend fun renameThread(threadId: String, title: String, updatedAtMillis: Long)

    @Query(
        """
        UPDATE chat_threads
        SET connector = :connector, updatedAtMillis = :updatedAtMillis
        WHERE id = :threadId
        """
    )
    suspend fun updateThreadConnector(
        threadId: String,
        connector: String,
        updatedAtMillis: Long
    )

    @Query(
        """
        UPDATE chat_threads
        SET mode = :mode, updatedAtMillis = :updatedAtMillis
        WHERE id = :threadId
        """
    )
    suspend fun updateThreadMode(threadId: String, mode: String, updatedAtMillis: Long)

    @Query("UPDATE chat_threads SET updatedAtMillis = :updatedAtMillis WHERE id = :threadId")
    suspend fun touchThread(threadId: String, updatedAtMillis: Long)

    @Query("DELETE FROM chat_threads WHERE id = :threadId")
    suspend fun deleteThread(threadId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages()

    @Query("DELETE FROM chat_threads")
    suspend fun clearThreads()

    @Query("DELETE FROM deleted_chat_threads")
    suspend fun clearDeletedThreads()
}
