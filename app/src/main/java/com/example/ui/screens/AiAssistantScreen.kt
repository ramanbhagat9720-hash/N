package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.domain.model.AssistantConfig
import com.example.domain.model.SupportedLanguage
import com.example.ui.components.AcousticWaveVisualizer
import com.example.ui.components.SampleAiNoticeBanner
import com.example.ui.components.SectionTitle
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalCallAssistantColors
import com.example.ui.viewmodel.MainUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
    uiState: MainUiState,
    onSaveConfig: (AssistantConfig) -> Unit,
    onTogglePreferredLanguage: (SupportedLanguage) -> Unit,
    onSaveKnowledge: (String, String, String) -> Unit,
    onDeleteKnowledge: (Long) -> Unit,
    onUpdateVoiceTestInput: (String) -> Unit,
    onRunVoiceTest: (String) -> Unit,
    onStartVoiceTestMic: () -> Unit,
    onStopVoiceTestAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalCallAssistantColors.current
    var userName by remember(uiState.config.userName) { mutableStateOf(uiState.config.userName) }
    var workStart by remember(uiState.config.workingHoursStart) { mutableStateOf(uiState.config.workingHoursStart) }
    var workEnd by remember(uiState.config.workingHoursEnd) { mutableStateOf(uiState.config.workingHoursEnd) }
    var customGreeting by remember(uiState.config.customGreeting) { mutableStateOf(uiState.config.customGreeting) }
    var showAddKnowledgeDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionTitle(
                title = "Personal AI Brain",
                subtitle = "Approved facts, working hours, greetings & multilingual voice studio"
            )
        }

        item {
            SampleAiNoticeBanner()
        }

        // 1. Personal Identity & Working Hours & Custom Greeting
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
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Identity & Schedule", style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("Your Name (spoken by AI)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_user_name")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = workStart,
                            onValueChange = { workStart = it },
                            label = { Text("Work Start") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = workEnd,
                            onValueChange = { workEnd = it },
                            label = { Text("Work End") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = customGreeting,
                        onValueChange = { customGreeting = it },
                        label = { Text("Custom Caller Greeting (use {userName})") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_greeting")
                    )

                    Button(
                        onClick = {
                            onSaveConfig(
                                uiState.config.copy(
                                    userName = userName.trim().ifEmpty { "User" },
                                    workingHoursStart = workStart.trim().ifEmpty { "09:00" },
                                    workingHoursEnd = workEnd.trim().ifEmpty { "19:00" },
                                    customGreeting = customGreeting.trim()
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_save_ai_brain")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Save Personal AI Brain")
                    }
                }
            }
        }

        // 2. Multilingual Auto-Detection & Preferred Languages
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
                    Text("Preferred Languages (12+ Supported)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "AI detects caller speech automatically, handles mixed languages, and never forces English.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SupportedLanguage.entries.forEach { lang ->
                            val selected = uiState.config.preferredLanguages.contains(lang)
                            FilterChip(
                                selected = selected,
                                onClick = { onTogglePreferredLanguage(lang) },
                                label = { Text("${lang.displayName} (${lang.nativeName})") },
                                modifier = Modifier.testTag("chip_lang_${lang.code}")
                            )
                        }
                    }
                }
            }
        }

        // 3. Multilingual Voice AI Engine Studio
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Live Voice AI Studio", style = MaterialTheme.typography.titleMedium)
                        StatusPill(
                            text = "Detected: ${uiState.voiceTestDetectedLanguage.displayName}",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    }

                    AcousticWaveVisualizer(
                        isActive = uiState.voiceState.isSpeaking || uiState.voiceState.isListening,
                        rmsLevel = uiState.voiceState.rmsLevel,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = uiState.voiceTestInput,
                        onValueChange = onUpdateVoiceTestInput,
                        label = { Text("Speak or type in Hindi, Punjabi, Urdu, Tamil, Bengali, English...") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_voice_test")
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Hindi" to "Mujhe office ke kaam ke liye baat karni thi",
                            "Punjabi" to "Sat Sri Akal, mainu kal di meeting layi gal karni si",
                            "Tamil" to "Vanakkam, naan ungaludan pesanum",
                            "English" to "What are your working hours?"
                        ).forEach { (label, phrase) ->
                            FilledTonalButton(
                                onClick = { onRunVoiceTest(phrase) },
                                modifier = Modifier.heightIn(min = 38.dp)
                            ) {
                                Text("Try $label")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onStartVoiceTestMic,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_voice_studio_mic")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(if (uiState.voiceState.isListening) "Listening..." else "Speak")
                        }
                        FilledTonalButton(
                            onClick = { onRunVoiceTest(uiState.voiceTestInput) },
                            enabled = uiState.voiceTestInput.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_voice_studio_run")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("Test Reply")
                        }
                        OutlinedButton(
                            onClick = onStopVoiceTestAudio,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_voice_studio_stop")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop voice audio")
                        }
                    }

                    if (uiState.voiceTestResponse.isNotBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "AI Response (${uiState.voiceTestDetectedLanguage.displayName}) • Sample Mode",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = uiState.voiceTestResponse,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Approved Business Info & FAQs (Never invent answers)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(
                    title = "Approved Answers & FAQs",
                    subtitle = "AI only shares information explicitly approved here",
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = { showAddKnowledgeDialog = true },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_add_knowledge")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Add FAQ")
                }
            }
        }

        items(uiState.knowledgeList, key = { it.id }) { item ->
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            StatusPill(
                                text = item.category,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text(
                            text = item.approvedAnswer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onDeleteKnowledge(item.id) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete approved FAQ",
                            tint = semantic.danger
                        )
                    }
                }
            }
        }
    }

    if (showAddKnowledgeDialog) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Business Info") }
        var answer by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddKnowledgeDialog = false },
            title = { Text("Add Approved Answer / FAQ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Topic / Keyword (e.g., Office Address)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        label = { Text("Exact Approved Answer") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveKnowledge(title, category, answer)
                        showAddKnowledgeDialog = false
                    },
                    enabled = title.isNotBlank() && answer.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddKnowledgeDialog = false }) { Text("Cancel") }
            }
        )
    }
}
