package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.AiAnsweringMode
import com.example.domain.model.ApprovedKnowledge
import com.example.domain.model.AssistantConfig
import com.example.domain.model.BlockedNumber
import com.example.domain.model.CallHandlingMode
import com.example.domain.model.CallRecord
import com.example.domain.model.ContactHandlingRule
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.TrustedContact
import com.example.domain.model.VoiceProfile

@Entity(tableName = "assistant_config")
data class AssistantConfigEntity(
    @PrimaryKey val id: Int = 1,
    val userName: String = "Raman ji",
    val assistantEnabled: Boolean = true,
    val autoLanguageDetection: Boolean = true,
    val answeringMode: String = AiAnsweringMode.GREET_USER_FIRST.name,
    val voiceProfile: String = VoiceProfile.WARM_FRIENDLY.name,
    val preferredLanguageCodes: String = "hi,pa,en",
    val workingHoursStart: String = "09:00",
    val workingHoursEnd: String = "19:00",
    val customGreeting: String = "Hello! Main {userName} ka AI assistant hoon. Woh abhi available nahi hain. Main aapki kya madad kar sakta hoon?",
    val answeringDelaySeconds: Int = 5,
    val callRecordingEnabled: Boolean = false,
    val recordingConsentAccepted: Boolean = false,
    val hostedAiPhoneNumber: String = "",
    val callForwardingEnabled: Boolean = false
)

@Entity(tableName = "approved_knowledge")
data class ApprovedKnowledgeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val approvedAnswer: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trusted_contacts")
data class TrustedContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val rule: String
)

@Entity(tableName = "blocked_numbers")
data class BlockedNumberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "call_records")
data class CallRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callerName: String,
    val phoneNumber: String,
    val durationSeconds: Int,
    val detectedLanguageCode: String,
    val handlingMode: String,
    val reasonForCalling: String,
    val aiSummary: String,
    val requestedAction: String,
    val callerMessage: String,
    val transcript: String,
    val callbackReminderAt: Long?,
    val callbackCompleted: Boolean,
    val isSavedMessage: Boolean,
    val recordingConsentObtained: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

fun AssistantConfigEntity.toDomain(): AssistantConfig {
    val langs = preferredLanguageCodes
        .split(",")
        .mapNotNull { code -> code.trim().takeIf { it.isNotEmpty() }?.let { SupportedLanguage.fromCode(it) } }
        .ifEmpty { listOf(SupportedLanguage.HINDI, SupportedLanguage.ENGLISH) }
    return AssistantConfig(
        userName = userName,
        assistantEnabled = assistantEnabled,
        autoLanguageDetection = autoLanguageDetection,
        answeringMode = enumValues<AiAnsweringMode>().firstOrNull { it.name == answeringMode }
            ?: AiAnsweringMode.GREET_USER_FIRST,
        voiceProfile = enumValues<VoiceProfile>().firstOrNull { it.name == voiceProfile }
            ?: VoiceProfile.WARM_FRIENDLY,
        preferredLanguages = langs,
        workingHoursStart = workingHoursStart,
        workingHoursEnd = workingHoursEnd,
        customGreeting = customGreeting,
        answeringDelaySeconds = answeringDelaySeconds,
        callRecordingEnabled = callRecordingEnabled,
        recordingConsentAccepted = recordingConsentAccepted,
        hostedAiPhoneNumber = hostedAiPhoneNumber,
        callForwardingEnabled = callForwardingEnabled
    )
}

fun AssistantConfig.toEntity(): AssistantConfigEntity = AssistantConfigEntity(
    id = 1,
    userName = userName,
    assistantEnabled = assistantEnabled,
    autoLanguageDetection = autoLanguageDetection,
    answeringMode = answeringMode.name,
    voiceProfile = voiceProfile.name,
    preferredLanguageCodes = preferredLanguages.joinToString(",") { it.code },
    workingHoursStart = workingHoursStart,
    workingHoursEnd = workingHoursEnd,
    customGreeting = customGreeting,
    answeringDelaySeconds = answeringDelaySeconds,
    callRecordingEnabled = callRecordingEnabled,
    recordingConsentAccepted = recordingConsentAccepted,
    hostedAiPhoneNumber = hostedAiPhoneNumber,
    callForwardingEnabled = callForwardingEnabled
)

fun ApprovedKnowledgeEntity.toDomain() = ApprovedKnowledge(
    id = id,
    title = title,
    category = category,
    approvedAnswer = approvedAnswer,
    updatedAt = updatedAt
)

fun TrustedContactEntity.toDomain() = TrustedContact(
    id = id,
    name = name,
    phoneNumber = phoneNumber,
    relationship = relationship,
    rule = enumValues<ContactHandlingRule>().firstOrNull { it.name == rule }
        ?: ContactHandlingRule.GREET_FIRST
)

fun BlockedNumberEntity.toDomain() = BlockedNumber(
    id = id,
    phoneNumber = phoneNumber,
    reason = reason,
    createdAt = createdAt
)

fun CallRecordEntity.toDomain() = CallRecord(
    id = id,
    callerName = callerName,
    phoneNumber = phoneNumber,
    durationSeconds = durationSeconds,
    detectedLanguage = SupportedLanguage.fromCode(detectedLanguageCode),
    handlingMode = enumValues<CallHandlingMode>().firstOrNull { it.name == handlingMode }
        ?: CallHandlingMode.AI_HANDLED,
    reasonForCalling = reasonForCalling,
    aiSummary = aiSummary,
    requestedAction = requestedAction,
    callerMessage = callerMessage,
    transcript = transcript,
    callbackReminderAt = callbackReminderAt,
    callbackCompleted = callbackCompleted,
    isSavedMessage = isSavedMessage,
    recordingConsentObtained = recordingConsentObtained,
    createdAt = createdAt
)

fun CallRecord.toEntity() = CallRecordEntity(
    id = id,
    callerName = callerName,
    phoneNumber = phoneNumber,
    durationSeconds = durationSeconds,
    detectedLanguageCode = detectedLanguage.code,
    handlingMode = handlingMode.name,
    reasonForCalling = reasonForCalling,
    aiSummary = aiSummary,
    requestedAction = requestedAction,
    callerMessage = callerMessage,
    transcript = transcript,
    callbackReminderAt = callbackReminderAt,
    callbackCompleted = callbackCompleted,
    isSavedMessage = isSavedMessage,
    recordingConsentObtained = recordingConsentObtained,
    createdAt = createdAt
)
