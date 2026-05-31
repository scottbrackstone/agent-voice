package com.agentvoice.app.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agentvoice.app.model.AgentMode
import com.agentvoice.app.model.ConnectorType
import com.agentvoice.app.model.ConversationRecord
import com.agentvoice.app.model.QueuedAction
import com.agentvoice.app.network.RelayClient
import com.agentvoice.app.storage.ConversationRepository
import com.agentvoice.app.storage.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val connectionStatus: String = "Relay not connected",
    val mode: AgentMode = AgentMode.Normal,
    val isListening: Boolean = false,
    val isLoading: Boolean = false,
    val isTestingConnection: Boolean = false,
    val transcript: String = "",
    val agentReply: String = "",
    val typedMessage: String = "",
    val queuedActions: List<QueuedAction> = emptyList(),
    val warnings: List<String> = emptyList(),
    val errorMessage: String? = null,
    val lastRequestId: String? = null,
    val lastConnector: ConnectorType? = null,
    val lastErrorMessage: String? = null,
    val connectionTestMessage: String? = null,
    val connectionTestSucceeded: Boolean? = null,
    val shortcutStatusMessage: String? = null,
    val ttsEnabled: Boolean = true,
    val startInDrivingMode: Boolean = false,
    val keepScreenAwakeInDrivingMode: Boolean = true,
    val drivingAutoSpeak: Boolean = true,
    val drivingMode: AgentMode = AgentMode.Mobile,
    val isHandsFreeSessionActive: Boolean = false,
    val isSpeaking: Boolean = false,
    val handsFreeTurns: Int = 0,
    val handsFreeSessionStatus: String? = null,
    val recentHistory: List<ConversationRecord> = emptyList(),
    val selectedHistory: ConversationRecord? = null,
    val backendUrl: String = SettingsRepository.DEFAULT_BACKEND_URL,
    val backendUrlDraft: String = SettingsRepository.DEFAULT_BACKEND_URL,
    val selectedAgent: ConnectorType = ConnectorType.Mock,
    val showSettings: Boolean = false,
    val isDrivingMode: Boolean = false,
    val appVersion: String = "unknown",
    val lastTranscript: String? = null
) {
    val canRetry: Boolean
        get() = !isLoading && errorMessage != null && !lastTranscript.isNullOrBlank()

    val canSendTypedMessage: Boolean
        get() = !isLoading && !isListening && typedMessage.isNotBlank()

    val canSpeakAgain: Boolean
        get() = !isLoading &&
            agentReply.isNotBlank() &&
            (ttsEnabled || (isDrivingMode && drivingAutoSpeak))
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application.applicationContext)
    private val conversationRepository = ConversationRepository(application.applicationContext)
    private val relayClient = RelayClient()
    private var appliedInitialDrivingMode = false

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        _uiState.update { it.copy(appVersion = resolveAppVersion(application)) }

        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                val shouldOpenDrivingMode =
                    !appliedInitialDrivingMode && settings.startInDrivingMode
                appliedInitialDrivingMode = true

                _uiState.update {
                    it.copy(
                        backendUrl = settings.backendUrl,
                        backendUrlDraft = settings.backendUrl,
                        selectedAgent = settings.selectedAgent,
                        mode = settings.defaultMode,
                        ttsEnabled = settings.ttsEnabled,
                        startInDrivingMode = settings.startInDrivingMode,
                        keepScreenAwakeInDrivingMode = settings.keepScreenAwakeInDrivingMode,
                        drivingAutoSpeak = settings.drivingAutoSpeak,
                        drivingMode = settings.drivingMode,
                        isDrivingMode = it.isDrivingMode || shouldOpenDrivingMode
                    )
                }
            }
        }

        viewModelScope.launch {
            conversationRepository.recentConversations.collect { conversations ->
                _uiState.update {
                    it.copy(
                        recentHistory = conversations,
                        selectedHistory = it.selectedHistory?.let { selected ->
                            conversations.firstOrNull { record -> record.id == selected.id }
                        }
                    )
                }
            }
        }
    }

    fun selectMode(mode: AgentMode) {
        _uiState.update { it.copy(mode = mode) }
        viewModelScope.launch {
            settingsRepository.setDefaultMode(mode)
        }
    }

    fun onListeningStarted() {
        _uiState.update {
            it.copy(
                isListening = true,
                isSpeaking = false,
                errorMessage = null,
                handsFreeSessionStatus = if (it.isHandsFreeSessionActive) {
                    "Listening"
                } else {
                    it.handsFreeSessionStatus
                },
                connectionStatus = "Listening"
            )
        }
    }

    fun onListeningFinished() {
        _uiState.update {
            it.copy(
                isListening = false,
                connectionStatus = if (it.isLoading) "Sending to relay" else "Relay ready"
            )
        }
    }

    fun onTranscriptCaptured(transcript: String, speak: (String) -> Unit) {
        submitTranscript(transcript, speak)
    }

    fun updateTypedMessage(message: String) {
        _uiState.update { it.copy(typedMessage = message, errorMessage = null) }
    }

    fun sendTypedMessage(speak: (String) -> Unit) {
        submitTranscript(_uiState.value.typedMessage, speak, clearTypedMessage = true)
    }

    fun sendTestPrompt(agent: ConnectorType, speak: (String) -> Unit) {
        submitTranscript(
            transcript = "Reply with one short sentence confirming AgentVoice can reach you.",
            speak = speak,
            agentOverride = agent,
            modeOverride = AgentMode.Normal
        )
    }

    private fun submitTranscript(
        transcript: String,
        speak: (String) -> Unit,
        clearTypedMessage: Boolean = false,
        agentOverride: ConnectorType? = null,
        modeOverride: AgentMode? = null
    ) {
        val trimmedTranscript = transcript.trim()
        if (trimmedTranscript.isBlank()) {
            onSpeechError("I did not catch that. Try again.")
            return
        }

        val snapshot = _uiState.value
        val agent = agentOverride ?: snapshot.selectedAgent
        val mode = modeOverride ?: if (snapshot.isDrivingMode) {
            snapshot.drivingMode
        } else {
            snapshot.mode
        }
        _uiState.update {
            it.copy(
                isListening = false,
                isLoading = true,
                transcript = trimmedTranscript,
                agentReply = "",
                queuedActions = emptyList(),
                warnings = emptyList(),
                errorMessage = null,
                lastTranscript = trimmedTranscript,
                typedMessage = if (clearTypedMessage) "" else it.typedMessage,
                lastErrorMessage = null,
                connectionStatus = "Sending to relay"
            )
        }

        viewModelScope.launch {
            relayClient
                .sendMessage(
                    backendUrl = snapshot.backendUrl,
                    agent = agent,
                    message = trimmedTranscript,
                    mode = mode
                )
                .onSuccess { response ->
                    conversationRepository.saveResponse(trimmedTranscript, response)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            connectionStatus = "Relay replied via ${response.connector.label}",
                            agentReply = response.reply,
                            queuedActions = response.queuedActions,
                            warnings = response.warnings,
                            errorMessage = null,
                            lastRequestId = response.requestId,
                            lastConnector = response.connector,
                            lastErrorMessage = null,
                            handsFreeTurns = if (it.isHandsFreeSessionActive) {
                                it.handsFreeTurns + 1
                            } else {
                                it.handsFreeTurns
                            },
                            handsFreeSessionStatus = if (it.isHandsFreeSessionActive) {
                                "Reply received"
                            } else {
                                it.handsFreeSessionStatus
                            },
                            mode = response.mode
                        )
                    }

                    val latestState = _uiState.value
                    if (
                        latestState.ttsEnabled ||
                        (latestState.isDrivingMode && latestState.drivingAutoSpeak) ||
                        latestState.isHandsFreeSessionActive
                    ) {
                        speak(response.reply)
                    }
                }
                .onFailure { error ->
                    val message = error.message ?: "Unable to reach AgentVoice relay."
                    conversationRepository.saveFailure(
                        transcript = trimmedTranscript,
                        mode = mode,
                        connector = agent,
                        error = message
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            connectionStatus = "Relay error",
                            errorMessage = message,
                            lastErrorMessage = message,
                            isHandsFreeSessionActive = false,
                            handsFreeSessionStatus = if (it.isHandsFreeSessionActive) {
                                "Stopped after relay error"
                            } else {
                                it.handsFreeSessionStatus
                            }
                        )
                    }
                }
        }
    }

    fun retryLastMessage(speak: (String) -> Unit) {
        val transcript = _uiState.value.lastTranscript ?: return
        onTranscriptCaptured(transcript, speak)
    }

    fun onPermissionDenied() {
        _uiState.update {
            it.copy(
                isListening = false,
                isLoading = false,
                connectionStatus = "Microphone permission denied",
                errorMessage = "Microphone permission is needed to use push-to-talk.",
                isHandsFreeSessionActive = false,
                handsFreeSessionStatus = if (it.isHandsFreeSessionActive) {
                    "Stopped after permission denial"
                } else {
                    it.handsFreeSessionStatus
                }
            )
        }
    }

    fun onSpeechError(message: String) {
        _uiState.update {
            it.copy(
                isListening = false,
                isLoading = false,
                connectionStatus = "Speech input error",
                errorMessage = message,
                isHandsFreeSessionActive = false,
                handsFreeSessionStatus = if (it.isHandsFreeSessionActive) {
                    "Stopped after speech error"
                } else {
                    it.handsFreeSessionStatus
                }
            )
        }
    }

    fun setTtsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(ttsEnabled = enabled) }
        viewModelScope.launch {
            settingsRepository.setTtsEnabled(enabled)
        }
    }

    fun setStartInDrivingMode(enabled: Boolean) {
        _uiState.update { it.copy(startInDrivingMode = enabled) }
        viewModelScope.launch {
            settingsRepository.setStartInDrivingMode(enabled)
        }
    }

    fun setKeepScreenAwakeInDrivingMode(enabled: Boolean) {
        _uiState.update { it.copy(keepScreenAwakeInDrivingMode = enabled) }
        viewModelScope.launch {
            settingsRepository.setKeepScreenAwakeInDrivingMode(enabled)
        }
    }

    fun setDrivingAutoSpeak(enabled: Boolean) {
        _uiState.update { it.copy(drivingAutoSpeak = enabled) }
        viewModelScope.launch {
            settingsRepository.setDrivingAutoSpeak(enabled)
        }
    }

    fun setDrivingMode(mode: AgentMode) {
        _uiState.update { it.copy(drivingMode = mode) }
        viewModelScope.launch {
            settingsRepository.setDrivingMode(mode)
        }
    }

    fun updateBackendUrlDraft(url: String) {
        _uiState.update {
            it.copy(
                backendUrlDraft = url,
                errorMessage = null,
                connectionTestMessage = null,
                connectionTestSucceeded = null
            )
        }
    }

    fun saveBackendUrl() {
        val url = _uiState.value.backendUrlDraft.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Backend URL cannot be empty.") }
            return
        }

        viewModelScope.launch {
            settingsRepository.setBackendUrl(url)
            _uiState.update {
                it.copy(
                    backendUrl = url,
                    backendUrlDraft = url,
                    connectionStatus = "Relay URL saved",
                    errorMessage = null,
                    connectionTestMessage = null,
                    connectionTestSucceeded = null
                )
            }
        }
    }

    fun testConnection() {
        val url = _uiState.value.backendUrlDraft.trim()
        if (url.isBlank()) {
            _uiState.update {
                it.copy(
                    connectionTestMessage = "Backend URL cannot be empty.",
                    connectionTestSucceeded = false,
                    errorMessage = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isTestingConnection = true,
                connectionStatus = "Checking relay",
                connectionTestMessage = "Checking AgentVoice relay...",
                connectionTestSucceeded = null,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            relayClient
                .testConnection(url)
                .onSuccess { message ->
                    _uiState.update {
                        it.copy(
                            isTestingConnection = false,
                            connectionStatus = "Relay reachable",
                            connectionTestMessage = message,
                            connectionTestSucceeded = true,
                            shortcutStatusMessage = null,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isTestingConnection = false,
                            connectionStatus = "Relay test failed",
                            connectionTestMessage = error.message
                                ?: "Unable to reach AgentVoice relay.",
                            connectionTestSucceeded = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun selectAgent(agent: ConnectorType) {
        _uiState.update { it.copy(selectedAgent = agent) }
        viewModelScope.launch {
            settingsRepository.setSelectedAgent(agent)
        }
    }

    fun selectHistory(record: ConversationRecord) {
        _uiState.update { it.copy(selectedHistory = record) }
    }

    fun dismissHistoryDetail() {
        _uiState.update { it.copy(selectedHistory = null) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            conversationRepository.clearHistory()
            _uiState.update {
                it.copy(
                    selectedHistory = null,
                    connectionStatus = "Local history cleared",
                    errorMessage = null
                )
            }
        }
    }

    fun openSettings() {
        _uiState.update { it.copy(showSettings = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(showSettings = false) }
    }

    fun openDrivingMode() {
        _uiState.update { it.copy(isDrivingMode = true, showSettings = false) }
    }

    fun closeDrivingMode() {
        _uiState.update {
            it.copy(
                isDrivingMode = false,
                isHandsFreeSessionActive = false,
                handsFreeSessionStatus = if (it.isHandsFreeSessionActive) {
                    "Stopped"
                } else {
                    it.handsFreeSessionStatus
                }
            )
        }
    }

    fun startHandsFreeSession() {
        _uiState.update {
            it.copy(
                isDrivingMode = true,
                showSettings = false,
                isHandsFreeSessionActive = true,
                handsFreeTurns = 0,
                handsFreeSessionStatus = "Starting"
            )
        }
    }

    fun stopHandsFreeSession(reason: String = "Stopped") {
        _uiState.update {
            it.copy(
                isHandsFreeSessionActive = false,
                isListening = false,
                isSpeaking = false,
                handsFreeSessionStatus = reason
            )
        }
    }

    fun onSpeakingStarted() {
        _uiState.update {
            it.copy(
                isSpeaking = true,
                handsFreeSessionStatus = if (it.isHandsFreeSessionActive) {
                    "Jynx is speaking"
                } else {
                    it.handsFreeSessionStatus
                }
            )
        }
    }

    fun onSpeakingFinished() {
        _uiState.update {
            it.copy(
                isSpeaking = false,
                handsFreeSessionStatus = if (it.isHandsFreeSessionActive) {
                    "Preparing to listen"
                } else {
                    it.handsFreeSessionStatus
                }
            )
        }
    }

    fun onHandsFreeWaitingForNextTurn() {
        _uiState.update {
            if (it.isHandsFreeSessionActive) {
                it.copy(handsFreeSessionStatus = "Ready for next turn")
            } else {
                it
            }
        }
    }

    fun onShortcutNotificationShown() {
        _uiState.update {
            it.copy(shortcutStatusMessage = "Driving shortcut notification is on.")
        }
    }

    fun onShortcutNotificationHidden() {
        _uiState.update {
            it.copy(shortcutStatusMessage = "Driving shortcut notification is off.")
        }
    }

    fun onNotificationPermissionDenied() {
        _uiState.update {
            it.copy(shortcutStatusMessage = "Notification permission is needed for the driving shortcut.")
        }
    }

    override fun onCleared() {
        relayClient.close()
        super.onCleared()
    }

    private fun resolveAppVersion(application: Application): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                application.packageManager.getPackageInfo(
                    application.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                application.packageManager.getPackageInfo(application.packageName, 0)
            }

            packageInfo.versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }
}
