package com.example.telephony

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.CallPhase
import com.example.ui.components.LiveCallConsoleCard
import com.example.ui.theme.AICallAssistantTheme

class IncomingCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val coordinator = CallStateCoordinator.getInstance(applicationContext)

        val callerName = intent.getStringExtra(EXTRA_CALLER_NAME).orEmpty()
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER).orEmpty()
        if (coordinator.liveSession.value.phase == CallPhase.IDLE && phoneNumber.isNotBlank()) {
            coordinator.onIncomingCallDetected(
                phoneNumber = phoneNumber,
                contactDisplayName = callerName,
                isRealTelecomCall = true
            )
        }

        setContent {
            AICallAssistantTheme {
                val session by coordinator.liveSession.collectAsStateWithLifecycle()
                val voiceState by coordinator.voiceState.collectAsStateWithLifecycle()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        LiveCallConsoleCard(
                            session = session,
                            voiceState = voiceState,
                            onAnswerMyself = { coordinator.onAnswerMyself() },
                            onLetAiTalk = { coordinator.onLetAiTalk() },
                            onRejectCall = {
                                coordinator.onRejectCall()
                                finish()
                            },
                            onSilentMode = {
                                coordinator.onSilentMode()
                                finish()
                            },
                            onTakeOverCall = { coordinator.onTakeOverCall() },
                            onStopAi = { coordinator.onStopAi() },
                            onToggleMuteAi = { coordinator.onToggleMuteAi() },
                            onEndCall = { coordinator.onEndCall() },
                            onStartListening = { coordinator.beginListeningTurn() },
                            onSubmitCallerUtterance = { coordinator.onCallerSpoke(it) },
                            onApprovePermission = { coordinator.onApprovePermission() },
                            onDenyPermission = { coordinator.onDenyPermission() },
                            onDismissSummary = {
                                coordinator.dismissCompletedSession()
                                finish()
                            },
                            onDialCallback = { number ->
                                TelephonyRoleManager.launchDialerCallback(this@IncomingCallActivity, number)
                            }
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_CALLER_NAME = "extra_caller_name"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
    }
}
