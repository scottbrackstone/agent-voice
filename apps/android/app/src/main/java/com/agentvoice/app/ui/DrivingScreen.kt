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
                        Text("Jynx", fontWeight = FontWeight.SemiBold)
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

            HandsFreeSessionControls(
                uiState = uiState,
                onStartHandsFreeClick = onStartHandsFreeClick,
                onStopHandsFreeClick = onStopHandsFreeClick,
                onInterruptClick = onInterruptClick
            )

            Button(
                onClick = onTalkClick,
                enabled = !uiState.isLoading && !uiState.isListening,
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
                        uiState.isLoading -> "Sending"
                        uiState.isListening -> "Listening"
                        else -> "Talk"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (uiState.isLoading || uiState.isListening) {
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
        uiState.isHandsFreeSessionActive -> "Hands-free session active"
        uiState.isListening -> "Listening now"
        uiState.isLoading -> "Sending to ${uiState.selectedAgent.label}"
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
                        uiState.isHandsFreeSessionActive ->
                            if (uiState.isSpeaking) {
                                "Tap Interrupt to speak now. Turns: ${uiState.handsFreeTurns}."
                            } else {
                                "${uiState.handsFreeSessionStatus ?: "Ready"}. Turns: ${uiState.handsFreeTurns}."
                            }
                        uiState.isListening -> "Speak your command."
                        uiState.isLoading -> "Waiting for the relay."
                        uiState.drivingAutoSpeak -> "Replies will be read aloud. Mode: ${uiState.drivingMode.label}."
                        else -> "Auto-read replies is off. Mode: ${uiState.drivingMode.label}."
                    },
            style = MaterialTheme.typography.bodyLarge,
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
