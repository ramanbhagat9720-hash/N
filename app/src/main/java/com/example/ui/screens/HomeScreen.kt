package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CallPhase
import com.example.ui.components.SampleAiNoticeBanner
import com.example.ui.components.SectionTitle
import com.example.ui.components.StatusPill
import com.example.ui.components.formatDuration
import com.example.ui.theme.LocalCallAssistantColors
import com.example.ui.viewmodel.MainUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    uiState: MainUiState,
    onToggleAssistant: (Boolean) -> Unit,
    onTriggerIncomingCall: (String, String) -> Unit,
    onNavigateToCalls: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalCallAssistantColors.current
    var testCallerName by remember { mutableStateOf("Amit") }
    var testPhoneNumber by remember { mutableStateOf("+91 98765 43210") }

    val savedMessages = remember(uiState.callRecords) {
        uiState.callRecords.filter { it.isSavedMessage }
    }
    val pendingCallbacks = remember(uiState.callRecords) {
        uiState.callRecords.filter { it.callbackReminderAt != null && !it.callbackCompleted }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Dashboard Banner + Master AI Assistant Toggle
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_assistant_hero_1794934890250),
                        contentDescription = stringResource(R.string.cd_hero_banner),
                        contentScale = ContentScale.Crop,
                        alpha = 0.35f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(172.dp)
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.52f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(172.dp)
                    ) {}

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_ai_orb_1794934889938),
                                contentDescription = stringResource(R.string.cd_ai_orb),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Welcome, ${uiState.config.userName}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uiState.config.assistantEnabled) {
                                        "AI Greets You First • Ready"
                                    } else {
                                        "AI Assistant Paused"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.config.assistantEnabled) {
                                        semantic.success
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            Switch(
                                checked = uiState.config.assistantEnabled,
                                onCheckedChange = onToggleAssistant,
                                modifier = Modifier.testTag("switch_assistant_enabled")
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatusPill(
                                text = uiState.config.answeringMode.label,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                            StatusPill(
                                text = if (uiState.config.autoLanguageDetection) {
                                    "Auto Language: 12+ Supported"
                                } else {
                                    "Fixed Language"
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            StatusPill(
                                text = if (uiState.systemStatus.hasCallScreeningRole) {
                                    "Call Screening Active"
                                } else {
                                    "Role Setup Needed"
                                },
                                containerColor = if (uiState.systemStatus.hasCallScreeningRole) {
                                    semantic.successContainer
                                } else {
                                    semantic.warningContainer
                                },
                                contentColor = if (uiState.systemStatus.hasCallScreeningRole) {
                                    semantic.success
                                } else {
                                    semantic.warning
                                }
                            )
                        }
                    }
                }
            }
        }

        // 2. Sample AI Notice
        item {
            SampleAiNoticeBanner()
        }

        // 3. Active Call Banner if a call session is currently live
        if (uiState.liveSession.phase != CallPhase.IDLE) {
            item {
                Card(
                    onClick = onNavigateToCalls,
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("card_active_call_jump")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Live Call Session: ${uiState.liveSession.callerName}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap to open Live Call Control Console",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }

        // 4. Interactive Incoming Call Greeter Launcher
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Test AI Call Greeter Flow",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Trigger the \"AI Greets You First\" call experience before answering or letting AI handle the caller.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = testCallerName,
                            onValueChange = { testCallerName = it },
                            label = { Text("Caller Name") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_test_caller_name")
                        )
                        OutlinedTextField(
                            value = testPhoneNumber,
                            onValueChange = { testPhoneNumber = it },
                            label = { Text("Phone") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_test_caller_phone")
                        )
                    }
                    Button(
                        onClick = {
                            onTriggerIncomingCall(testCallerName, testPhoneNumber)
                            onNavigateToCalls()
                        },
                        enabled = uiState.config.assistantEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_trigger_incoming_call")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Receive Incoming Call (AI Greets First)")
                    }
                }
            }
        }

        // 5. Quick Dashboard Grid (Section 9)
        item {
            SectionTitle(
                title = "Command Center",
                subtitle = "Incoming screening, multilingual brain, voice & call controls"
            )
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardTile(
                        icon = Icons.Default.Shield,
                        title = "Incoming Rules",
                        value = uiState.config.answeringMode.label,
                        subtitle = "${uiState.config.answeringDelaySeconds}s delay",
                        onClick = onNavigateToSettings,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardTile(
                        icon = Icons.Default.Translate,
                        title = "Auto Language",
                        value = if (uiState.config.autoLanguageDetection) "Automatic" else "Manual",
                        subtitle = "${uiState.config.preferredLanguages.size} preferred",
                        onClick = onNavigateToAssistant,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardTile(
                        icon = Icons.Default.RecordVoiceOver,
                        title = "AI Voice",
                        value = uiState.config.voiceProfile.label.substringBefore(" —"),
                        subtitle = if (uiState.voiceState.isTtsReady) "TTS Engine Ready" else "Initializing",
                        onClick = onNavigateToSettings,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardTile(
                        icon = Icons.Default.Psychology,
                        title = "Personal Brain",
                        value = "${uiState.knowledgeList.size} Approved FAQs",
                        subtitle = "${uiState.trustedContacts.size} trusted contacts",
                        onClick = onNavigateToAssistant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 6. Pending Callback Reminders & Saved Messages
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(
                    title = "Recent Summaries & Messages",
                    subtitle = "${pendingCallbacks.size} pending callbacks • ${savedMessages.size} saved messages",
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = onNavigateToHistory,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_view_all_history")
                ) {
                    Text("View All")
                }
            }
        }

        if (uiState.callRecords.isEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "No Call Summaries Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Incoming calls screened or answered by your AI assistant will appear here with detected language, caller message, and callback reminders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.callRecords.take(4), key = { it.id }) { record ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToHistory() },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = record.callerName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            StatusPill(
                                text = "${record.detectedLanguage.displayName} • ${formatDuration(record.durationSeconds)}",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = record.reasonForCalling,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = record.aiSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardTile(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 104.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
