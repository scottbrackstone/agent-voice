package com.agentvoice.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agentvoice.app.model.AgentMode
import com.agentvoice.app.model.ConnectorType
import com.agentvoice.app.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onBackendUrlChange: (String) -> Unit,
    onSaveBackendUrl: () -> Unit,
    onTestConnection: () -> Unit,
    onSendMockTestPrompt: () -> Unit,
    onSendOpenClawTestPrompt: () -> Unit,
    onSendHermesTestPrompt: () -> Unit,
    onShowDrivingNotification: () -> Unit,
    onHideDrivingNotification: () -> Unit,
    onStartInDrivingModeToggle: (Boolean) -> Unit,
    onKeepScreenAwakeToggle: (Boolean) -> Unit,
    onDrivingAutoSpeakToggle: (Boolean) -> Unit,
    onDrivingRequireWakeWordToggle: (Boolean) -> Unit,
    onDrivingUseVoxtralTranscriptionToggle: (Boolean) -> Unit,
    onDrivingModeSelected: (AgentMode) -> Unit,
    onAgentSelected: (ConnectorType) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Backend URL", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = uiState.backendUrlDraft,
                    onValueChange = onBackendUrlChange,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onSaveBackendUrl,
                        enabled = !uiState.isTestingConnection,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Save")
                    }
                    OutlinedButton(
                        onClick = onTestConnection,
                        enabled = !uiState.isTestingConnection,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Test")
                    }
                }
                if (uiState.isTestingConnection) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                uiState.connectionTestMessage?.let { message ->
                    Text(
                        message,
                        color = when (uiState.connectionTestSucceeded) {
                            true -> MaterialTheme.colorScheme.primary
                            false -> MaterialTheme.colorScheme.error
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            ConnectorSelector(
                selectedAgent = uiState.selectedAgent,
                onAgentSelected = onAgentSelected
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Test prompts", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onSendHermesTestPrompt,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hermes")
                    }
                    OutlinedButton(
                        onClick = onSendMockTestPrompt,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mock")
                    }
                }
                OutlinedButton(
                    onClick = onSendOpenClawTestPrompt,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OpenClaw")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Driving shortcuts", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onShowDrivingNotification,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Show")
                    }
                    OutlinedButton(
                        onClick = onHideDrivingNotification,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hide")
                    }
                }
                uiState.shortcutStatusMessage?.let { message ->
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Driving behavior", fontWeight = FontWeight.SemiBold)
                SettingSwitch(
                    label = "Start in Driving mode",
                    checked = uiState.startInDrivingMode,
                    onCheckedChange = onStartInDrivingModeToggle
                )
                SettingSwitch(
                    label = "Keep screen awake",
                    checked = uiState.keepScreenAwakeInDrivingMode,
                    onCheckedChange = onKeepScreenAwakeToggle
                )
                SettingSwitch(
                    label = "Auto-read replies",
                    checked = uiState.drivingAutoSpeak,
                    onCheckedChange = onDrivingAutoSpeakToggle
                )
                SettingSwitch(
                    label = "Require trigger phrase",
                    checked = uiState.drivingRequireWakeWord,
                    onCheckedChange = onDrivingRequireWakeWordToggle
                )
                SettingSwitch(
                    label = "Use Voxtral transcription",
                    checked = uiState.drivingUseVoxtralTranscription,
                    onCheckedChange = onDrivingUseVoxtralTranscriptionToggle
                )
            }

            DrivingModeSelector(
                selectedMode = uiState.drivingMode,
                onDrivingModeSelected = onDrivingModeSelected
            )

            SettingValue("Default mode", uiState.mode.label)
            SettingValue("TTS", if (uiState.ttsEnabled) "enabled" else "disabled")
            SettingValue(
                "Driving STT",
                if (uiState.drivingUseVoxtralTranscription) "Voxtral" else "Android"
            )
            SettingValue("App version", uiState.appVersion)
            SettingValue("Last connector", uiState.lastConnector?.label ?: "none")
            SettingValue("Last request", uiState.lastRequestId ?: "none")
            SettingValue("Last error", uiState.lastErrorMessage ?: "none")
            SettingValue("Last speech error", uiState.lastSpeechErrorMessage ?: "none")
            SettingValue("Last relay error", uiState.lastRelayErrorMessage ?: "none")
            SettingValue("Last voice command", uiState.lastVoiceCommand ?: "none")
            SettingValue("Last ignored speech", uiState.lastIgnoredTranscript ?: "none")
            SettingValue("Hands-free recoveries", uiState.handsFreeRecoveryCount.toString())

            Button(
                onClick = onClearHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Clear local history")
            }

            uiState.errorMessage?.let { error ->
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DrivingModeSelector(
    selectedMode: AgentMode,
    onDrivingModeSelected: (AgentMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Driving mode default", fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                AgentMode.Mobile,
                AgentMode.CaptureOnly,
                AgentMode.ReviewRequired,
                AgentMode.Normal
            ).forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onDrivingModeSelected(mode) },
                    label = { Text(mode.label) }
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ConnectorSelector(
    selectedAgent: ConnectorType,
    onAgentSelected: (ConnectorType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Connector", fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(ConnectorType.Hermes, ConnectorType.Mock, ConnectorType.OpenClaw).forEach { agent ->
                FilterChip(
                    selected = selectedAgent == agent,
                    onClick = { onAgentSelected(agent) },
                    label = { Text(agent.label) }
                )
            }
        }
    }
}

@Composable
private fun SettingValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )
    }
}
