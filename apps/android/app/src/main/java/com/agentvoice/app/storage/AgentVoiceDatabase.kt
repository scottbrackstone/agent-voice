package com.agentvoice.app.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ConversationEntity::class,
        ChatThreadEntity::class,
        ChatMessageEntity::class,
        DeletedThreadEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AgentVoiceDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var instance: AgentVoiceDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_threads` (
                        `id` TEXT NOT NULL,
                        `remoteId` TEXT,
                        `title` TEXT NOT NULL,
                        `connector` TEXT NOT NULL,
                        `mode` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_messages` (
                        `id` TEXT NOT NULL,
                        `threadId` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `requestId` TEXT,
                        `queuedActionsSummary` TEXT NOT NULL,
                        `error` TEXT,
                        `timestampMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`threadId`) REFERENCES `chat_threads`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chat_messages_threadId` " +
                        "ON `chat_messages` (`threadId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chat_messages_threadId_timestampMillis` " +
                        "ON `chat_messages` (`threadId`, `timestampMillis`)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `deleted_chat_threads` (
                        `threadId` TEXT NOT NULL,
                        `remoteId` TEXT,
                        `deletedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`threadId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_deleted_chat_threads_remoteId` " +
                        "ON `deleted_chat_threads` (`remoteId`)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM `chat_messages`")
                db.execSQL("DELETE FROM `chat_threads`")
                db.execSQL("DELETE FROM `deleted_chat_threads`")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `chat_threads` ADD COLUMN `backgroundId` " +
                        "TEXT NOT NULL DEFAULT 'sakura_signal'"
                )
                db.execSQL(
                    "ALTER TABLE `chat_threads` ADD COLUMN `avatarModelId` " +
                        "TEXT NOT NULL DEFAULT 'solara_static'"
                )
            }
        }

        fun get(context: Context): AgentVoiceDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AgentVoiceDatabase::class.java,
                    "agentvoice.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
    }
}
