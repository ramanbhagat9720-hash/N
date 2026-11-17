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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.CallHandlingMode
import com.example.domain.model.CallRecord
import com.example.ui.components.SampleAiNoticeBanner
import com.example.ui.components.SectionTitle
import com.example.ui.components.StatusPill
import com.example.ui.components.formatDuration
import com.example.ui.theme.LocalCallAssistantColors
import com.example.ui.viewmodel.MainUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistoryFilter(val label: String) {
    ALL("All"),
    AI_HANDLED("AI Handled"),
    SAVED_MESSAGES("Messages"),
    CALLBACKS("Callbacks"),
    MISSED_BLOCKED("Missed / Blocked")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    uiState: MainUiState,
    onToggleCallbackCompleted: (Long, Boolean) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onDialCallback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalCallAssistantColors.current
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var selectedRecord by remember { mutableStateOf<CallRecord?>(null) }

    val filtered = remember(uiState.callRecords, selectedFilter) {
        when (selectedFilter) {
            HistoryFilter.ALL -> uiState.callRecords
            HistoryFilter.AI_HANDLED -> uiState.callRecords.filter { it.handlingMode == CallHandlingMode.AI_HANDLED }
            HistoryFilter.SAVED_MESSAGES -> uiState.callRecords.filter { it.isSavedMessage }
            HistoryFilter.CALLBACKS -> uiState.callRecords.filter { it.callbackReminderAt != null }
            HistoryFilter.MISSED_BLOCKED -> uiState.callRecords.filter {
                it.handlingMode == CallHandlingMode.MISSED ||
                    it.handlingMode == CallHandlingMode.REJECTED ||
                    it.handlingMode == CallHandlingMode.BLOCKED ||
                    it.handlingMode == CallHandlingMode.SILENT
            }
        }
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitle(
                title = "Call Records & Summaries",
                subtitle = "Caller messages, detected languages, AI summaries & callback reminders"
            )
        }

        item {
            SampleAiNoticeBanner()
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HistoryFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No Call Records in \"${selectedFilter.label}\"",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Completed AI calls, saved caller messages, and callback reminders will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { record ->
                Card(
                    onClick = { selectedRecord = record },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.testTag("call_record_${record.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(record.callerName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${record.phoneNumber} • ${dateFormatter.format(Date(record.createdAt))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusPill(
                                text = record.handlingMode.label,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill(
                                text = "Duration: ${formatDuration(record.durationSeconds)}",
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            StatusPill(
                                text = "Lang: ${record.detectedLanguage.displayName}",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            if (record.callbackReminderAt != null) {
                                StatusPill(
                                    text = if (record.callbackCompleted) "Callback Done" else "Callback Pending",
                                    containerColor = if (record.callbackCompleted) semantic.successContainer else semantic.warningContainer,
                                    contentColor = if (record.callbackCompleted) semantic.success else semantic.warning
                                )
                            }
                        }

                        Text(
                            text = "Reason: ${record.reasonForCalling}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = record.aiSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Action: ${record.requestedAction}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onDialCallback(record.phoneNumber) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Call Back")
                            }
                            if (record.callbackReminderAt != null) {
                                OutlinedButton(
                                    onClick = { onToggleCallbackCompleted(record.id, !record.callbackCompleted) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (record.callbackCompleted) Icons.Default.CheckCircle else Icons.Default.Alarm,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.size(4.dp))
                                    Text(if (record.callbackCompleted) "Done" else "Mark Done")
                                }
                            }
                            IconButton(
                                onClick = { onDeleteRecord(record.id) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete call record",
                                    tint = semantic.danger
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedRecord?.let { rec ->
        AlertDialog(
            onDismissRequest = { selectedRecord = null },
            title = { Text("${rec.callerName} — Full Record") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Phone: ${rec.phoneNumber}", style = MaterialTheme.typography.labelMedium)
                    Text("Duration: ${formatDuration(rec.durationSeconds)} | Language: ${rec.detectedLanguage.displayName}", style = MaterialTheme.typography.bodySmall)
                    Text("Reason: ${rec.reasonForCalling}", style = MaterialTheme.typography.bodyMedium)
                    Text("Summary: ${rec.aiSummary}", style = MaterialTheme.typography.bodySmall)
                    Text("Requested Action: ${rec.requestedAction}", style = MaterialTheme.typography.bodySmall)
                    if (rec.callerMessage.isNotBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Saved Message:\n${rec.callerMessage}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    Text(
                        text = "Call Recording Consent: ${if (rec.recordingConsentObtained) "Obtained" else "Disabled (Default)"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (rec.transcript.isNotBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = rec.transcript,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onDialCallback(rec.phoneNumber)
                    selectedRecord = null
                }) { Text("Call Back") }
            },
            dismissButton = {
                TextButton(onClick = { selectedRecord = null }) { Text("Close") }
            }
        )
    }
}
