package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.VoiceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

data class VoiceEngineState(
    val isTtsReady: Boolean = false,
    val isSpeaking: Boolean = false,
    val isListening: Boolean = false,
    val speechRecognitionAvailable: Boolean = false,
    val partialTranscript: String = "",
    val rmsLevel: Float = 0f,
    val activeLocaleTag: String = "hi-IN",
    val lastError: String? = null
)

class VoiceAiEngine(context: Context) {

    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(
        VoiceEngineState(
            speechRecognitionAvailable = SpeechRecognizer.isRecognitionAvailable(appContext)
        )
    )
    val state: StateFlow<VoiceEngineState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentVoiceProfile: VoiceProfile = VoiceProfile.WARM_FRIENDLY
    private var currentLocale: Locale = SupportedLanguage.HINDI.locale
    private var onFinalResultCallback: ((String) -> Unit)? = null
    private var onSpeakingDoneCallback: (() -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _state.value = _state.value.copy(isSpeaking = true, lastError = null)
                    }

                    override fun onDone(utteranceId: String?) {
                        _state.value = _state.value.copy(isSpeaking = false)
                        onSpeakingDoneCallback?.invoke()
                        onSpeakingDoneCallback = null
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _state.value = _state.value.copy(isSpeaking = false)
                        onSpeakingDoneCallback?.invoke()
                        onSpeakingDoneCallback = null
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _state.value = _state.value.copy(
                            isSpeaking = false,
                            lastError = "TTS error code: $errorCode"
                        )
                        onSpeakingDoneCallback?.invoke()
                        onSpeakingDoneCallback = null
                    }
                })
                applyVoiceSettings(currentVoiceProfile, currentLocale)
                _state.value = _state.value.copy(isTtsReady = true)
            } else {
                Log.w(TAG, "TextToSpeech init failed with status=$status")
                _state.value = _state.value.copy(
                    isTtsReady = false,
                    lastError = "Device TTS engine unavailable"
                )
            }
        }
    }

    fun applyVoiceSettings(profile: VoiceProfile, locale: Locale) {
        currentVoiceProfile = profile
        currentLocale = locale
        tts?.let { engine ->
            engine.setPitch(profile.pitch)
            engine.setSpeechRate(profile.speechRate)
            val result = engine.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale("en", "IN"))
            }
        }
        _state.value = _state.value.copy(activeLocaleTag = locale.toLanguageTag())
    }

    fun speak(
        text: String,
        language: SupportedLanguage,
        profile: VoiceProfile = currentVoiceProfile,
        muted: Boolean = false,
        onDone: (() -> Unit)? = null
    ) {
        if (muted || text.isBlank()) {
            onDone?.invoke()
            return
        }
        applyVoiceSettings(profile, language.locale)
        val engine = tts
        if (engine == null || !_state.value.isTtsReady) {
            onDone?.invoke()
            return
        }
        onSpeakingDoneCallback = onDone
        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle()
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stopSpeaking() {
        tts?.stop()
        onSpeakingDoneCallback = null
        _state.value = _state.value.copy(isSpeaking = false)
    }

    fun startListening(
        language: SupportedLanguage,
        onFinalResult: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            _state.value = _state.value.copy(
                speechRecognitionAvailable = false,
                isListening = false,
                lastError = "Speech recognition service unavailable on this device"
            )
            return
        }
        onFinalResultCallback = onFinalResult
        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                    setRecognitionListener(createRecognitionListener())
                }
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.bcp47Tag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            _state.value = _state.value.copy(
                isListening = true,
                partialTranscript = "",
                lastError = null,
                activeLocaleTag = language.bcp47Tag
            )
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
            _state.value = _state.value.copy(
                isListening = false,
                lastError = e.localizedMessage ?: "Microphone listening failed"
            )
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
        _state.value = _state.value.copy(isListening = false, rmsLevel = 0f)
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = _state.value.copy(isListening = true, lastError = null)
        }

        override fun onBeginningOfSpeech() {
            // Voice interruption (barge-in): halt TTS immediately when caller/user starts speaking
            if (_state.value.isSpeaking) {
                stopSpeaking()
            }
        }

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _state.value = _state.value.copy(rmsLevel = normalized)
            if (rmsdB > 6.5f && _state.value.isSpeaking) {
                stopSpeaking()
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _state.value = _state.value.copy(isListening = false, rmsLevel = 0f)
        }

        override fun onError(error: Int) {
            val msg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech network error"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out"
                else -> "Speech recognizer error ($error)"
            }
            _state.value = _state.value.copy(isListening = false, rmsLevel = 0f, lastError = msg)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull().orEmpty().trim()
            _state.value = _state.value.copy(
                isListening = false,
                partialTranscript = "",
                rmsLevel = 0f
            )
            if (text.isNotEmpty()) {
                onFinalResultCallback?.invoke(text)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull().orEmpty()
            if (partial.isNotBlank()) {
                if (_state.value.isSpeaking) {
                    stopSpeaking()
                }
                _state.value = _state.value.copy(partialTranscript = partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun release() {
        stopSpeaking()
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.shutdown()
        tts = null
    }

    companion object {
        private const val TAG = "VoiceAiEngine"
    }
}
