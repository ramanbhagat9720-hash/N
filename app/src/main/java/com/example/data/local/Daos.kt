package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantConfigDao {
    @Query("SELECT * FROM assistant_config WHERE id = 1 LIMIT 1")
    fun observeConfig(): Flow<AssistantConfigEntity?>

    @Query("SELECT * FROM assistant_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): AssistantConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfig(config: AssistantConfigEntity)

    @Query("DELETE FROM assistant_config")
    suspend fun clearConfig()
}

@Dao
interface ApprovedKnowledgeDao {
    @Query("SELECT * FROM approved_knowledge ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ApprovedKnowledgeEntity>>

    @Query("SELECT * FROM approved_knowledge ORDER BY updatedAt DESC")
    suspend fun getAll(): List<ApprovedKnowledgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ApprovedKnowledgeEntity): Long

    @Query("DELETE FROM approved_knowledge WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM approved_knowledge")
    suspend fun clearAll()
}

@Dao
interface TrustedContactDao {
    @Query("SELECT * FROM trusted_contacts ORDER BY name ASC")
    fun observeAll(): Flow<List<TrustedContactEntity>>

    @Query("SELECT * FROM trusted_contacts")
    suspend fun getAll(): List<TrustedContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TrustedContactEntity): Long

    @Query("DELETE FROM trusted_contacts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM trusted_contacts")
    suspend fun clearAll()
}

@Dao
interface BlockedNumberDao {
    @Query("SELECT * FROM blocked_numbers ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BlockedNumberEntity>>

    @Query("SELECT * FROM blocked_numbers")
    suspend fun getAll(): List<BlockedNumberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BlockedNumberEntity): Long

    @Query("DELETE FROM blocked_numbers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM blocked_numbers")
    suspend fun clearAll()
}

@Dao
interface CallRecordDao {
    @Query("SELECT * FROM call_records ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CallRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CallRecordEntity): Long

    @Query("UPDATE call_records SET callbackCompleted = :completed WHERE id = :id")
    suspend fun updateCallbackCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM call_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM call_records")
    suspend fun clearAll()
}
