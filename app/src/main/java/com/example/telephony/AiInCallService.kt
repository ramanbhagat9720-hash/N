package com.example.telephony

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

/**
 * Telecom [InCallService] active when the user grants the Default Dialer role ([RoleManager.ROLE_DIALER]).
 * Binds live Telecom [Call] objects to [CallStateCoordinator] so Answer Myself, Let AI Talk,
 * Reject Call, and End Call control real cellular/VoIP calls.
 */
class AiInCallService : InCallService() {

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            if (state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) {
                CallStateCoordinator.getInstance(applicationContext).onEndCall()
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callCallback)
        val coordinator = CallStateCoordinator.getInstance(applicationContext)
        coordinator.attachTelecomCall(call)

        val number = call.details?.handle?.schemeSpecificPart.orEmpty().ifBlank { "Private Number" }
        val contactName = call.details?.callerDisplayName?.takeIf { it.isNotBlank() }
            ?: ContactsHelper.lookupCallerName(applicationContext, number)

        val state = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            call.details.state
        } else {
            @Suppress("DEPRECATION")
            call.state
        }

        if (state == Call.STATE_RINGING) {
            coordinator.onIncomingCallDetected(
                phoneNumber = number,
                contactDisplayName = contactName,
                isRealTelecomCall = true
            )
            val intent = Intent(applicationContext, IncomingCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(IncomingCallActivity.EXTRA_CALLER_NAME, contactName ?: "Unknown Caller")
                putExtra(IncomingCallActivity.EXTRA_PHONE_NUMBER, number)
            }
            try {
                startActivity(intent)
            } catch (_: Exception) {
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        val coordinator = CallStateCoordinator.getInstance(applicationContext)
        coordinator.attachTelecomCall(null)
    }
}
