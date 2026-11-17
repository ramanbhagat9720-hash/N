package com.example.data.local

import android.content.Context
import com.example.domain.model.ApprovedKnowledge
import com.example.domain.model.AssistantConfig
import com.example.domain.model.BlockedNumber
import com.example.domain.model.CallRecord
import com.example.domain.model.ContactHandlingRule
import com.example.domain.model.TrustedContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalAssistantRepository(private val database: AppDatabase) {

    constructor(context: Context) : this(AppDatabase.getInstance(context))

    private val configDao = database.assistantConfigDao()
    private val knowledgeDao = database.approvedKnowledgeDao()
    private val trustedDao = database.trustedContactDao()
    private val blockedDao = database.blockedNumberDao()
    private val callRecordDao = database.callRecordDao()

    fun observeConfig(): Flow<AssistantConfig> =
        configDao.observeConfig().map { it?.toDomain() ?: AssistantConfig() }

    suspend fun getConfig(): AssistantConfig =
        configDao.getConfig()?.toDomain() ?: AssistantConfig()

    suspend fun saveConfig(config: AssistantConfig) {
        configDao.upsertConfig(config.toEntity())
    }

    fun observeKnowledge(): Flow<List<ApprovedKnowledge>> =
        knowledgeDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getKnowledge(): List<ApprovedKnowledge> =
        knowledgeDao.getAll().map { it.toDomain() }

    suspend fun saveKnowledge(title: String, category: String, answer: String, id: Long = 0) {
        knowledgeDao.upsert(
            ApprovedKnowledgeEntity(
                id = id,
                title = title.trim(),
                category = category.trim().ifEmpty { "General" },
                approvedAnswer = answer.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteKnowledge(id: Long) {
        knowledgeDao.deleteById(id)
    }

    fun observeTrustedContacts(): Flow<List<TrustedContact>> =
        trustedDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getTrustedContacts(): List<TrustedContact> =
        trustedDao.getAll().map { it.toDomain() }

    suspend fun saveTrustedContact(
        name: String,
        phoneNumber: String,
        relationship: String,
        rule: ContactHandlingRule,
        id: Long = 0
    ) {
        trustedDao.upsert(
            TrustedContactEntity(
                id = id,
                name = name.trim(),
                phoneNumber = normalizePhone(phoneNumber),
                relationship = relationship.trim().ifEmpty { "Contact" },
                rule = rule.name
            )
        )
    }

    suspend fun deleteTrustedContact(id: Long) {
        trustedDao.deleteById(id)
    }

    fun observeBlockedNumbers(): Flow<List<BlockedNumber>> =
        blockedDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun isBlocked(phoneNumber: String): Boolean {
        val normalized = normalizePhone(phoneNumber)
        if (normalized.isEmpty()) return false
        return blockedDao.getAll().any { normalizePhone(it.phoneNumber) == normalized }
    }

    suspend fun findTrustedContact(phoneNumber: String): TrustedContact? {
        val normalized = normalizePhone(phoneNumber)
        if (normalized.isEmpty()) return null
        return trustedDao.getAll()
            .firstOrNull { normalizePhone(it.phoneNumber) == normalized }
            ?.toDomain()
    }

    suspend fun blockNumber(phoneNumber: String, reason: String = "Spam / Unwanted") {
        val normalized = normalizePhone(phoneNumber)
        if (normalized.isEmpty()) return
        blockedDao.upsert(
            BlockedNumberEntity(
                phoneNumber = normalized,
                reason = reason.trim().ifEmpty { "Blocked by user" }
            )
        )
    }

    suspend fun unblockNumber(id: Long) {
        blockedDao.deleteById(id)
    }

    fun observeCallRecords(): Flow<List<CallRecord>> =
        callRecordDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun saveCallRecord(record: CallRecord): Long =
        callRecordDao.upsert(record.toEntity())

    suspend fun setCallbackCompleted(id: Long, completed: Boolean) {
        callRecordDao.updateCallbackCompleted(id, completed)
    }

    suspend fun deleteCallRecord(id: Long) {
        callRecordDao.deleteById(id)
    }

    suspend fun clearAllUserData() {
        callRecordDao.clearAll()
        blockedDao.clearAll()
        trustedDao.clearAll()
        knowledgeDao.clearAll()
        configDao.clearConfig()
        AppDatabase.seedInitialConfiguration(database)
    }

    companion object {
        fun normalizePhone(raw: String): String {
            val digits = raw.filter { it.isDigit() || it == '+' }
            return if (digits.length > 10 && !digits.startsWith("+")) {
                digits.takeLast(10)
            } else {
                digits
            }
        }
    }
}
