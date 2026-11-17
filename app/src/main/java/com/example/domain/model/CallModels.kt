package com.example.domain.model

enum class AiAnsweringMode(val label: String, val description: String) {
    GREET_USER_FIRST("Greet User First", "AI speaks to you before answering or rejecting any call"),
    AFTER_HOURS_AI("After Working Hours", "AI screens outside working hours; greets you first during work"),
    UNKNOWN_CALLERS_AI("Screen Unknown Callers", "Trusted contacts ring directly; unknown callers are screened first"),
    SILENT_ASSISTANT("Silent Screen Only", "Logs caller intent silently without ringing aloud")
}

enum class VoiceProfile(
    val label: String,
    val pitch: Float,
    val speechRate: Float,
    val description: String
) {
    CALM_NATURAL("Aria — Calm & Natural", 1.0f, 0.98f, "Balanced conversational cadence"),
    WARM_FRIENDLY("Aarav — Warm & Helpful", 0.94f, 1.02f, "Friendly personal assistant tone"),
    CRISP_EXECUTIVE("Meera — Crisp Executive", 1.06f, 1.06f, "Concise, clear business articulation"),
    SOFT_RESPECTFUL("Kabir — Soft & Polite", 0.88f, 0.94f, "Gentle respectful pacing")
}

enum class ContactHandlingRule(val label: String) {
    ALWAYS_RING("Ring Me Immediately"),
    GREET_FIRST("AI Greets Me First"),
    AI_AUTO_SCREEN("Let AI Answer After Delay"),
    PRIORITY_VIP("VIP Priority Ring")
}

enum class CallHandlingMode(val label: String) {
    AI_HANDLED("AI Handled"),
    ANSWERED_MYSELF("Answered Myself"),
    REJECTED("Rejected"),
    SILENT("Silent Mode"),
    MISSED("Missed"),
    BLOCKED("Blocked")
}

enum class CallPhase {
    IDLE,
    GREETING_USER_FIRST,
    AI_ANSWERING_CALLER,
    PERSONAL_CALL_ACTIVE,
    CALL_SUMMARY_READY
}

enum class PermissionGateType(val title: String) {
    PERSONAL_INFO("Share Personal Information"),
    SCHEDULE_MEETING("Schedule Meeting / Commitment"),
    BUSINESS_ORDER("Confirm Business Order")
}

data class AssistantConfig(
    val userName: String = "Raman ji",
    val assistantEnabled: Boolean = true,
    val autoLanguageDetection: Boolean = true,
    val answeringMode: AiAnsweringMode = AiAnsweringMode.GREET_USER_FIRST,
    val voiceProfile: VoiceProfile = VoiceProfile.WARM_FRIENDLY,
    val preferredLanguages: List<SupportedLanguage> = listOf(
        SupportedLanguage.HINDI,
        SupportedLanguage.PUNJABI,
        SupportedLanguage.ENGLISH
    ),
    val workingHoursStart: String = "09:00",
    val workingHoursEnd: String = "19:00",
    val customGreeting: String = "Hello! Main {userName} ka AI assistant hoon. Woh abhi available nahi hain. Main aapki kya madad kar sakta hoon?",
    val answeringDelaySeconds: Int = 5,
    val callRecordingEnabled: Boolean = false,
    val recordingConsentAccepted: Boolean = false,
    val hostedAiPhoneNumber: String = "",
    val callForwardingEnabled: Boolean = false
)

data class ApprovedKnowledge(
    val id: Long = 0,
    val title: String,
    val category: String,
    val approvedAnswer: String,
    val updatedAt: Long = System.currentTimeMillis()
)

data class TrustedContact(
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val relationship: String = "Contact",
    val rule: ContactHandlingRule = ContactHandlingRule.GREET_FIRST
)

data class BlockedNumber(
    val id: Long = 0,
    val phoneNumber: String,
    val reason: String = "Spam / Unwanted",
    val createdAt: Long = System.currentTimeMillis()
)

data class CallRecord(
    val id: Long = 0,
    val callerName: String,
    val phoneNumber: String,
    val durationSeconds: Int,
    val detectedLanguage: SupportedLanguage,
    val handlingMode: CallHandlingMode,
    val reasonForCalling: String,
    val aiSummary: String,
    val requestedAction: String,
    val callerMessage: String,
    val transcript: String,
    val callbackReminderAt: Long? = null,
    val callbackCompleted: Boolean = false,
    val isSavedMessage: Boolean = false,
    val recordingConsentObtained: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class ConversationTurn(
    val id: String,
    val speaker: SpeakerRole,
    val text: String,
    val language: SupportedLanguage,
    val timestampMs: Long = System.currentTimeMillis(),
    val isSampleMode: Boolean = true
)

enum class SpeakerRole {
    AI_TO_USER,
    AI_TO_CALLER,
    CALLER,
    USER,
    SYSTEM
}

data class PermissionApprovalRequest(
    val id: String,
    val gateType: PermissionGateType,
    val promptToUser: String,
    val proposedResponse: String
)

data class LiveCallSession(
    val sessionId: String = "",
    val callerName: String = "",
    val phoneNumber: String = "",
    val isUnknownCaller: Boolean = true,
    val phase: CallPhase = CallPhase.IDLE,
    val detectedLanguage: SupportedLanguage = SupportedLanguage.HINDI,
    val isAiMuted: Boolean = false,
    val isRecordingWithConsent: Boolean = false,
    val userGreetingMessage: String = "",
    val callerExtractedName: String = "",
    val callerExtractedReason: String = "",
    val turns: List<ConversationTurn> = emptyList(),
    val pendingPermission: PermissionApprovalRequest? = null,
    val startedAtMs: Long = 0L,
    val elapsedSeconds: Int = 0,
    val isRealTelecomCall: Boolean = false,
    val postCallBriefingForUser: String = "",
    val completedRecordId: Long? = null
)
