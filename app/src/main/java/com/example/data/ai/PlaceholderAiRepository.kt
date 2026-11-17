package com.example.data.ai

import com.example.domain.model.ApprovedKnowledge
import com.example.domain.model.AssistantConfig
import com.example.domain.model.ConversationTurn
import com.example.domain.model.PermissionApprovalRequest
import com.example.domain.model.PermissionGateType
import com.example.domain.model.SpeakerRole
import com.example.domain.model.SupportedLanguage
import java.util.UUID

/**
 * Sample-mode implementation of [AiRepository] used when Firebase AI Logic is not enabled.
 * Strictly avoids inventing unapproved personal answers, enforces user permission gates,
 * and demonstrates multilingual greeting, NLU, and call summary flows.
 */
class PlaceholderAiRepository : AiRepository {

    override val isSampleMode: Boolean = true

    override suspend fun generateUserGreetingPrompt(
        callerName: String,
        phoneNumber: String,
        config: AssistantConfig
    ): String {
        val who = callerName.takeIf { it.isNotBlank() && it != "Unknown Caller" } ?: phoneNumber
        val primaryLang = config.preferredLanguages.firstOrNull() ?: SupportedLanguage.HINDI
        return when (primaryLang) {
            SupportedLanguage.HINDI ->
                "Hello ${config.userName}! Aapko $who ka phone aa raha hai. Kya aap khud baat karenge ya main aapki taraf se baat karun?"
            SupportedLanguage.PUNJABI ->
                "Sat Sri Akal ${config.userName}! Tuhanu $who da phone aa reha hai. Tusi aap gal karoge ya main tuhadi taraf ton gal karaan?"
            SupportedLanguage.URDU ->
                "Assalam-o-Alaikum ${config.userName}! Aapko $who ki call aa rahi hai. Kya aap khud baat karenge ya main aapki jagah baat karun?"
            else ->
                "Hello ${config.userName}! You have an incoming call from $who. Would you like to answer yourself or let me handle it?"
        }
    }

    override suspend fun generateCallerOpening(
        callerName: String,
        config: AssistantConfig,
        language: SupportedLanguage
    ): String {
        val custom = config.customGreeting
            .replace("{userName}", config.userName)
            .trim()
        if (language == SupportedLanguage.HINDI && custom.isNotEmpty()) {
            return custom
        }
        return String.format(language.aiDisclosureSample, config.userName)
    }

    override suspend fun generateCallerReply(
        callerUtterance: String,
        callerDisplayName: String,
        history: List<ConversationTurn>,
        config: AssistantConfig,
        approvedKnowledge: List<ApprovedKnowledge>,
        grantedPermission: PermissionApprovalRequest?
    ): AiCallerReplyResult {
        val detectedLang = if (config.autoLanguageDetection) {
            LanguageDetector.detectLanguage(
                utterance = callerUtterance,
                preferredLanguages = config.preferredLanguages,
                fallback = config.preferredLanguages.firstOrNull() ?: SupportedLanguage.HINDI
            )
        } else {
            config.preferredLanguages.firstOrNull() ?: SupportedLanguage.ENGLISH
        }

        val lower = callerUtterance.lowercase()
        val extractedName = extractName(callerUtterance)
        val extractedReason = extractReason(callerUtterance)

        // If user just granted permission for a gated action, execute approved response
        if (grantedPermission != null) {
            return AiCallerReplyResult(
                replyText = grantedPermission.proposedResponse,
                detectedLanguage = detectedLang,
                extractedCallerName = extractedName,
                extractedReason = extractedReason,
                permissionRequest = null,
                isSampleMode = true
            )
        }

        // Check permission gates BEFORE sharing personal info, commitments, meetings, or business orders
        val personalInfoTriggers = listOf("address", "email", "otp", "pan", "aadhaar", "location", "home", "number", "ghar", "pata")
        if (personalInfoTriggers.any { lower.contains(it) }) {
            val proposed = localizeMessage(
                detectedLang,
                hi = "${config.userName} ne is jankari ko share karne ki anumati de di hai. Main aapka message unhe bhej raha hoon.",
                pa = "${config.userName} ne eh jankari share karan di manzoori de ditti hai.",
                en = "${config.userName} approved sharing that detail. Please go ahead with your request."
            )
            return AiCallerReplyResult(
                replyText = localizeMessage(
                    detectedLang,
                    hi = "Main ${config.userName} ki personal details bina unki permission ke share nahi kar sakta. Main abhi unse pooch raha hoon, kripya ek pal intezaar karein.",
                    pa = "Main ${config.userName} di personal jankari bina ijazat share nahi kar sakda. Ek pal ruko, main puchh ke dassda haan.",
                    en = "I cannot share ${config.userName}'s personal information without their explicit approval. One moment while I check with them."
                ),
                detectedLanguage = detectedLang,
                extractedCallerName = extractedName,
                extractedReason = extractedReason ?: "Requested personal information",
                permissionRequest = PermissionApprovalRequest(
                    id = UUID.randomUUID().toString(),
                    gateType = PermissionGateType.PERSONAL_INFO,
                    promptToUser = "$callerDisplayName is asking for personal details (\"$callerUtterance\"). Allow AI to share approved contact/location info?",
                    proposedResponse = proposed
                ),
                isSampleMode = true
            )
        }

        val meetingTriggers = listOf("meeting", "schedule", "appointment", "calendar", "tomorrow", "kal", "baje", "slot", "book")
        if (meetingTriggers.any { lower.contains(it) }) {
            val proposed = localizeMessage(
                detectedLang,
                hi = "${config.userName} ne meeting request note kar li hai aur woh timing confirm karne ke liye aapko wapas call karenge.",
                pa = "${config.userName} ne meeting di request manzoor kar layi hai, oh jaldi confirm karan ge.",
                en = "${config.userName} has approved noting this meeting request and will confirm the slot shortly."
            )
            return AiCallerReplyResult(
                replyText = localizeMessage(
                    detectedLang,
                    hi = "Meeting ya time confirm karne ke liye mujhe ${config.userName} ki permission chahiye. Main abhi unse confirm kar raha hoon.",
                    pa = "Meeting confirm karan layi mainu ${config.userName} di ijazat chahidi hai. Main hunne puchhda haan.",
                    en = "I need ${config.userName}'s permission before committing to a meeting or schedule. Let me check with them right now."
                ),
                detectedLanguage = detectedLang,
                extractedCallerName = extractedName,
                extractedReason = extractedReason ?: "Schedule a meeting",
                permissionRequest = PermissionApprovalRequest(
                    id = UUID.randomUUID().toString(),
                    gateType = PermissionGateType.SCHEDULE_MEETING,
                    promptToUser = "$callerDisplayName wants to schedule a meeting/commitment (\"$callerUtterance\"). Approve AI to acknowledge & book?",
                    proposedResponse = proposed
                ),
                isSampleMode = true
            )
        }

        val orderTriggers = listOf("order", "confirm", "payment", "invoice", "quote", "booking", "deliver", "price")
        if (orderTriggers.any { lower.contains(it) }) {
            val proposed = localizeMessage(
                detectedLang,
                hi = "${config.userName} ne business order inquiry approve kar di hai. Main aapki details note kar raha hoon.",
                pa = "${config.userName} ne order inquiry approve kar ditti hai. Main details note kar reha haan.",
                en = "${config.userName} approved confirming this business order inquiry. I have logged your details."
            )
            return AiCallerReplyResult(
                replyText = localizeMessage(
                    detectedLang,
                    hi = "Kisi bhi business order ya commitment ko confirm karne se pehle mujhe ${config.userName} ki permission leni hogi. Ek second rukiye.",
                    pa = "Business order confirm karan ton pehlan mainu ${config.userName} di manzoori laini pavegi.",
                    en = "Before confirming any business order or commitment, I must ask ${config.userName}'s permission. Please hold a moment."
                ),
                detectedLanguage = detectedLang,
                extractedCallerName = extractedName,
                extractedReason = extractedReason ?: "Business order confirmation",
                permissionRequest = PermissionApprovalRequest(
                    id = UUID.randomUUID().toString(),
                    gateType = PermissionGateType.BUSINESS_ORDER,
                    promptToUser = "$callerDisplayName is requesting a business order confirmation (\"$callerUtterance\"). Approve AI to proceed?",
                    proposedResponse = proposed
                ),
                isSampleMode = true
            )
        }

        // Search Approved Knowledge Base strictly (never invent unapproved answers)
        val matchedKnowledge = approvedKnowledge.firstOrNull { entry ->
            val keywords = (entry.title + " " + entry.category).lowercase().split(Regex("\\s+"))
                .filter { it.length > 3 }
            keywords.any { lower.contains(it) }
        }

        if (matchedKnowledge != null) {
            val prefix = localizeMessage(
                detectedLang,
                hi = "Approved jankari ke anusaar: ",
                pa = "Approved jankari mutabik: ",
                en = "Based on ${config.userName}'s approved information: "
            )
            return AiCallerReplyResult(
                replyText = "$prefix${matchedKnowledge.approvedAnswer}",
                detectedLanguage = detectedLang,
                extractedCallerName = extractedName,
                extractedReason = extractedReason,
                isSampleMode = true
            )
        }

        // Standard conversational turn: ask name & reason if not yet known, or confirm message taken
        val alreadyAskedName = history.any {
            it.speaker == SpeakerRole.AI_TO_CALLER &&
                (it.text.contains("naam", ignoreCase = true) || it.text.contains("name", ignoreCase = true))
        }

        val reply = if (!alreadyAskedName && extractedName == null && callerDisplayName == "Unknown Caller") {
            localizeMessage(
                detectedLang,
                hi = "Ji, aap apna naam aur call karne ka reason bata sakte hain? Main ${config.userName} ko aapka message de dunga.",
                pa = "Haanji, ki tusi apna naam te phone karan da kaaran dass sakde ho? Main ${config.userName} nu message de devanga.",
                en = "Certainly! May I know your name and the reason for your call so I can pass it along to ${config.userName}?"
            )
        } else {
            val ackReason = extractedReason?.let { " ($it)" } ?: ""
            localizeMessage(
                detectedLang,
                hi = "Dhanyavaad! Maine aapka message$ackReason note kar liya hai. Main ${config.userName} ko abhi inform kar dunga aur woh aapko jald callback karenge. Kya kuch aur add karna hai?",
                pa = "Dhanvaad! Main tuhada suneha$ackReason note kar laya hai. Main ${config.userName} nu dass devanga te oh wapas call karan ge.",
                en = "Thank you! I have noted your message$ackReason for ${config.userName}. Is there anything else you would like me to pass along?"
            )
        }

        return AiCallerReplyResult(
            replyText = reply,
            detectedLanguage = detectedLang,
            extractedCallerName = extractedName,
            extractedReason = extractedReason,
            isSampleMode = true
        )
    }

    override suspend fun summarizeCall(
        callerName: String,
        phoneNumber: String,
        durationSeconds: Int,
        turns: List<ConversationTurn>,
        config: AssistantConfig
    ): AiCallSummaryResult {
        val callerTurns = turns.filter { it.speaker == SpeakerRole.CALLER }
        val dominantLanguage = callerTurns.lastOrNull()?.language
            ?: turns.lastOrNull()?.language
            ?: config.preferredLanguages.firstOrNull()
            ?: SupportedLanguage.HINDI

        val combinedCallerSpeech = callerTurns.joinToString(" ") { it.text }.trim()
        val reason = when {
            combinedCallerSpeech.isBlank() -> "General inquiry / no speech captured"
            combinedCallerSpeech.contains("office", ignoreCase = true) ||
                combinedCallerSpeech.contains("kaam", ignoreCase = true) -> "Office work / urgent discussion"
            combinedCallerSpeech.contains("meeting", ignoreCase = true) -> "Meeting scheduling request"
            combinedCallerSpeech.contains("order", ignoreCase = true) -> "Business order inquiry"
            else -> combinedCallerSpeech.take(100)
        }

        val who = callerName.ifBlank { phoneNumber }
        val primaryUserLang = config.preferredLanguages.firstOrNull() ?: SupportedLanguage.HINDI
        val postCallBriefing = when (primaryUserLang) {
            SupportedLanguage.HINDI ->
                "${config.userName}, $who ne aapko \"$reason\" ke liye call kiya tha. Kya aap unhe wapas call karna chahenge?"
            SupportedLanguage.PUNJABI ->
                "${config.userName}, $who ne tuhanu \"$reason\" layi phone kita si. Ki tusi wapas call karna chahoge?"
            else ->
                "${config.userName}, $who called regarding \"$reason\". Would you like to schedule a callback?"
        }

        val summary = buildString {
            append("[Sample AI Summary] ")
            append("$who spoke in ${dominantLanguage.displayName}. ")
            if (combinedCallerSpeech.isNotBlank()) {
                append("Caller stated: \"$combinedCallerSpeech\". ")
            }
            append("AI disclosed assistant identity and offered only user-approved responses.")
        }

        val action = if (callerTurns.isNotEmpty()) {
            "Call back $who regarding: $reason"
        } else {
            "Review missed/screened call from $who"
        }

        return AiCallSummaryResult(
            detectedLanguage = dominantLanguage,
            reasonForCalling = reason,
            aiSummary = summary,
            requestedAction = action,
            callerMessage = combinedCallerSpeech.ifBlank { "No spoken message left." },
            postCallUserBriefing = postCallBriefing,
            needsCallbackReminder = callerTurns.isNotEmpty(),
            isSampleMode = true
        )
    }

    private fun extractName(utterance: String): String? {
        val patterns = listOf(
            Regex("""(?:my name is|this is|i am|main|mera naam)\s+([A-Za-z]{2,20})""", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(utterance)
            if (match != null) {
                return match.groupValues[1].replaceFirstChar { it.uppercase() }
            }
        }
        return null
    }

    private fun extractReason(utterance: String): String? {
        val trimmed = utterance.trim()
        return if (trimmed.length >= 6) trimmed.take(120) else null
    }

    private fun localizeMessage(
        language: SupportedLanguage,
        hi: String,
        pa: String,
        en: String
    ): String = when (language) {
        SupportedLanguage.HINDI, SupportedLanguage.MARATHI, SupportedLanguage.URDU -> hi
        SupportedLanguage.PUNJABI -> pa
        else -> en
    }
}
