package com.agentvoice.app.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ConversationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AgentVoiceDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao

    companion object {
        @Volatile
        private var instance: AgentVoiceDatabase? = null

        fun get(context: Context): AgentVoiceDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AgentVoiceDatabase::class.java,
                    "agentvoice.db"
                ).build().also { instance = it }
            }
    }
}

