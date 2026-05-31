package com.agentvoice.app.ui

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
    onTypedMessageChange: (String) -> Unit,
    onSendTypedMessage: () -> Unit,
    onRetryClick: () -> Unit,
    onSpeakAgainClick: () -> Unit,
    onHistorySelected: (ConversationRecord) -> Unit,
    onDismissHistory: () -> Unit,
    onTtsToggle: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

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

            TypedMessageCard(
                message = uiState.typedMessage,
                canSend = uiState.canSendTypedMessage,
                enabled = !uiState.isLoading && !uiState.isListening,
                onMessageChange = onTypedMessageChange,
                onSend = onSendTypedMessage
            )

            SettingRow(
                label = "TTS enabled",
                checked = uiState.ttsEnabled,
                onCheckedChange = onTtsToggle
            )

            if (uiState.isLoading || uiState.isListening) {
                LoadingCard(
                    title = if (uiState.isListening) "Listening" else "Sending",
                    body = if (uiState.isListening) {
                        "Waiting for speech input."
                    } else {
                        "Waiting for AgentVoice relay."
                    }
                )
            }

            uiState.errorMessage?.let { error ->
                ErrorCard(
                    body = error,
                    canRetry = uiState.canRetry,
                    onRetry = onRetryClick
                )
            }

            MessageCard(
                title = "Transcript",
                body = uiState.transcript,
                emptyBody = "No transcript yet.",
                onCopy = {
                    copyToClipboard(context, clipboardManager, "Transcript", uiState.transcript)
                }
            )

            MessageCard(
                title = "Agent response",
                body = uiState.agentReply,
                emptyBody = "No response yet.",
                onCopy = {
                    copyToClipboard(context, clipboardManager, "Reply", uiState.agentReply)
                },
                onSpeakAgain = onSpeakAgainClick,
                canSpeakAgain = uiState.canSpeakAgain
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
                    onDismiss = onDismissHistory,
                    onCopyTranscript = {
                        copyToClipboard(context, clipboardManager, "Transcript", selected.transcript)
                    },
                    onCopyReply = {
                        copyToClipboard(context, clipboardManager, "Reply", selected.reply)
                    }
                )
            }
        }
    }
}

@Composable
private fun TypedMessageCard(
    message: String,
    canSend: Boolean,
    enabled: Boolean,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit
) {
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
                "Typed message",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                enabled = enabled,
                placeholder = { Text("Type a command") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Send")
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
private fun LoadingCard(title: String, body: String) {
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
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(body, style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ErrorCard(
    body: String,
    canRetry: Boolean,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Error",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (canRetry) {
                OutlinedButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Retry")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageCard(
    title: String,
    body: String,
    emptyBody: String,
    onCopy: () -> Unit,
    onSpeakAgain: (() -> Unit)? = null,
    canSpeakAgain: Boolean = false
) {
    val hasBody = body.isNotBlank()
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
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                body.ifBlank { emptyBody },
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasBody) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopy,
                    enabled = hasBody
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Copy")
                }
                if (onSpeakAgain != null) {
                    OutlinedButton(
                        onClick = onSpeakAgain,
                        enabled = canSpeakAgain
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Speak again")
                    }
                }
            }
        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryDetailCard(
    record: ConversationRecord,
    onDismiss: () -> Unit,
    onCopyTranscript: () -> Unit,
    onCopyReply: () -> Unit
) {
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
            DetailLine("When", formatTimestamp(record.timestampMillis))
            DetailLine("Mode", record.mode.label)
            DetailLine("Connector", record.connector.label)
            DetailLine("Status", record.status.wireValue)
            DetailBlock("Transcript", record.transcript)
            DetailBlock("Reply", record.reply.ifBlank { "No reply saved." })
            if (record.queuedActionsSummary.isNotBlank()) {
                DetailBlock("Queued actions", record.queuedActionsSummary)
            }
            if (!record.error.isNullOrBlank()) {
                DetailBlock("Error", record.error)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onCopyTranscript) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Copy transcript")
                }
                OutlinedButton(
                    onClick = onCopyReply,
                    enabled = record.reply.isNotBlank()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Copy reply")
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailBlock(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatTimestamp(timestampMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        .format(Date(timestampMillis))

private fun copyToClipboard(
    context: Context,
    clipboardManager: ClipboardManager,
    label: String,
    text: String
) {
    if (text.isBlank()) {
        return
    }

    clipboardManager.setText(AnnotatedString(text))
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}
