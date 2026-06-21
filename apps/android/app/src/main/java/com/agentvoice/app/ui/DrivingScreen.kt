package com.agentvoice.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agentvoice.app.model.ConversationRecord
import com.agentvoice.app.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrivingScreen(
    uiState: MainUiState,
    onTalkClick: () -> Unit,
    onRetryClick: () -> Unit,
    onSpeakAgainClick: () -> Unit,
    onStartHandsFreeClick: () -> Unit,
    onStopHandsFreeClick: () -> Unit,
    onInterruptClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.selectedAgent.label, fontWeight = FontWeight.SemiBold)
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
                    IconButton(onClick = onExitClick) {
                        Icon(Icons.Default.Close, contentDescription = "Close driving mode")
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DrivingStatusCard(uiState)

            uiState.handsFreeSessionSummary?.let { summary ->
                DrivingCard {
                    Text(
                        "Session summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HandsFreeSessionControls(
                uiState = uiState,
                onStartHandsFreeClick = onStartHandsFreeClick,
                onStopHandsFreeClick = onStopHandsFreeClick,
                onInterruptClick = onInterruptClick
            )

            Button(
                onClick = onTalkClick,
                enabled = !uiState.isLoading &&
                    !uiState.isListening &&
                    !uiState.isTranscribingVoiceClip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    when {
                        uiState.isRecordingVoiceClip -> "Stop recording"
                        uiState.isTranscribingVoiceClip -> "Transcribing"
                        uiState.isLoading -> "Thinking"
                        uiState.isListening -> "Listening"
                        else -> "Talk"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (
                uiState.isLoading ||
                uiState.isListening ||
                uiState.isRecordingVoiceClip ||
                uiState.isTranscribingVoiceClip
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            uiState.errorMessage?.let { error ->
                DrivingCard {
                    Text(
                        "Error",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(error, style = MaterialTheme.typography.bodyLarge)
                    if (uiState.canRetry) {
                        OutlinedButton(onClick = onRetryClick) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }

            DrivingCard {
                Text(
                    uiState.lastConnector?.let { "Reply from ${it.label}" } ?: "Reply",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    uiState.agentReply.ifBlank { "No reply yet." },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                    color = if (uiState.agentReply.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                OutlinedButton(
                    onClick = onSpeakAgainClick,
                    enabled = uiState.canSpeakAgain
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Speak again")
                }
            }

            if (uiState.queuedActions.isNotEmpty()) {
                DrivingCard {
                    Text(
                        "Queued for review",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    uiState.localQueueStatusMessage?.let { status ->
                        Text(
                            status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    uiState.queuedActions.take(3).forEach { action ->
                        Text(
                            "${action.summary} (${action.status.wireValue})",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            DrivingCard {
                Text(
                    "Last capture",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    uiState.transcript.ifBlank { "Nothing captured yet." },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = if (uiState.transcript.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            SessionMemoryCard(uiState)

            DrivingCommandsCard()

            RecentRepliesCard(
                records = uiState.recentHistory
                    .filter { it.reply.isNotBlank() }
                    .take(3)
            )
        }
    }
}

@Composable
private fun DrivingStatusCard(uiState: MainUiState) {
    val statusText = when {
        uiState.isSpeaking -> "Speaking"
        uiState.isRecordingVoiceClip -> "Recording"
        uiState.isTranscribingVoiceClip -> "Transcribing"
        uiState.isListening && uiState.drivingRequireWakeWord -> "Listening for trigger phrase"
        uiState.isListening -> "Listening now"
        uiState.isLoading -> "Thinking"
        uiState.isHandsFreeSessionActive -> "Hands-free session active"
        else -> "Ready for ${uiState.selectedAgent.label}"
    }

    DrivingCard {
        Text(
            statusText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
                Text(
                    when {
                        uiState.isSpeaking ->
                            "Tap Interrupt to speak now. Turns: ${uiState.handsFreeTurns}."
                        uiState.isRecordingVoiceClip && uiState.drivingRequireWakeWord ->
                            "Say the trigger phrase, then your request. Recording ends after a short silence."
                        uiState.isRecordingVoiceClip ->
                            "Speak naturally. Recording ends after a short silence."
                        uiState.isTranscribingVoiceClip ->
                            "Voxtral is transcribing your voice."
                        uiState.isListening && uiState.drivingRequireWakeWord ->
                            "Say the trigger phrase before your request."
                        uiState.isListening -> "Speak naturally."
                        uiState.isLoading -> "Waiting for ${uiState.selectedAgent.label}."
                        uiState.isHandsFreeSessionActive ->
                            "${uiState.handsFreeSessionStatus ?: "Ready"}. Turns: ${uiState.handsFreeTurns}."
                        uiState.drivingAutoSpeak -> "Replies will be read aloud. Mode: ${uiState.drivingMode.label}."
                        else -> "Auto-read replies is off. Mode: ${uiState.drivingMode.label}."
                    },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Activation: ${
                if (uiState.drivingRequireWakeWord) {
                    "say trigger phrase first"
                } else {
                    "always listen in session"
                }
            }${
                if (uiState.drivingUseVoxtralTranscription) {
                    ". STT: Voxtral"
                } else {
                    ". STT: Android"
                }
            }",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HandsFreeSessionControls(
    uiState: MainUiState,
    onStartHandsFreeClick: () -> Unit,
    onStopHandsFreeClick: () -> Unit,
    onInterruptClick: () -> Unit
) {
    if (uiState.isHandsFreeSessionActive && uiState.isSpeaking) {
        Button(
            onClick = onInterruptClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Mic, contentDescription = null)
            Spacer(Modifier.size(10.dp))
            Text(
                "Interrupt",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        OutlinedButton(
            onClick = onStopHandsFreeClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Stop, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Stop hands-free")
        }
    } else if (uiState.isHandsFreeSessionActive) {
        Button(
            onClick = onStopHandsFreeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Stop, contentDescription = null)
            Spacer(Modifier.size(10.dp))
            Text(
                "Stop hands-free",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    } else {
        OutlinedButton(
            onClick = onStartHandsFreeClick,
            enabled = !uiState.isLoading && !uiState.isListening,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.size(10.dp))
            Text(
                "Start hands-free",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SessionMemoryCard(uiState: MainUiState) {
    if (
        uiState.recentHandsFreeCaptures.isEmpty() &&
        uiState.recentIgnoredUtterances.isEmpty() &&
        uiState.lastVoiceCommand.isNullOrBlank()
    ) {
        return
    }

    DrivingCard {
        Text(
            "Session memory",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        uiState.lastVoiceCommand?.let { command ->
            Text(
                "Command: $command",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (uiState.recentHandsFreeCaptures.isNotEmpty()) {
            Text(
                "Captured",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            uiState.recentHandsFreeCaptures.take(3).forEach { capture ->
                Text(
                    capture,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (uiState.recentIgnoredUtterances.isNotEmpty()) {
            Text(
                "Ignored",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            uiState.recentIgnoredUtterances.take(2).forEach { ignored ->
                Text(
                    ignored,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DrivingCommandsCard() {
    DrivingCard {
        Text(
            "Voice commands",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        listOf(
            "trigger phrase, capture this...",
            "trigger phrase, read queued actions",
            "trigger phrase, confirm",
            "trigger phrase, cancel",
            "trigger phrase, repeat that",
            "trigger phrase, review mode",
            "stop hands free"
        ).forEach { command ->
            Text(
                command,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecentRepliesCard(records: List<ConversationRecord>) {
    DrivingCard {
        Text(
            "Recent replies",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (records.isEmpty()) {
            Text(
                "No recent replies yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            records.forEach { record ->
                Text(
                    record.reply,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DrivingCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}
