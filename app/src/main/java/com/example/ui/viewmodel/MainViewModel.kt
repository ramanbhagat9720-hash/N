package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.LanguageDetector
import com.example.data.local.LocalAssistantRepository
import com.example.domain.model.AiAnsweringMode
import com.example.domain.model.ApprovedKnowledge
import com.example.domain.model.AssistantConfig
import com.example.domain.model.BlockedNumber
import com.example.domain.model.CallRecord
import com.example.domain.model.ContactHandlingRule
import com.example.domain.model.LiveCallSession
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.TrustedContact
import com.example.domain.model.VoiceProfile
import com.example.telephony.CallStateCoordinator
import com.example.telephony.ContactsHelper
import com.example.telephony.DeviceContactEntry
import com.example.telephony.TelephonyRoleManager
import com.example.telephony.TelephonySystemStatus
import com.example.voice.VoiceEngineState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val config: AssistantConfig = AssistantConfig(),
    val knowledgeList: List<ApprovedKnowledge> = emptyList(),
    val trustedContacts: List<TrustedContact> = emptyList(),
    val blockedNumbers: List<BlockedNumber> = emptyList(),
    val callRecords: List<CallRecord> = emptyList(),
    val liveSession: LiveCallSession = LiveCallSession(),
    val voiceState: VoiceEngineState = VoiceEngineState(),
    val systemStatus: TelephonySystemStatus = TelephonySystemStatus(),
    val deviceContacts: List<DeviceContactEntry> = emptyList(),
    val voiceTestInput: String = "",
    val voiceTestDetectedLanguage: SupportedLanguage = SupportedLanguage.HINDI,
    val voiceTestResponse: String = "",
    val isSampleAiMode: Boolean = true,
    val statusBannerMessage: String? = null
)

class MainViewModel(
    private val appContext: Context,
    private val coordinator: CallStateCoordinator
) : ViewModel() {

    private val repository: LocalAssistantRepository = coordinator.repository
    private val _systemStatus = MutableStateFlow(TelephonyRoleManager.inspectSystemStatus(appContext))
    private val _deviceContacts = MutableStateFlow<List<DeviceContactEntry>>(emptyList())
    private val _voiceTestInput = MutableStateFlow("")
    private val _voiceTestDetectedLang = MutableStateFlow(SupportedLanguage.HINDI)
    private val _voiceTestResponse = MutableStateFlow("")
    private val _statusBanner = MutableStateFlow<String?>(null)

    private data class LocalDataSnapshot(
        val config: AssistantConfig,
        val knowledge: List<ApprovedKnowledge>,
        val trusted: List<TrustedContact>,
        val blocked: List<BlockedNumber>,
        val records: List<CallRecord>
    )

    private data class VoiceTestSnapshot(
        val input: String,
        val lang: SupportedLanguage,
        val response: String,
        val status: TelephonySystemStatus,
        val deviceContacts: List<DeviceContactEntry>,
        val banner: String?
    )

    private val localDataFlow = combine(
        repository.observeConfig(),
        repository.observeKnowledge(),
        repository.observeTrustedContacts(),
        repository.observeBlockedNumbers(),
        repository.observeCallRecords()
    ) { config, knowledge, trusted, blocked, records ->
        LocalDataSnapshot(config, knowledge, trusted, blocked, records)
    }

    private val voiceTestFlow = combine(
        _voiceTestInput,
        _voiceTestDetectedLang,
        _voiceTestResponse,
        _systemStatus,
        _deviceContacts
    ) { input, lang, response, sys, contacts ->
        VoiceTestSnapshot(input, lang, response, sys, contacts, _statusBanner.value)
    }

    val uiState: StateFlow<MainUiState> = combine(
        localDataFlow,
        coordinator.liveSession,
        coordinator.voiceState,
        voiceTestFlow,
        _statusBanner
    ) { local, session, voice, testSnap, banner ->
        MainUiState(
            config = local.config,
            knowledgeList = local.knowledge,
            trustedContacts = local.trusted,
            blockedNumbers = local.blocked,
            callRecords = local.records,
            liveSession = session,
            voiceState = voice,
            systemStatus = testSnap.status,
            deviceContacts = testSnap.deviceContacts,
            voiceTestInput = testSnap.input,
            voiceTestDetectedLanguage = testSnap.lang,
            voiceTestResponse = testSnap.response,
            isSampleAiMode = coordinator.aiRepository.isSampleMode,
            statusBannerMessage = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = MainUiState()
    )

    fun refreshSystemStatus() {
        _systemStatus.value = TelephonyRoleManager.inspectSystemStatus(appContext)
        if (_systemStatus.value.hasContactsPermission) {
            loadDeviceContacts()
        }
    }

    fun loadDeviceContacts() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                ContactsHelper.loadDeviceContacts(appContext)
            }
            _deviceContacts.value = loaded
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun setAssistantEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val updated = uiState.value.config.copy(assistantEnabled = enabled)
            repository.saveConfig(updated)
        }
    }

    fun updateConfig(transform: (AssistantConfig) -> AssistantConfig) {
        viewModelScope.launch {
            val updated = transform(uiState.value.config)
            repository.saveConfig(updated)
            _statusBanner.value = "Assistant settings saved"
        }
    }

    fun togglePreferredLanguage(language: SupportedLanguage) {
        viewModelScope.launch {
            val current = uiState.value.config
            val mutable = current.preferredLanguages.toMutableList()
            if (mutable.contains(language)) {
                if (mutable.size > 1) mutable.remove(language)
            } else {
                mutable.add(language)
            }
            repository.saveConfig(current.copy(preferredLanguages = mutable))
        }
    }

    fun setAnsweringMode(mode: AiAnsweringMode) = updateConfig { it.copy(answeringMode = mode) }

    fun setVoiceProfile(profile: VoiceProfile) {
        viewModelScope.launch {
            val updated = uiState.value.config.copy(voiceProfile = profile)
            repository.saveConfig(updated)
            val lang = updated.preferredLanguages.firstOrNull() ?: SupportedLanguage.HINDI
            coordinator.voiceEngine.speak(
                text = "Namaste ${updated.userName}, main aapka AI call assistant hoon.",
                language = lang,
                profile = profile
            )
        }
    }

    fun saveKnowledge(title: String, category: String, answer: String, id: Long = 0) {
        if (title.isBlank() || answer.isBlank()) return
        viewModelScope.launch {
            repository.saveKnowledge(title, category, answer, id)
            _statusBanner.value = "Approved knowledge saved"
        }
    }

    fun deleteKnowledge(id: Long) {
        viewModelScope.launch { repository.deleteKnowledge(id) }
    }

    fun saveTrustedContact(
        name: String,
        phoneNumber: String,
        relationship: String,
        rule: ContactHandlingRule
    ) {
        if (name.isBlank() || phoneNumber.isBlank()) return
        viewModelScope.launch {
            repository.saveTrustedContact(name, phoneNumber, relationship, rule)
            _statusBanner.value = "$name added to trusted contacts"
        }
    }

    fun deleteTrustedContact(id: Long) {
        viewModelScope.launch { repository.deleteTrustedContact(id) }
    }

    fun blockNumber(phoneNumber: String, reason: String) {
        if (phoneNumber.isBlank()) return
        viewModelScope.launch {
            repository.blockNumber(phoneNumber, reason)
            _statusBanner.value = "$phoneNumber added to blocklist"
        }
    }

    fun unblockNumber(id: Long) {
        viewModelScope.launch { repository.unblockNumber(id) }
    }

    // --- Live Call & Greeter Actions ---

    fun triggerIncomingCallGreeter(callerName: String, phoneNumber: String) {
        coordinator.onIncomingCallDetected(
            phoneNumber = phoneNumber.ifBlank { "+91 98765 43210" },
            contactDisplayName = callerName.ifBlank { "Amit" },
            isRealTelecomCall = false
        )
    }

    fun answerMyself() = coordinator.onAnswerMyself()
    fun letAiTalk() = coordinator.onLetAiTalk()
    fun rejectCall() = coordinator.onRejectCall()
    fun silentMode() = coordinator.onSilentMode()
    fun takeOverCall() = coordinator.onTakeOverCall()
    fun stopAi() = coordinator.onStopAi()
    fun toggleMuteAi() = coordinator.onToggleMuteAi()
    fun endCall() = coordinator.onEndCall()
    fun dismissCompletedSummary() = coordinator.dismissCompletedSession()
    fun startCallerListening() = coordinator.beginListeningTurn()
    fun submitCallerSpeech(text: String) = coordinator.onCallerSpoke(text)
    fun approvePermission() = coordinator.onApprovePermission()
    fun denyPermission() = coordinator.onDenyPermission()

    // --- History & Callbacks ---

    fun setCallbackCompleted(id: Long, completed: Boolean) {
        viewModelScope.launch { repository.setCallbackCompleted(id, completed) }
    }

    fun deleteCallRecord(id: Long) {
        viewModelScope.launch { repository.deleteCallRecord(id) }
    }

    // --- Voice AI Engine Studio ---

    fun updateVoiceTestInput(text: String) {
        _voiceTestInput.value = text
    }

    fun runVoiceTest(utterance: String) {
        val clean = utterance.trim()
        if (clean.isEmpty()) return
        _voiceTestInput.value = clean
        viewModelScope.launch {
            val config = uiState.value.config
            val knowledge = uiState.value.knowledgeList
            val detected = LanguageDetector.detectLanguage(clean, config.preferredLanguages)
            _voiceTestDetectedLang.value = detected

            val reply = coordinator.aiRepository.generateCallerReply(
                callerUtterance = clean,
                callerDisplayName = "Voice Test",
                history = emptyList(),
                config = config,
                approvedKnowledge = knowledge
            )
            _voiceTestResponse.value = reply.replyText
            coordinator.voiceEngine.speak(
                text = reply.replyText,
                language = reply.detectedLanguage,
                profile = config.voiceProfile
            )
        }
    }

    fun startVoiceTestMic() {
        val lang = uiState.value.config.preferredLanguages.firstOrNull() ?: SupportedLanguage.HINDI
        coordinator.voiceEngine.startListening(lang) { recognized ->
            runVoiceTest(recognized)
        }
    }

    fun stopVoiceTestAudio() {
        coordinator.voiceEngine.stopSpeaking()
        coordinator.voiceEngine.stopListening()
    }

    // --- Privacy & Data Deletion ---

    fun enableCallRecordingWithConsent(consentAccepted: Boolean) {
        updateConfig {
            it.copy(
                callRecordingEnabled = consentAccepted,
                recordingConsentAccepted = consentAccepted
            )
        }
    }

    fun disableCallRecording() {
        updateConfig { it.copy(callRecordingEnabled = false) }
    }

    fun deleteAllLocalData() {
        viewModelScope.launch {
            coordinator.dismissCompletedSession()
            repository.clearAllUserData()
            _voiceTestInput.value = ""
            _voiceTestResponse.value = ""
            _statusBanner.value = "All local call records and personal data deleted"
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appCtx = context.applicationContext
            return MainViewModel(appCtx, CallStateCoordinator.getInstance(appCtx)) as T
        }
    }
}
