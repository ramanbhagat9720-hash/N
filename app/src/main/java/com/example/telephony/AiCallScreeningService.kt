package com.example.telephony

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import com.example.domain.model.CallHandlingMode
import com.example.domain.model.CallRecord
import com.example.domain.model.SupportedLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Android 10+ [CallScreeningService] implementation.
 * Screens incoming calls against the user's blocklist, identifies saved contacts,
 * and alerts [CallStateCoordinator] so AI greets the user first before answering or rejecting.
 */
class AiCallScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            true
        }

        if (!isIncoming) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val rawNumber = callDetails.handle?.schemeSpecificPart.orEmpty()
        val coordinator = CallStateCoordinator.getInstance(applicationContext)

        scope.launch {
            val config = coordinator.repository.getConfig()
            val blocked = coordinator.repository.isBlocked(rawNumber)

            if (blocked) {
                val response = CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipNotification(true)
                    .build()
                respondToCall(callDetails, response)

                coordinator.repository.saveCallRecord(
                    CallRecord(
                        callerName = "Blocked Caller",
                        phoneNumber = rawNumber,
                        durationSeconds = 0,
                        detectedLanguage = SupportedLanguage.ENGLISH,
                        handlingMode = CallHandlingMode.BLOCKED,
                        reasonForCalling = "Blocked by user blocklist",
                        aiSummary = "Incoming call automatically blocked per blocklist rules.",
                        requestedAction = "None",
                        callerMessage = "",
                        transcript = "",
                        callbackReminderAt = null,
                        callbackCompleted = true,
                        isSavedMessage = false,
                        recordingConsentObtained = false
                    )
                )
                return@launch
            }

            // Never answer or reject without user authorization; let the call ring/notify
            // while AI greets the user first and presents Answer Myself / Let AI Talk / Reject / Silent.
            val verificationFailed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                callDetails.callerNumberVerificationStatus == Connection.VERIFICATION_STATUS_FAILED
            } else {
                false
            }

            val contactName = ContactsHelper.lookupCallerName(applicationContext, rawNumber)
                ?: if (verificationFailed) "Unverified Caller" else null

            respondToCall(
                callDetails,
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSilenceCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build()
            )

            if (config.assistantEnabled) {
                coordinator.onIncomingCallDetected(
                    phoneNumber = rawNumber.ifBlank { "Private Number" },
                    contactDisplayName = contactName,
                    isRealTelecomCall = true
                )

                val launchIntent = Intent(applicationContext, IncomingCallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(IncomingCallActivity.EXTRA_CALLER_NAME, contactName ?: "Unknown Caller")
                    putExtra(IncomingCallActivity.EXTRA_PHONE_NUMBER, rawNumber)
                }
                try {
                    startActivity(launchIntent)
                } catch (_: Exception) {
                }
            }
        }
    }
}
