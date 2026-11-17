package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.CallPhase
import com.example.domain.model.ContactHandlingRule
import com.example.ui.components.LiveCallConsoleCard
import com.example.ui.components.SectionTitle
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalCallAssistantColors
import com.example.ui.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    uiState: MainUiState,
    onTriggerIncomingCall: (String, String) -> Unit,
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
    onSaveTrustedContact: (String, String, String, ContactHandlingRule) -> Unit,
    onDeleteTrustedContact: (Long) -> Unit,
    onLoadDeviceContacts: () -> Unit,
    onBlockNumber: (String, String) -> Unit,
    onUnblockNumber: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalCallAssistantColors.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Live Console", "Trusted", "Blocklist")

    var showAddTrustedDialog by remember { mutableStateOf(false) }
    var showImportContactsDialog by remember { mutableStateOf(false) }
    var showAddBlockDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                SegmentedButton(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                    modifier = Modifier.testTag("calls_tab_${title.lowercase().replace(" ", "_")}")
                ) {
                    Text(title)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    if (uiState.liveSession.phase != CallPhase.IDLE) {
                        item {
                            LiveCallConsoleCard(
                                session = uiState.liveSession,
                                voiceState = uiState.voiceState,
                                onAnswerMyself = onAnswerMyself,
                                onLetAiTalk = onLetAiTalk,
                                onRejectCall = onRejectCall,
                                onSilentMode = onSilentMode,
                                onTakeOverCall = onTakeOverCall,
                                onStopAi = onStopAi,
                                onToggleMuteAi = onToggleMuteAi,
                                onEndCall = onEndCall,
                                onStartListening = onStartListening,
                                onSubmitCallerUtterance = onSubmitCallerUtterance,
                                onApprovePermission = onApprovePermission,
                                onDenyPermission = onDenyPermission,
                                onDismissSummary = onDismissSummary,
                                onDialCallback = onDialCallback
                            )
                        }
                    } else {
                        item {
                            SectionTitle(
                                title = "Call Screening & Live Control",
                                subtitle = "Waiting for incoming calls via CallScreeningService / InCallService"
                            )
                        }
                        item {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "No Active Incoming Call",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "When an incoming call arrives, AI greets you first by caller name, asks whether to answer personally or let AI talk, and stays under your live control.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = { onTriggerIncomingCall("Amit", "+91 98765 43210") },
                                            enabled = uiState.config.assistantEnabled,
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("btn_calls_test_amit")
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.size(6.dp))
                                            Text("Amit Calling")
                                        }
                                        FilledTonalButton(
                                            onClick = { onTriggerIncomingCall("Unknown Caller", "+91 99100 11223") },
                                            enabled = uiState.config.assistantEnabled,
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("btn_calls_test_unknown")
                                        ) {
                                            Text("Unknown Caller")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionTitle(
                                title = "Trusted Contacts",
                                subtitle = "VIPs and per-caller handling rules",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showAddTrustedDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_add_trusted_contact")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Add Contact")
                            }
                            OutlinedButton(
                                onClick = {
                                    onLoadDeviceContacts()
                                    showImportContactsDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_import_contacts")
                            ) {
                                Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("From Phone")
                            }
                        }
                    }

                    if (uiState.trustedContacts.isEmpty()) {
                        item {
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = "No trusted contacts added yet. Add VIP family or business contacts to customize how AI greets or routes their calls.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(uiState.trustedContacts, key = { it.id }) { contact ->
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
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = semantic.success
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${contact.name} (${contact.relationship})",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = contact.phoneNumber,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        StatusPill(
                                            text = contact.rule.label,
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteTrustedContact(contact.id) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove trusted contact",
                                            tint = semantic.danger
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        SectionTitle(
                            title = "Blocklist Management",
                            subtitle = "Numbers here are automatically rejected by CallScreeningService"
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { showAddBlockDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("btn_add_blocked_number")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Block Phone Number")
                        }
                    }

                    if (uiState.blockedNumbers.isEmpty()) {
                        item {
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = "Your blocklist is empty. Spam or telemarketing numbers you add will be blocked silently at the system screening layer.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(uiState.blockedNumbers, key = { it.id }) { blocked ->
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, semantic.danger.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = null, tint = semantic.danger)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = blocked.phoneNumber,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = blocked.reason,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onUnblockNumber(blocked.id) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Unblock number"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTrustedDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var relation by remember { mutableStateOf("Family / Business") }
        var selectedRule by remember { mutableStateOf(ContactHandlingRule.GREET_FIRST) }
        var ruleMenuExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddTrustedDialog = false },
            title = { Text("Add Trusted Contact") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = relation,
                        onValueChange = { relation = it },
                        label = { Text("Relationship / Tag") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenuBox(
                        expanded = ruleMenuExpanded,
                        onExpandedChange = { ruleMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedRule.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Handling Rule") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ruleMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = ruleMenuExpanded,
                            onDismissRequest = { ruleMenuExpanded = false }
                        ) {
                            ContactHandlingRule.entries.forEach { rule ->
                                DropdownMenuItem(
                                    text = { Text(rule.label) },
                                    onClick = {
                                        selectedRule = rule
                                        ruleMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveTrustedContact(name, phone, relation, selectedRule)
                        showAddTrustedDialog = false
                    },
                    enabled = name.isNotBlank() && phone.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTrustedDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showImportContactsDialog) {
        AlertDialog(
            onDismissRequest = { showImportContactsDialog = false },
            title = { Text("Import from Device Contacts") },
            text = {
                if (!uiState.systemStatus.hasContactsPermission) {
                    Text("Grant Contacts permission in Settings so AI Call Assistant can identify callers and import contacts.")
                } else if (uiState.deviceContacts.isEmpty()) {
                    Text("No contacts found on this device.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(uiState.deviceContacts) { entry ->
                            TextButton(
                                onClick = {
                                    onSaveTrustedContact(
                                        entry.name,
                                        entry.phoneNumber,
                                        "Saved Contact",
                                        ContactHandlingRule.GREET_FIRST
                                    )
                                    showImportContactsDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${entry.name} • ${entry.phoneNumber}")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportContactsDialog = false }) { Text("Close") }
            }
        )
    }

    if (showAddBlockDialog) {
        var phone by remember { mutableStateOf("") }
        var reason by remember { mutableStateOf("Spam / Telemarketing") }
        AlertDialog(
            onDismissRequest = { showAddBlockDialog = false },
            title = { Text("Block Number") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBlockNumber(phone, reason)
                        showAddBlockDialog = false
                    },
                    enabled = phone.isNotBlank()
                ) { Text("Block") }
            },
            dismissButton = {
                TextButton(onClick = { showAddBlockDialog = false }) { Text("Cancel") }
            }
        )
    }
}
