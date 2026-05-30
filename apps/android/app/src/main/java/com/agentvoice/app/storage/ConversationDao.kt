package com.agentvoice.app.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversation_interactions ORDER BY timestampMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ConversationEntity)

    @Query("DELETE FROM conversation_interactions")
    suspend fun clearAll()
}

