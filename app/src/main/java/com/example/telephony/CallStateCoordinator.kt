package com.example.telephony

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.telecom.Call
import android.telecom.VideoProfile
import com.example.data.ai.AiRepository
import com.example.data.ai.LanguageDetector
import com.example.data.ai.PlaceholderAiRepository
import com.example.data.local.AppDatabase
import com.example.data.local.LocalAssistantRepository
import com.example.domain.model.AssistantConfig
import com.example.domain.model.CallHandlingMode
import com.example.domain.model.CallPhase
import com.example.domain.model.CallRecord
import com.example.domain.model.ConversationTurn
import com.example.domain.model.LiveCallSession
import com.example.domain.model.PermissionApprovalRequest
import com.example.domain.model.SpeakerRole
import com.example.domain.model.SupportedLanguage
import com.example.voice.VoiceAiEngine
import com.example.voice.VoiceEngineState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CallStateCoordinator private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val repository: LocalAssistantRepository by lazy {
        LocalAssistantRepository(AppDatabase.getInstance(appContext))
    }

    val aiRepository: AiRepository = PlaceholderAiRepository()
    val voiceEngine: VoiceAiEngine = VoiceAiEngine(appContext)

    private val _liveSession = MutableStateFlow(LiveCallSession())
    val liveSession: StateFlow<LiveCallSession> = _liveSession.asStateFlow()

    val voiceState: StateFlow<VoiceEngineState> = voiceEngine.state

    private var activeTelecomCall: Call? = null
    private var durationJob: Job? = null

    fun attachTelecomCall(call: Call?) {
        activeTelecomCall = call
    }

    fun onIncomingCallDetected(
        phoneNumber: String,
        contactDisplayName: String? = null,
        isRealTelecomCall: Boolean = false
    ) {
        scope.launch {
            val config = repository.getConfig()
            if (!config.assistantEnabled) return@launch

            val resolvedName = contactDisplayName?.takeIf { it.isNotBlank() }
                ?: ContactsHelper.lookupCallerName(appContext, phoneNumber)
                ?: repository.findTrustedContact(phoneNumber)?.name
                ?: "Unknown Caller"

            val greetingPrompt = aiRepository.generateUserGreetingPrompt(
                callerName = resolvedName,
                phoneNumber = phoneNumber,
                config = config
            )

            val primaryLang = config.preferredLanguages.firstOrNull() ?: SupportedLanguage.HINDI

            _liveSession.value = LiveCallSession(
                sessionId = UUID.randomUUID().toString(),
                callerName = resolvedName,
                phoneNumber = phoneNumber,
                isUnknownCaller = resolvedName == "Unknown Caller",
                phase = CallPhase.GREETING_USER_FIRST,
                detectedLanguage = primaryLang,
                userGreetingMessage = greetingPrompt,
                turns = listOf(
                    ConversationTurn(
                        id = UUID.randomUUID().toString(),
                        speaker = SpeakerRole.AI_TO_USER,
                        text = greetingPrompt,
                        language = primaryLang,
                        isSampleMode = aiRepository.isSampleMode
                    )
                ),
                startedAtMs = System.currentTimeMillis(),
                isRealTelecomCall = isRealTelecomCall
            )

            // Speak greeting to the user first before answering or rejecting
            voiceEngine.speak(
                text = greetingPrompt,
                language = primaryLang,
                profile = config.voiceProfile
            )

            if (isRealTelecomCall) {
                startForegroundService(resolvedName, phoneNumber)
            }
        }
    }

    fun onAnswerMyself() {
        scope.launch {
            voiceEngine.stopSpeaking()
            voiceEngine.stopListening()

            val current = _liveSession.value
            if (current.isRealTelecomCall) {
                activeTelecomCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
                    ?: TelephonyRoleManager.answerRingingCallViaTelecom(appContext)
            }

            startDurationTimer()
            _liveSession.value = current.copy(
                phase = CallPhase.PERSONAL_CALL_ACTIVE,
                turns = current.turns + ConversationTurn(
                    id = UUID.randomUUID().toString(),
                    speaker = SpeakerRole.SYSTEM,
                    text = "Call answered personally by you. AI is standing down.",
                    language = current.detectedLanguage,
                    isSampleMode = false
                )
            )
        }
    }

    fun onLetAiTalk() {
        scope.launch {
            voiceEngine.stopSpeaking()
            val config = repository.getConfig()
            val current = _liveSession.value
            if (current.phase == CallPhase.IDLE) return@launch

            if (current.isRealTelecomCall) {
                activeTelecomCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
                    ?: TelephonyRoleManager.answerRingingCallViaTelecom(appContext)
                routeAudioToSpeakerForAiBridge()
            }

            startForegroundService(current.callerName, current.phoneNumber)
            startDurationTimer()

            val openingLang = current.detectedLanguage
            val openingText = aiRepository.generateCallerOpening(
                callerName = current.callerName,
                config = config,
                language = openingLang
            )

            val updatedTurns = current.turns + ConversationTurn(
                id = UUID.randomUUID().toString(),
                speaker = SpeakerRole.AI_TO_CALLER,
                text = openingText,
                language = openingLang,
                isSampleMode = aiRepository.isSampleMode
            )

            _liveSession.value = current.copy(
                phase = CallPhase.AI_ANSWERING_CALLER,
                isRecordingWithConsent = config.callRecordingEnabled && config.recordingConsentAccepted,
                turns = updatedTurns
            )

            voiceEngine.speak(
                text = openingText,
                language = openingLang,
                profile = config.voiceProfile,
                muted = current.isAiMuted
            ) {
                // After AI speaks, automatically start listening if speech recognition is available
                if (_liveSession.value.phase == CallPhase.AI_ANSWERING_CALLER) {
                    beginListeningTurn()
                }
            }
        }
    }

    fun beginListeningTurn() {
        val current = _liveSession.value
        if (current.phase != CallPhase.AI_ANSWERING_CALLER) return
        voiceEngine.startListening(current.detectedLanguage) { spokenText ->
            onCallerSpoke(spokenText)
        }
    }

    fun onCallerSpoke(utterance: String) {
        val cleanText = utterance.trim()
        if (cleanText.isEmpty()) return

        scope.launch {
            val current = _liveSession.value
            if (current.phase != CallPhase.AI_ANSWERING_CALLER) return@launch

            val config = repository.getConfig()
            val knowledge = repository.getKnowledge()

            val detectedLang = if (config.autoLanguageDetection) {
                LanguageDetector.detectLanguage(
                    utterance = cleanText,
                    preferredLanguages = config.preferredLanguages,
                    fallback = current.detectedLanguage
                )
            } else {
                current.detectedLanguage
            }

            val callerTurn = ConversationTurn(
                id = UUID.randomUUID().toString(),
                speaker = SpeakerRole.CALLER,
                text = cleanText,
                language = detectedLang,
                isSampleMode = false
            )

            val turnsWithCaller = current.turns + callerTurn

            val reply = aiRepository.generateCallerReply(
                callerUtterance = cleanText,
                callerDisplayName = current.callerName,
                history = turnsWithCaller,
                config = config,
                approvedKnowledge = knowledge
            )

            val aiTurn = ConversationTurn(
                id = UUID.randomUUID().toString(),
                speaker = SpeakerRole.AI_TO_CALLER,
                text = reply.replyText,
                language = reply.detectedLanguage,
                isSampleMode = reply.isSampleMode
            )

            _liveSession.value = _liveSession.value.copy(
                detectedLanguage = reply.detectedLanguage,
                callerExtractedName = reply.extractedCallerName ?: current.callerExtractedName,
                callerExtractedReason = reply.extractedReason ?: current.callerExtractedReason,
                turns = turnsWithCaller + aiTurn,
                pendingPermission = reply.permissionRequest
            )

            voiceEngine.speak(
                text = reply.replyText,
                language = reply.detectedLanguage,
                profile = config.voiceProfile,
                muted = _liveSession.value.isAiMuted
            ) {
                if (_liveSession.value.phase == CallPhase.AI_ANSWERING_CALLER &&
                    _liveSession.value.pendingPermission == null
                ) {
                    beginListeningTurn()
                }
            }
        }
    }

    fun onApprovePermission() {
        scope.launch {
            val current = _liveSession.value
            val request = current.pendingPermission ?: return@launch
            val config = repository.getConfig()
            val knowledge = repository.getKnowledge()

            val reply = aiRepository.generateCallerReply(
                callerUtterance = request.proposedResponse,
                callerDisplayName = current.callerName,
                history = current.turns,
                config = config,
                approvedKnowledge = knowledge,
                grantedPermission = request
            )

            val approvalTurn = ConversationTurn(
                id = UUID.randomUUID().toString(),
                speaker = SpeakerRole.SYSTEM,
                text = "User approved: ${request.gateType.title}",
                language = current.detectedLanguage,
                isSampleMode = false
            )
            val aiTurn = ConversationTurn(
                id = UUID.randomUUID().toString(),
                speaker = SpeakerRole.AI_TO_CALLER,
                text = reply.replyText,
                language = current.detectedLanguage,
                isSampleMode = reply.isSampleMode
            )

            _liveSession.value = current.copy(
                pendingPermission = null,
                turns = current.turns + approvalTurn + aiTurn
            )

            voiceEngine.speak(
                text = reply.replyText,
                language = current.detectedLanguage,
                profile = config.voiceProfile,
                muted = current.isAiMuted
            )
        }
    }

    fun onDenyPermission() {
        scope.launch {
            val current = _liveSession.value
            val request = current.pendingPermission ?: return@launch
            val config = repository.getConfig()

            val declineReply = if (current.detectedLanguage == SupportedLanguage.HINDI) {
                "Maaf kijiye, ${config.userName} ne abhi yeh detail share karne ki permission nahi di hai. Woh khud aapse baat karenge."
            } else {
                "Apologies, ${config.userName} has declined sharing or confirming this right now. They will follow up directly."
            }

            val systemTurn = ConversationTurn(
                id = UUID.randomUUID().toString(),
                speaker = SpeakerRole.SYSTEM,
                text = "User denied: ${request.gateType.title}",
                language = current.detectedLanguage,
                isSampleMode = false
            )
            val aiTurn = ConversationTurn(
                id = UUID.randomUUID().toString(),
                speaker = SpeakerRole.AI_TO_CALLER,
                text = declineReply,
                language = current.detectedLanguage,
                isSampleMode = aiRepository.isSampleMode
            )

            _liveSession.value = current.copy(
                pendingPermission = null,
                turns = current.turns + systemTurn + aiTurn
            )

            voiceEngine.speak(
                text = declineReply,
                language = current.detectedLanguage,
                profile = config.voiceProfile,
                muted = current.isAiMuted
            )
        }
    }

    fun onTakeOverCall() {
        scope.launch {
            voiceEngine.stopSpeaking()
            voiceEngine.stopListening()
            val current = _liveSession.value
            val config = repository.getConfig()
            val handoffMsg = "Connecting you directly to ${config.userName} now."
            _liveSession.value = current.copy(
                phase = CallPhase.PERSONAL_CALL_ACTIVE,
                pendingPermission = null,
                turns = current.turns + ConversationTurn(
                    id = UUID.randomUUID().toString(),
                    speaker = SpeakerRole.SYSTEM,
                    text = "You took over the call. $handoffMsg",
                    language = current.detectedLanguage,
                    isSampleMode = false
                )
            )
        }
    }

    fun onStopAi() {
        voiceEngine.stopSpeaking()
        voiceEngine.stopListening()
        val current = _liveSession.value
        _liveSession.value = current.copy(
            turns = current.turns + ConversationTurn(
                id = UUID.randomUUID().toString(),
                speaker = SpeakerRole.SYSTEM,
                text = "AI paused by user.",
                language = current.detectedLanguage,
                isSampleMode = false
            )
        )
    }

    fun onToggleMuteAi() {
        val current = _liveSession.value
        val newMuted = !current.isAiMuted
        if (newMuted) {
            voiceEngine.stopSpeaking()
        }
        _liveSession.value = current.copy(isAiMuted = newMuted)
    }

    fun onRejectCall() {
        scope.launch {
            voiceEngine.stopSpeaking()
            voiceEngine.stopListening()
            val current = _liveSession.value
            if (current.isRealTelecomCall) {
                activeTelecomCall?.disconnect()
                    ?: TelephonyRoleManager.endCallViaTelecom(appContext)
            }
            val recordId = saveTerminalRecord(current, CallHandlingMode.REJECTED)
            stopForegroundService()
            durationJob?.cancel()
            _liveSession.value = current.copy(
                phase = CallPhase.CALL_SUMMARY_READY,
                completedRecordId = recordId,
                postCallBriefingForUser = "Call from ${current.callerName} was rejected as requested."
            )
        }
    }

    fun onSilentMode() {
        scope.launch {
            voiceEngine.stopSpeaking()
            voiceEngine.stopListening()
            silenceRinger()
            val current = _liveSession.value
            val recordId = saveTerminalRecord(current, CallHandlingMode.SILENT)
            stopForegroundService()
            durationJob?.cancel()
            _liveSession.value = current.copy(
                phase = CallPhase.CALL_SUMMARY_READY,
                completedRecordId = recordId,
                postCallBriefingForUser = "Call from ${current.callerName} placed in Silent Mode."
            )
        }
    }

    fun onEndCall() {
        scope.launch {
            voiceEngine.stopSpeaking()
            voiceEngine.stopListening()
            durationJob?.cancel()

            val current = _liveSession.value
            if (current.isRealTelecomCall) {
                activeTelecomCall?.disconnect()
                    ?: TelephonyRoleManager.endCallViaTelecom(appContext)
                activeTelecomCall = null
            }
            stopForegroundService()

            if (current.phase == CallPhase.IDLE) return@launch

            val config = repository.getConfig()
            val mode = if (current.turns.any { it.speaker == SpeakerRole.AI_TO_CALLER }) {
                CallHandlingMode.AI_HANDLED
            } else {
                CallHandlingMode.ANSWERED_MYSELF
            }

            val summary = aiRepository.summarizeCall(
                callerName = current.callerExtractedName.ifBlank { current.callerName },
                phoneNumber = current.phoneNumber,
                durationSeconds = current.elapsedSeconds,
                turns = current.turns,
                config = config
            )

            val transcriptText = current.turns.joinToString("\n") { turn ->
                "[${turn.speaker.name} | ${turn.language.displayName}] ${turn.text}"
            }

            val record = CallRecord(
                callerName = current.callerExtractedName.ifBlank { current.callerName },
                phoneNumber = current.phoneNumber,
                durationSeconds = current.elapsedSeconds.coerceAtLeast(1),
                detectedLanguage = summary.detectedLanguage,
                handlingMode = mode,
                reasonForCalling = current.callerExtractedReason.ifBlank { summary.reasonForCalling },
                aiSummary = summary.aiSummary,
                requestedAction = summary.requestedAction,
                callerMessage = summary.callerMessage,
                transcript = transcriptText,
                callbackReminderAt = if (summary.needsCallbackReminder) {
                    System.currentTimeMillis() + 30 * 60 * 1000L
                } else null,
                callbackCompleted = false,
                isSavedMessage = summary.callerMessage.isNotBlank() && mode == CallHandlingMode.AI_HANDLED,
                recordingConsentObtained = current.isRecordingWithConsent
            )

            val savedId = repository.saveCallRecord(record)

            _liveSession.value = current.copy(
                phase = CallPhase.CALL_SUMMARY_READY,
                postCallBriefingForUser = summary.postCallUserBriefing,
                completedRecordId = savedId
            )

            val primaryLang = config.preferredLanguages.firstOrNull() ?: SupportedLanguage.HINDI
            voiceEngine.speak(
                text = summary.postCallUserBriefing,
                language = primaryLang,
                profile = config.voiceProfile
            )
        }
    }

    fun dismissCompletedSession() {
        voiceEngine.stopSpeaking()
        voiceEngine.stopListening()
        durationJob?.cancel()
        _liveSession.value = LiveCallSession()
    }

    private suspend fun saveTerminalRecord(
        session: LiveCallSession,
        mode: CallHandlingMode
    ): Long {
        val label = when (mode) {
            CallHandlingMode.REJECTED -> "Call rejected by user authorization."
            CallHandlingMode.SILENT -> "Call silenced by user."
            else -> "Call ended."
        }
        return repository.saveCallRecord(
            CallRecord(
                callerName = session.callerName,
                phoneNumber = session.phoneNumber,
                durationSeconds = session.elapsedSeconds,
                detectedLanguage = session.detectedLanguage,
                handlingMode = mode,
                reasonForCalling = label,
                aiSummary = label,
                requestedAction = "Call back if needed",
                callerMessage = "",
                transcript = session.turns.joinToString("\n") { "${it.speaker.name}: ${it.text}" },
                callbackReminderAt = null,
                callbackCompleted = false,
                isSavedMessage = false,
                recordingConsentObtained = false
            )
        )
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        durationJob = scope.launch {
            while (true) {
                delay(1000L)
                val curr = _liveSession.value
                if (curr.phase == CallPhase.AI_ANSWERING_CALLER || curr.phase == CallPhase.PERSONAL_CALL_ACTIVE) {
                    _liveSession.value = curr.copy(elapsedSeconds = curr.elapsedSeconds + 1)
                } else {
                    break
                }
            }
        }
    }

    private fun routeAudioToSpeakerForAiBridge() {
        try {
            val audioManager = appContext.getSystemService(AudioManager::class.java)
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = true
        } catch (_: Exception) {
        }
    }

    private fun silenceRinger() {
        try {
            val audioManager = appContext.getSystemService(AudioManager::class.java)
            audioManager?.adjustStreamVolume(
                AudioManager.STREAM_RING,
                AudioManager.ADJUST_MUTE,
                0
            )
        } catch (_: Exception) {
        }
    }

    private fun startForegroundService(callerName: String, phoneNumber: String) {
        val intent = Intent(appContext, CallAssistantForegroundService::class.java).apply {
            putExtra(CallAssistantForegroundService.EXTRA_CALLER_NAME, callerName)
            putExtra(CallAssistantForegroundService.EXTRA_PHONE_NUMBER, phoneNumber)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
    }

    private fun stopForegroundService() {
        appContext.stopService(Intent(appContext, CallAssistantForegroundService::class.java))
    }

    companion object {
        @Volatile
        private var INSTANCE: CallStateCoordinator? = null

        fun getInstance(context: Context): CallStateCoordinator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CallStateCoordinator(context).also { INSTANCE = it }
            }
        }
    }
}
