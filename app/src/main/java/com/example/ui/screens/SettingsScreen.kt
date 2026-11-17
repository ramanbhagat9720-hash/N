package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsPhone
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.AiAnsweringMode
import com.example.domain.model.AssistantConfig
import com.example.domain.model.VoiceProfile
import com.example.telephony.TelephonyRoleManager
import com.example.ui.components.SectionTitle
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalCallAssistantColors
import com.example.ui.viewmodel.MainUiState
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onToggleAssistant: (Boolean) -> Unit,
    onSetVoiceProfile: (VoiceProfile) -> Unit,
    onSetAnsweringMode: (AiAnsweringMode) -> Unit,
    onSaveConfig: (AssistantConfig) -> Unit,
    onRequestPermissions: () -> Unit,
    onRequestCallScreeningRole: () -> Unit,
    onRequestDefaultDialerRole: () -> Unit,
    onLaunchMmiCode: (String) -> Unit,
    onEnableRecordingWithConsent: (Boolean) -> Unit,
    onDisableRecording: () -> Unit,
    onDeleteAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalCallAssistantColors.current
    var hostedNumber by remember(uiState.config.hostedAiPhoneNumber) {
        mutableStateOf(uiState.config.hostedAiPhoneNumber)
    }
    var showRecordingConsentDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showCompatibilityDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionTitle(
                title = "Admin, Telephony & Privacy",
                subtitle = "Voice, answering rules, system roles, forwarding & security"
            )
        }

        // 1. Master Switches
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI Call Assistant", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Disable AI answering anytime",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.config.assistantEnabled,
                            onCheckedChange = onToggleAssistant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Language Detection", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Detect and switch across 12+ languages dynamically",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.config.autoLanguageDetection,
                            onCheckedChange = { enabled ->
                                onSaveConfig(uiState.config.copy(autoLanguageDetection = enabled))
                            },
                            modifier = Modifier.testTag("switch_auto_lang")
                        )
                    }

                    Column {
                        Text(
                            text = "AI Answering Delay: ${uiState.config.answeringDelaySeconds}s",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Slider(
                            value = uiState.config.answeringDelaySeconds.toFloat(),
                            onValueChange = { value ->
                                onSaveConfig(uiState.config.copy(answeringDelaySeconds = value.roundToInt()))
                            },
                            valueRange = 0f..15f,
                            steps = 14
                        )
                    }
                }
            }
        }

        // 2. AI Voice Selection
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("AI Voice Selection (Tap to Preview)", style = MaterialTheme.typography.titleMedium)
                    }

                    VoiceProfile.entries.forEach { profile ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.config.voiceProfile == profile,
                                onClick = { onSetVoiceProfile(profile) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(profile.label, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    profile.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. AI Answering Mode
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("AI Answering Mode", style = MaterialTheme.typography.titleMedium)
                    }
                    AiAnsweringMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.config.answeringMode == mode,
                                onClick = { onSetAnsweringMode(mode) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mode.label, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Android Telephony Roles & Permissions
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Android Telephony & Permissions", style = MaterialTheme.typography.titleMedium)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(
                            text = if (uiState.systemStatus.hasCallScreeningRole) "Screening Role: Granted" else "Screening Role: Off",
                            containerColor = if (uiState.systemStatus.hasCallScreeningRole) semantic.successContainer else semantic.warningContainer,
                            contentColor = if (uiState.systemStatus.hasCallScreeningRole) semantic.success else semantic.warning
                        )
                        StatusPill(
                            text = if (uiState.systemStatus.isDefaultDialer) "Default Dialer: Yes" else "Default Dialer: No",
                            containerColor = if (uiState.systemStatus.isDefaultDialer) semantic.successContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (uiState.systemStatus.isDefaultDialer) semantic.success else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Permissions: Mic=${uiState.systemStatus.hasRecordAudioPermission}, Contacts=${uiState.systemStatus.hasContactsPermission}, Phone=${uiState.systemStatus.hasPhoneStatePermission}, Answer=${uiState.systemStatus.hasAnswerCallsPermission}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_request_permissions")
                    ) {
                        Text("Grant Call, Mic & Contact Permissions")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onRequestCallScreeningRole,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_role_screening")
                        ) {
                            Text("Set Screening App")
                        }
                        OutlinedButton(
                            onClick = onRequestDefaultDialerRole,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_role_dialer")
                        ) {
                            Text("Set Dialer App")
                        }
                    }
                }
            }
        }

        // 5. Hosted AI Number & Call Forwarding Alternative (Section 12)
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
                        Icon(Icons.Default.SettingsPhone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Hosted AI Bridge & Call Forwarding", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "Because standard Android apps cannot inject audio directly into the cellular uplink stream on all devices, configure conditional forwarding (*61*) to your hosted AI voice bridge number when unanswered or busy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = hostedNumber,
                        onValueChange = { hostedNumber = it },
                        label = { Text("Hosted AI Phone Number (e.g. +91...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveConfig(
                                    uiState.config.copy(
                                        hostedAiPhoneNumber = hostedNumber.trim(),
                                        callForwardingEnabled = true
                                    )
                                )
                                val mmi = TelephonyRoleManager.buildConditionalForwardMmiCode(
                                    hostedNumber.trim(),
                                    uiState.config.answeringDelaySeconds
                                )
                                onLaunchMmiCode(mmi)
                            },
                            enabled = hostedNumber.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Enable Forwarding")
                        }
                        OutlinedButton(
                            onClick = {
                                onSaveConfig(uiState.config.copy(callForwardingEnabled = false))
                                onLaunchMmiCode(TelephonyRoleManager.buildCancelForwardingMmiCode())
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Cancel (##002#)")
                        }
                    }
                }
            }
        }

        // 6. Security, Recording Consent, Privacy Policy & Device Compatibility Report
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Security & Privacy Controls", style = MaterialTheme.typography.titleMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Call Audio Recording", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Disabled by default. Requires explicit legal consent.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.config.callRecordingEnabled,
                            onCheckedChange = { wantEnable ->
                                if (wantEnable) {
                                    showRecordingConsentDialog = true
                                } else {
                                    onDisableRecording()
                                }
                            },
                            modifier = Modifier.testTag("switch_call_recording")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPrivacyDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("Privacy & Terms")
                        }
                        OutlinedButton(
                            onClick = { showCompatibilityDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("Compatibility")
                        }
                    }

                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = semantic.danger),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_delete_all_data")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Delete All Local Data")
                    }
                }
            }
        }
    }

    if (showRecordingConsentDialog) {
        AlertDialog(
            onDismissRequest = { showRecordingConsentDialog = false },
            title = { Text("Call Recording Consent Required") },
            text = {
                Text(
                    "Call audio recording is disabled by default. By enabling this option, you confirm that you will obtain caller consent and comply with all local telecommunications and two-party consent laws."
                )
            },
            confirmButton = {
                Button(onClick = {
                    onEnableRecordingWithConsent(true)
                    showRecordingConsentDialog = false
                }) { Text("I Consent & Enable") }
            },
            dismissButton = {
                TextButton(onClick = { showRecordingConsentDialog = false }) { Text("Keep Disabled") }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete All Data?") },
            text = { Text("This permanently deletes all call records, saved messages, trusted contacts, blocklist numbers, and custom AI knowledge stored on this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllData()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = semantic.danger)
                ) { Text("Delete Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy & Terms") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "1. Explicit AI Disclosure: AI Call Assistant always informs callers they are speaking with an automated AI assistant.\n\n" +
                            "2. Zero Secret Recording: Call recording is off by default and never activates silently.\n\n" +
                            "3. Permission-Gated Contacts & Calls: Contacts and phone state are accessed strictly after runtime permission grant.\n\n" +
                            "4. Data Control: All personal brain settings, summaries, and messages reside locally in encrypted app storage until you choose to delete them or enable cloud sync.\n\n" +
                            "5. Personal Information Protection: AI never shares personal info, confirms business orders, or books meetings without your live approval.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Close") }
            }
        )
    }

    if (showCompatibilityDialog) {
        AlertDialog(
            onDismissRequest = { showCompatibilityDialog = false },
            icon = { Icon(Icons.Default.GppGood, contentDescription = null) },
            title = { Text("Device Compatibility & Architecture") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Android Telephony Architecture:\n" +
                            "• CallScreeningService (API 29+): Identifies callers, enforces blocklist, and notifies user before answering.\n" +
                            "• InCallService (ROLE_DIALER): Controls incoming call answer/reject/disconnect when set as Default Dialer.\n" +
                            "• ConnectionService: Supports self-managed VoIP & hosted AI bridge routing.\n\n" +
                            "Important Hardware & OS Limitation:\n" +
                            "Standard Android OS security restricts third-party apps from injecting TTS audio directly into the raw cellular uplink (VOICE_UPLINK) on unrooted devices. On devices without speakerphone acoustic coupling or OEM support, configure Conditional Call Forwarding (*61*) to a hosted SIP/PSTN AI voice gateway.\n\n" +
                            "Voice & AI Status:\n" +
                            "• Device TTS Ready: ${uiState.voiceState.isTtsReady}\n" +
                            "• SpeechRecognizer Available: ${uiState.voiceState.speechRecognitionAvailable}\n" +
                            "• AI Engine Mode: Sample Placeholder Mode (enable Firebase AI Logic for live Gemini NLU).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCompatibilityDialog = false }) { Text("Understood") }
            }
        )
    }
}
