package com.agentvoice.app.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agentvoice.app.model.AgentMode
import com.agentvoice.app.model.ConversationRecord
import com.agentvoice.app.model.QueuedAction
import com.agentvoice.app.viewmodel.MainUiState
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onModeSelected: (AgentMode) -> Unit,
    onTalkClick: () -> Unit,
    onRetryClick: () -> Unit,
    onHistorySelected: (ConversationRecord) -> Unit,
    onDismissHistory: () -> Unit,
    onTtsToggle: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AgentVoice", fontWeight = FontWeight.SemiBold)
                        Text(
                            uiState.connectionStatus,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModeSelector(
                selectedMode = uiState.mode,
                onModeSelected = onModeSelected
            )

            if (uiState.mode.showsCaptureNotice) {
                InfoCard(
                    title = "Safety mode",
                    body = "Capture now. Confirm later."
                )
            }

            Button(
                onClick = onTalkClick,
                enabled = !uiState.isLoading && !uiState.isListening,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    when {
                        uiState.isLoading -> "Sending..."
                        uiState.isListening -> "Listening..."
                        else -> "Talk"
                    }
                )
            }

            SettingRow(
                label = "TTS enabled",
                checked = uiState.ttsEnabled,
                onCheckedChange = onTtsToggle
            )

            uiState.errorMessage?.let { error ->
                InfoCard(
                    title = "Error",
                    body = error
                )
            }

            if (uiState.canRetry) {
                Button(
                    onClick = onRetryClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Retry")
                }
            }

            InfoCard(
                title = "Transcript",
                body = uiState.transcript.ifBlank { "No transcript yet." }
            )

            InfoCard(
                title = "Agent response",
                body = uiState.agentReply.ifBlank { "No response yet." }
            )

            QueuedActionsCard(uiState.queuedActions)

            if (uiState.warnings.isNotEmpty()) {
                InfoCard(
                    title = "Warnings",
                    body = uiState.warnings.joinToString(separator = "\n")
                )
            }

            RecentHistoryCard(
                records = uiState.recentHistory,
                onHistorySelected = onHistorySelected
            )

            uiState.selectedHistory?.let { selected ->
                HistoryDetailCard(
                    record = selected,
                    onDismiss = onDismissHistory
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(
    selectedMode: AgentMode,
    onModeSelected: (AgentMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Mode",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AgentMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == selectedMode,
                    onClick = { onModeSelected(mode) },
                    label = { Text(mode.label) }
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun QueuedActionsCard(actions: List<QueuedAction>) {
    val body = if (actions.isEmpty()) {
        "No queued actions."
    } else {
        actions.joinToString(separator = "\n") { action ->
            "${action.type.label}: ${action.summary}"
        }
    }

    InfoCard(title = "Queued actions", body = body)
}

@Composable
private fun RecentHistoryCard(
    records: List<ConversationRecord>,
    onHistorySelected: (ConversationRecord) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Recent history",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (records.isEmpty()) {
                Text("No local history yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                records.take(6).forEach { record ->
                    TextButton(
                        onClick = { onHistorySelected(record) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                "${formatTimestamp(record.timestampMillis)} - ${record.mode.label} - ${record.status.wireValue}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                record.transcript,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryDetailCard(
    record: ConversationRecord,
    onDismiss: () -> Unit
) {
    val body = buildString {
        appendLine("Transcript: ${record.transcript}")
        appendLine("Reply: ${record.reply.ifBlank { "No reply saved." }}")
        appendLine("Mode: ${record.mode.label}")
        appendLine("Connector: ${record.connector.label}")
        appendLine("Status: ${record.status.wireValue}")
        if (record.queuedActionsSummary.isNotBlank()) {
            appendLine("Queued actions: ${record.queuedActionsSummary}")
        }
        if (!record.error.isNullOrBlank()) {
            appendLine("Error: ${record.error}")
        }
    }.trim()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "History detail",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(body, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    }
}

private fun formatTimestamp(timestampMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        .format(Date(timestampMillis))
