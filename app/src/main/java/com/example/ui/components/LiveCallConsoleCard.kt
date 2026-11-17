package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CallPhase
import com.example.domain.model.LiveCallSession
import com.example.domain.model.SpeakerRole
import com.example.ui.theme.LocalCallAssistantColors
import com.example.voice.VoiceEngineState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveCallConsoleCard(
    session: LiveCallSession,
    voiceState: VoiceEngineState,
    onAnswerMyself: () -> Unit,
    onLetAiTalk: () -> Unit,
    onRejectCall: () -> Unit,
    onSilentMode: () -> Unit,
    onTakeOverCall: () -> Unit,
    onStopAi: () -> Unit,
    onToggleMuteAi: () -> Unit,
    onEndCall: () -> Unit,
    onStartListening: () -> Unit,
    onSubmitCallerUtterance: (String) -> Unit,
    onApprovePermission: () -> Unit,
    onDenyPermission: () -> Unit,
    onDismissSummary: () -> Unit,
    onDialCallback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalCallAssistantColors.current
    var callerInput by remember { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with AI orb + caller identity
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_ai_orb_1794934889938),
                    contentDescription = stringResource(R.string.cd_ai_orb),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.callerName.ifBlank { "Incoming Caller" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = session.phoneNumber,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    StatusPill(
                        text = when (session.phase) {
                            CallPhase.GREETING_USER_FIRST -> "AI GREETING YOU"
                            CallPhase.AI_ANSWERING_CALLER -> "AI ON CALL"
                            CallPhase.PERSONAL_CALL_ACTIVE -> "PERSONAL CALL"
                            CallPhase.CALL_SUMMARY_READY -> "SUMMARY READY"
                            CallPhase.IDLE -> "IDLE"
                        },
                        containerColor = when (session.phase) {
                            CallPhase.GREETING_USER_FIRST -> semantic.warningContainer
                            CallPhase.AI_ANSWERING_CALLER -> MaterialTheme.colorScheme.primaryContainer
                            CallPhase.PERSONAL_CALL_ACTIVE -> semantic.successContainer
                            CallPhase.CALL_SUMMARY_READY -> MaterialTheme.colorScheme.secondaryContainer
                            CallPhase.IDLE -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = when (session.phase) {
                            CallPhase.GREETING_USER_FIRST -> semantic.warning
                            CallPhase.AI_ANSWERING_CALLER -> MaterialTheme.colorScheme.primary
                            CallPhase.PERSONAL_CALL_ACTIVE -> semantic.success
                            CallPhase.CALL_SUMMARY_READY -> MaterialTheme.colorScheme.secondary
                            CallPhase.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if (session.elapsedSeconds > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = formatDuration(session.elapsedSeconds),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Sample AI notice where AI output appears
            SampleAiNoticeBanner()

            // Waveform & Detected Language Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusPill(
                    text = "Lang: ${session.detectedLanguage.displayName}",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (session.isRecordingWithConsent) {
                    StatusPill(
                        text = "REC (Consent)",
                        containerColor = semantic.dangerContainer,
                        contentColor = semantic.danger
                    )
                } else {
                    StatusPill(
                        text = "Recording OFF",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AcousticWaveVisualizer(
                isActive = voiceState.isSpeaking || voiceState.isListening,
                rmsLevel = voiceState.rmsLevel,
                color = if (voiceState.isListening) semantic.success else MaterialTheme.colorScheme.primary
            )

            // Phase-specific UI
            when (session.phase) {
                CallPhase.GREETING_USER_FIRST -> {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "AI Assistant to You (Before Answering)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = session.userGreetingMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        text = "Authorize how to handle this call:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onAnswerMyself,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = semantic.success,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_answer_myself")
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Answer Myself")
                            }

                            Button(
                                onClick = onLetAiTalk,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_let_ai_talk")
                            ) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Let AI Talk")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRejectCall,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = semantic.danger),
                                border = BorderStroke(1.dp, semantic.danger.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_reject_call")
                            ) {
                                Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Reject Call")
                            }

                            OutlinedButton(
                                onClick = onSilentMode,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_silent_mode")
                            ) {
                                Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Silent Mode")
                            }
                        }
                    }
                }

                CallPhase.AI_ANSWERING_CALLER -> {
                    // Conversation Transcript
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 230.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(session.turns, key = { it.id }) { turn ->
                            val isAi = turn.speaker == SpeakerRole.AI_TO_CALLER || turn.speaker == SpeakerRole.AI_TO_USER
                            val bg = when (turn.speaker) {
                                SpeakerRole.AI_TO_CALLER, SpeakerRole.AI_TO_USER ->
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                SpeakerRole.CALLER -> MaterialTheme.colorScheme.surfaceVariant
                                SpeakerRole.SYSTEM -> semantic.warningContainer.copy(alpha = 0.4f)
                                SpeakerRole.USER -> semantic.successContainer.copy(alpha = 0.45f)
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = bg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = when (turn.speaker) {
                                                SpeakerRole.AI_TO_USER -> "AI -> You"
                                                SpeakerRole.AI_TO_CALLER -> "AI -> Caller"
                                                SpeakerRole.CALLER -> "Caller (${turn.language.displayName})"
                                                SpeakerRole.USER -> "You"
                                                SpeakerRole.SYSTEM -> "System"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isAi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (turn.isSampleMode) {
                                            Text(
                                                text = "Sample AI",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = turn.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Permission Gate Prompt (before personal info / commitments / meetings / orders)
                    val permission = session.pendingPermission
                    if (permission != null) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = semantic.warningContainer,
                            border = BorderStroke(1.dp, semantic.warning),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = semantic.warning)
                                    Text(
                                        text = "Permission Required: ${permission.gateType.title}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = semantic.warning
                                    )
                                }
                                Text(
                                    text = permission.promptToUser,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = onApprovePermission,
                                        colors = ButtonDefaults.buttonColors(containerColor = semantic.success),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("btn_approve_permission")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text("Approve")
                                    }
                                    OutlinedButton(
                                        onClick = onDenyPermission,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("btn_deny_permission")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text("Decline")
                                    }
                                }
                            }
                        }
                    }

                    // Caller Voice / Speech Input Bar (Live Mic + Multilingual Quick Turn)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onStartListening,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (voiceState.isListening) semantic.successContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .testTag("btn_caller_mic")
                        ) {
                            Icon(
                                imageVector = if (voiceState.isListening) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Listen to caller voice",
                                tint = if (voiceState.isListening) semantic.success else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = callerInput,
                            onValueChange = { callerInput = it },
                            placeholder = {
                                Text(
                                    text = voiceState.partialTranscript.ifBlank {
                                        "Caller speech (Hindi, Punjabi, Tamil, English...)"
                                    },
                                    maxLines = 1
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_caller_utterance")
                        )

                        IconButton(
                            onClick = {
                                if (callerInput.isNotBlank()) {
                                    onSubmitCallerUtterance(callerInput)
                                    callerInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_send_caller_utterance")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send caller utterance",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Quick Caller Speech Chips for testing multilingual switching
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickUtterances = listOf(
                            "Mujhe Raman ji se office ke kaam ke liye baat karni thi",
                            "Sat Sri Akal, mainu kal di meeting schedule karni si",
                            "Can you share their home address and confirm my order?"
                        )
                        quickUtterances.forEach { sample ->
                            FilledTonalButton(
                                onClick = { onSubmitCallerUtterance(sample) },
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Text(sample.take(28) + "...", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // 4 Mandatory User Control Buttons
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onTakeOverCall,
                                colors = ButtonDefaults.buttonColors(containerColor = semantic.success),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_take_over_call")
                            ) {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Take Over Call")
                            }

                            OutlinedButton(
                                onClick = onStopAi,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_stop_ai")
                            ) {
                                Icon(Icons.Default.PauseCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Stop AI")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onToggleMuteAi,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_mute_ai")
                            ) {
                                Icon(
                                    imageVector = if (session.isAiMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.size(4.dp))
                                Text(if (session.isAiMuted) "Unmute AI" else "Mute AI")
                            }

                            Button(
                                onClick = onEndCall,
                                colors = ButtonDefaults.buttonColors(containerColor = semantic.danger),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_end_call")
                            ) {
                                Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("End Call")
                            }
                        }
                    }
                }

                CallPhase.PERSONAL_CALL_ACTIVE -> {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = semantic.successContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "You are speaking personally with ${session.callerName}. AI assistant is muted and standing by.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    Button(
                        onClick = onEndCall,
                        colors = ButtonDefaults.buttonColors(containerColor = semantic.danger),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_end_call")
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("End Call & Generate Summary")
                    }
                }

                CallPhase.CALL_SUMMARY_READY -> {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Post-Call AI Briefing",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = session.postCallBriefingForUser,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onDialCallback(session.phoneNumber) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_callback_now")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Call Back")
                        }
                        OutlinedButton(
                            onClick = onDismissSummary,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_dismiss_summary")
                        ) {
                            Text("Done")
                        }
                    }
                }

                CallPhase.IDLE -> Unit
            }
        }
    }
}

fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
