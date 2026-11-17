package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AssistantConfigEntity::class,
        ApprovedKnowledgeEntity::class,
        TrustedContactEntity::class,
        BlockedNumberEntity::class,
        CallRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun assistantConfigDao(): AssistantConfigDao
    abstract fun approvedKnowledgeDao(): ApprovedKnowledgeDao
    abstract fun trustedContactDao(): TrustedContactDao
    abstract fun blockedNumberDao(): BlockedNumberDao
    abstract fun callRecordDao(): CallRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): AppDatabase {
            return Room.databaseBuilder(
                appContext,
                AppDatabase::class.java,
                "ai_call_assistant.db"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            seedInitialConfiguration(getInstance(appContext))
                        }
                    }
                })
                .build()
        }

        suspend fun seedInitialConfiguration(database: AppDatabase) {
            val configDao = database.assistantConfigDao()
            if (configDao.getConfig() == null) {
                configDao.upsertConfig(AssistantConfigEntity())
            }
            val knowledgeDao = database.approvedKnowledgeDao()
            if (knowledgeDao.getAll().isEmpty()) {
                knowledgeDao.upsert(
                    ApprovedKnowledgeEntity(
                        title = "Office Availability",
                        category = "Working Hours",
                        approvedAnswer = "Working hours are 9:00 AM to 7:00 PM Monday to Saturday. Please leave your name and reason for calling."
                    )
                )
                knowledgeDao.upsert(
                    ApprovedKnowledgeEntity(
                        title = "Callback Promise",
                        category = "FAQ",
                        approvedAnswer = "I will note your message and notify immediately so they can call you back as soon as available."
                    )
                )
            }
        }
    }
}
