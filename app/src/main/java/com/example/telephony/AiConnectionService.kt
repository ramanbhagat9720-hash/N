package com.example.telephony

import android.os.Build
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

/**
 * Self-managed Telecom [ConnectionService] enabling hosted VoIP / forwarded AI phone sessions
 * to integrate natively with Android's Telecom audio routing and call notifications.
 */
class AiConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val connection = AiCallConnection().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                connectionProperties = Connection.PROPERTY_SELF_MANAGED
            }
            setAddress(request?.address, TelecomManager.PRESENTATION_ALLOWED)
            setInitializing()
            setRinging()
        }
        return connection
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        return AiCallConnection().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                connectionProperties = Connection.PROPERTY_SELF_MANAGED
            }
            setAddress(request?.address, TelecomManager.PRESENTATION_ALLOWED)
            setActive()
        }
    }

    private class AiCallConnection : Connection() {
        override fun onAnswer() {
            setActive()
        }

        override fun onReject() {
            setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
            destroy()
        }

        override fun onDisconnect() {
            setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
            destroy()
        }

        override fun onAbort() {
            setDisconnected(DisconnectCause(DisconnectCause.CANCELED))
            destroy()
        }
    }
}
