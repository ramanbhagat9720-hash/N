package com.example.data.ai

import com.example.domain.model.ApprovedKnowledge
import com.example.domain.model.AssistantConfig
import com.example.domain.model.ConversationTurn
import com.example.domain.model.PermissionApprovalRequest
import com.example.domain.model.SupportedLanguage

data class AiCallerReplyResult(
    val replyText: String,
    val detectedLanguage: SupportedLanguage,
    val extractedCallerName: String? = null,
    val extractedReason: String? = null,
    val permissionRequest: PermissionApprovalRequest? = null,
    val isSampleMode: Boolean = true
)

data class AiCallSummaryResult(
    val detectedLanguage: SupportedLanguage,
    val reasonForCalling: String,
    val aiSummary: String,
    val requestedAction: String,
    val callerMessage: String,
    val postCallUserBriefing: String,
    val needsCallbackReminder: Boolean,
    val isSampleMode: Boolean = true
)

interface AiRepository {
    val isSampleMode: Boolean

    suspend fun generateUserGreetingPrompt(
        callerName: String,
        phoneNumber: String,
        config: AssistantConfig
    ): String

    suspend fun generateCallerOpening(
        callerName: String,
        config: AssistantConfig,
        language: SupportedLanguage
    ): String

    suspend fun generateCallerReply(
        callerUtterance: String,
        callerDisplayName: String,
        history: List<ConversationTurn>,
        config: AssistantConfig,
        approvedKnowledge: List<ApprovedKnowledge>,
        grantedPermission: PermissionApprovalRequest? = null
    ): AiCallerReplyResult

    suspend fun summarizeCall(
        callerName: String,
        phoneNumber: String,
        durationSeconds: Int,
        turns: List<ConversationTurn>,
        config: AssistantConfig
    ): AiCallSummaryResult
}
