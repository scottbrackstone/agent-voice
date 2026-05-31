package com.agentvoice.app.viewmodel

import android.app.Application
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
    val connectionTestMessage: String? = null,
    val connectionTestSucceeded: Boolean? = null,
    val ttsEnabled: Boolean = true,
    val recentHistory: List<ConversationRecord> = emptyList(),
    val selectedHistory: ConversationRecord? = null,
    val backendUrl: String = SettingsRepository.DEFAULT_BACKEND_URL,
    val backendUrlDraft: String = SettingsRepository.DEFAULT_BACKEND_URL,
    val selectedAgent: ConnectorType = ConnectorType.Mock,
    val showSettings: Boolean = false,
    val lastTranscript: String? = null
) {
    val canRetry: Boolean
        get() = !isLoading && errorMessage != null && !lastTranscript.isNullOrBlank()

    val canSendTypedMessage: Boolean
        get() = !isLoading && !isListening && typedMessage.isNotBlank()

    val canSpeakAgain: Boolean
        get() = !isLoading && ttsEnabled && agentReply.isNotBlank()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application.applicationContext)
    private val conversationRepository = ConversationRepository(application.applicationContext)
    private val relayClient = RelayClient()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        backendUrl = settings.backendUrl,
                        backendUrlDraft = settings.backendUrl,
                        selectedAgent = settings.selectedAgent,
                        mode = settings.defaultMode,
                        ttsEnabled = settings.ttsEnabled
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
                errorMessage = null,
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

    private fun submitTranscript(
        transcript: String,
        speak: (String) -> Unit,
        clearTypedMessage: Boolean = false
    ) {
        val trimmedTranscript = transcript.trim()
        if (trimmedTranscript.isBlank()) {
            onSpeechError("I did not catch that. Try again.")
            return
        }

        val snapshot = _uiState.value
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
                connectionStatus = "Sending to relay"
            )
        }

        viewModelScope.launch {
            relayClient
                .sendMessage(
                    backendUrl = snapshot.backendUrl,
                    agent = snapshot.selectedAgent,
                    message = trimmedTranscript,
                    mode = snapshot.mode
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
                            mode = response.mode
                        )
                    }

                    if (_uiState.value.ttsEnabled) {
                        speak(response.reply)
                    }
                }
                .onFailure { error ->
                    val message = error.message ?: "Unable to reach AgentVoice relay."
                    conversationRepository.saveFailure(
                        transcript = trimmedTranscript,
                        mode = snapshot.mode,
                        connector = snapshot.selectedAgent,
                        error = message
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            connectionStatus = "Relay error",
                            errorMessage = message
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
                errorMessage = "Microphone permission is needed to use push-to-talk."
            )
        }
    }

    fun onSpeechError(message: String) {
        _uiState.update {
            it.copy(
                isListening = false,
                isLoading = false,
                connectionStatus = "Speech input error",
                errorMessage = message
            )
        }
    }

    fun setTtsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(ttsEnabled = enabled) }
        viewModelScope.launch {
            settingsRepository.setTtsEnabled(enabled)
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

    override fun onCleared() {
        relayClient.close()
        super.onCleared()
    }
}
