package com.agentvoice.app.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agentvoice.app.model.AgentMode
import com.agentvoice.app.model.ConnectorType
import com.agentvoice.app.model.ConversationRecord
import com.agentvoice.app.model.QueuedAction
import com.agentvoice.app.model.QueuedActionStatus
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
    val drivingRequireWakeWord: Boolean = false,
    val drivingUseVoxtralTranscription: Boolean = false,
    val drivingMode: AgentMode = AgentMode.Mobile,
    val isRecordingVoiceClip: Boolean = false,
    val isTranscribingVoiceClip: Boolean = false,
    val isHandsFreeSessionActive: Boolean = false,
    val isSpeaking: Boolean = false,
    val handsFreeTurns: Int = 0,
    val handsFreeSessionStatus: String? = null,
    val handsFreeRecoveryCount: Int = 0,
    val handsFreeRestartSignal: Int = 0,
    val handsFreeSessionQueuedActions: Int = 0,
    val handsFreeSessionConfirmedActions: Int = 0,
    val handsFreeSessionCancelledActions: Int = 0,
    val handsFreeSessionIgnoredUtterances: Int = 0,
    val handsFreeSessionSummary: String? = null,
    val recentHandsFreeCaptures: List<String> = emptyList(),
    val recentIgnoredUtterances: List<String> = emptyList(),
    val localQueueStatusMessage: String? = null,
    val lastSpeechErrorMessage: String? = null,
    val lastRelayErrorMessage: String? = null,
    val lastVoiceCommand: String? = null,
    val lastIgnoredTranscript: String? = null,
    val recentHistory: List<ConversationRecord> = emptyList(),
    val selectedHistory: ConversationRecord? = null,
    val backendUrl: String = SettingsRepository.DEFAULT_BACKEND_URL,
    val backendUrlDraft: String = SettingsRepository.DEFAULT_BACKEND_URL,
    val selectedAgent: ConnectorType = SettingsRepository.DEFAULT_SELECTED_AGENT,
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
    private var allowNextWakeGatedUtterance = false

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        _uiState.update { it.copy(appVersion = resolveAppVersion(application)) }

        viewModelScope.launch {
            settingsRepository.migrateDefaultAgentToHermes()
        }

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
                        drivingRequireWakeWord = settings.drivingRequireWakeWord,
                        drivingUseVoxtralTranscription = settings.drivingUseVoxtralTranscription,
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
                    if (it.drivingRequireWakeWord && !allowNextWakeGatedUtterance) {
                        "Listening for trigger phrase"
                    } else {
                        "Listening"
                    }
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
        if (handleImmediateHandsFreeVoiceCommand(transcript, speak)) {
            return
        }

        val preparedTranscript = prepareHandsFreeTranscript(transcript) ?: return
        if (handleHandsFreeVoiceCommand(preparedTranscript.transcript, speak)) {
            return
        }

        submitTranscript(
            transcript = preparedTranscript.transcript,
            speak = speak,
            modeOverride = preparedTranscript.modeOverride
        )
    }

    fun onVoiceClipRecordingStarted() {
        _uiState.update {
            it.copy(
                isRecordingVoiceClip = true,
                isTranscribingVoiceClip = false,
                isListening = false,
                errorMessage = null,
                connectionStatus = if (it.isHandsFreeSessionActive) {
                    "Recording"
                } else {
                    "Recording voice"
                },
                handsFreeSessionStatus = if (it.isHandsFreeSessionActive) {
                    if (it.drivingRequireWakeWord) {
                        "Recording. Say the trigger phrase, then your request"
                    } else {
                        "Recording"
                    }
                } else {
                    it.handsFreeSessionStatus
                }
            )
        }
    }

    fun onVoiceClipTranscribing() {
        _uiState.update {
            it.copy(
                isRecordingVoiceClip = false,
                isTranscribingVoiceClip = true,
                connectionStatus = "Transcribing",
                handsFreeSessionStatus = if (it.isHandsFreeSessionActive) {
                    "Transcribing"
                } else {
                    it.handsFreeSessionStatus
                }
            )
        }
    }

    fun onVoiceClipTranscribed(transcript: String, speak: (String) -> Unit) {
        _uiState.update { it.copy(isTranscribingVoiceClip = false) }
        onTranscriptCaptured(transcript, speak)
    }

    fun transcribeVoiceClip(audioBytes: ByteArray, speak: (String) -> Unit) {
        val backendUrl = _uiState.value.backendUrl
        onVoiceClipTranscribing()

        viewModelScope.launch {
            relayClient
                .transcribeAudio(
                    backendUrl = backendUrl,
                    audioBytes = audioBytes
                )
                .onSuccess { transcript ->
                    if (transcript.isBlank()) {
                        onVoiceClipError("I did not catch that. Try again.")
                    } else {
                        onVoiceClipTranscribed(transcript, speak)
                    }
                }
                .onFailure { error ->
                    onVoiceClipError(error.message ?: "Unable to transcribe voice.")
                }
        }
    }

    fun onVoiceClipError(message: String) {
        _uiState.update {
            it.copy(
                isRecordingVoiceClip = false,
                isTranscribingVoiceClip = false,
                connectionStatus = if (it.isHandsFreeSessionActive) "Listening again" else "Voice error",
                errorMessage = message,
                lastSpeechErrorMessage = message,
                handsFreeRecoveryCount = if (it.isHandsFreeSessionActive) {
                    it.handsFreeRecoveryCount + 1
                } else {
                    it.handsFreeRecoveryCount
                },
                handsFreeRestartSignal = if (it.isHandsFreeSessionActive) {
                    it.handsFreeRestartSignal + 1
                } else {
                    it.handsFreeRestartSignal
                },
                handsFreeSessionStatus = if (it.isHandsFreeSessionActive) {
                    "Listening again"
                } else {
                    it.handsFreeSessionStatus
                }
            )
        }
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
                lastRelayErrorMessage = null,
                localQueueStatusMessage = null,
                connectionStatus = if (it.isHandsFreeSessionActive) "Thinking" else "Sending to relay",
                handsFreeSessionStatus = if (it.isHandsFreeSessionActive) {
                    "Thinking"
                } else {
                    it.handsFreeSessionStatus
                }
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
                            handsFreeSessionQueuedActions = if (it.isHandsFreeSessionActive) {
                                it.handsFreeSessionQueuedActions + response.queuedActions.size
                            } else {
                                it.handsFreeSessionQueuedActions
                            },
                            recentHandsFreeCaptures = if (it.isHandsFreeSessionActive) {
                                appendRecent(trimmedTranscript, it.recentHandsFreeCaptures)
                            } else {
                                it.recentHandsFreeCaptures
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
                            connectionStatus = if (it.isHandsFreeSessionActive) {
                                "Recovering"
                            } else {
                                "Relay error"
                            },
                            errorMessage = message,
                            lastErrorMessage = message,
                            lastRelayErrorMessage = message,
                            isHandsFreeSessionActive = it.isHandsFreeSessionActive,
                            handsFreeRecoveryCount = if (it.isHandsFreeSessionActive) {
                                it.handsFreeRecoveryCount + 1
                            } else {
                                it.handsFreeRecoveryCount
                            },
                            handsFreeRestartSignal = if (it.isHandsFreeSessionActive) {
                                it.handsFreeRestartSignal + 1
                            } else {
                                it.handsFreeRestartSignal
                            },
                            handsFreeSessionStatus = if (it.isHandsFreeSessionActive) {
                                "Relay timed out. Listening again"
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
                lastSpeechErrorMessage = "Microphone permission is needed to use push-to-talk.",
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
                lastSpeechErrorMessage = message,
                isHandsFreeSessionActive = false,
                handsFreeSessionStatus = if (it.isHandsFreeSessionActive) {
                    "Stopped after speech error"
                } else {
                    it.handsFreeSessionStatus
                }
            )
        }
    }

    fun onHandsFreeRecoverableError(message: String) {
        _uiState.update {
            it.copy(
                isListening = false,
                isLoading = false,
                connectionStatus = "Listening again",
                errorMessage = message,
                lastErrorMessage = message,
                lastSpeechErrorMessage = message,
                handsFreeRecoveryCount = it.handsFreeRecoveryCount + 1,
                handsFreeRestartSignal = it.handsFreeRestartSignal + 1,
                handsFreeSessionStatus = "Listening again"
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

    fun setDrivingRequireWakeWord(enabled: Boolean) {
        if (!enabled) {
            allowNextWakeGatedUtterance = false
        }

        _uiState.update { it.copy(drivingRequireWakeWord = enabled) }
        viewModelScope.launch {
            settingsRepository.setDrivingRequireWakeWord(enabled)
        }
    }

    fun setDrivingUseVoxtralTranscription(enabled: Boolean) {
        _uiState.update { it.copy(drivingUseVoxtralTranscription = enabled) }
        viewModelScope.launch {
            settingsRepository.setDrivingUseVoxtralTranscription(enabled)
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
        allowNextWakeGatedUtterance = false
        _uiState.update {
            it.copy(
                isDrivingMode = false,
                isHandsFreeSessionActive = false,
                isRecordingVoiceClip = false,
                isTranscribingVoiceClip = false,
                handsFreeSessionStatus = if (it.isHandsFreeSessionActive) {
                    "Stopped"
                } else {
                    it.handsFreeSessionStatus
                }
            )
        }
    }

    fun startHandsFreeSession() {
        allowNextWakeGatedUtterance = false
        _uiState.update {
            it.copy(
                isDrivingMode = true,
                showSettings = false,
                isHandsFreeSessionActive = true,
                handsFreeTurns = 0,
                handsFreeSessionQueuedActions = 0,
                handsFreeSessionConfirmedActions = 0,
                handsFreeSessionCancelledActions = 0,
                handsFreeSessionIgnoredUtterances = 0,
                handsFreeSessionSummary = null,
                recentHandsFreeCaptures = emptyList(),
                recentIgnoredUtterances = emptyList(),
                localQueueStatusMessage = null,
                handsFreeSessionStatus = if (it.drivingRequireWakeWord) {
                    "Starting. Say the trigger phrase first"
                } else {
                    "Starting"
                }
            )
        }
    }

    fun stopHandsFreeSession(reason: String = "Stopped") {
        allowNextWakeGatedUtterance = false
        _uiState.update {
            it.copy(
                isHandsFreeSessionActive = false,
                isListening = false,
                isSpeaking = false,
                isRecordingVoiceClip = false,
                isTranscribingVoiceClip = false,
                handsFreeSessionStatus = reason,
                handsFreeSessionSummary = buildHandsFreeSummary(it, reason),
                lastVoiceCommand = if (reason.contains("voice", ignoreCase = true)) {
                    reason
                } else {
                    it.lastVoiceCommand
                }
            )
        }
    }

    fun onSpeakingStarted() {
        _uiState.update {
            it.copy(
                isSpeaking = true,
                handsFreeSessionStatus = if (it.isHandsFreeSessionActive) {
                    "${it.selectedAgent.label} is speaking"
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
                    if (it.drivingRequireWakeWord) {
                        "Preparing to listen for trigger phrase"
                    } else {
                        "Preparing to listen"
                    }
                } else {
                    it.handsFreeSessionStatus
                }
            )
        }
    }

    fun onHandsFreeWaitingForNextTurn() {
        _uiState.update {
            if (it.isHandsFreeSessionActive) {
                it.copy(
                    handsFreeSessionStatus = if (it.drivingRequireWakeWord) {
                        "Ready. Say the trigger phrase first"
                    } else {
                        "Ready for next turn"
                    }
                )
            } else {
                it
            }
        }
    }

    private fun prepareHandsFreeTranscript(transcript: String): PreparedTranscript? {
        val state = _uiState.value
        if (!state.isHandsFreeSessionActive) {
            return PreparedTranscript(transcript = transcript)
        }

        val wakeStripped = stripWakePhrase(transcript)
        val hasWakePhrase = wakeStripped != null
        val candidate = wakeStripped ?: transcript
        val modeOverride = modeOverrideForDrivingPhrase(candidate)
        val message = stripDrivingModePrefix(candidate).trim()

        if (state.drivingRequireWakeWord) {
            if (hasWakePhrase && message.isBlank()) {
                allowNextWakeGatedUtterance = true
                requestHandsFreeRestart(
                    status = "Trigger phrase heard",
                    sessionStatus = "Listening for your request",
                    command = "trigger phrase"
                )
                return null
            }

            if (!hasWakePhrase && !allowNextWakeGatedUtterance) {
                requestHandsFreeRestart(
                    status = "Waiting for trigger phrase",
                    sessionStatus = "Ignored. Say the trigger phrase first",
                    ignoredTranscript = transcript
                )
                return null
            }
        }

        allowNextWakeGatedUtterance = false
        return PreparedTranscript(
            transcript = message.ifBlank { candidate },
            modeOverride = modeOverride
        )
    }

    private fun handleImmediateHandsFreeVoiceCommand(
        transcript: String,
        speak: (String) -> Unit
    ): Boolean {
        if (!_uiState.value.isHandsFreeSessionActive) {
            return false
        }

        val commandText = stripWakePhrase(transcript) ?: transcript
        val normalized = normalizeVoiceText(commandText)
        return when (normalized) {
            "stop", "stop listening", "stop hands free", "stop handsfree",
            "cancel hands free", "cancel handsfree" -> {
                stopHandsFreeSession("Stopped by voice")
                true
            }

            "interrupt", "stop talking" -> {
                speakLocalFeedback("Ready.", speak, command = normalized)
                true
            }

            else -> false
        }
    }

    private fun handleHandsFreeVoiceCommand(
        transcript: String,
        speak: (String) -> Unit
    ): Boolean {
        if (!_uiState.value.isHandsFreeSessionActive) {
            return false
        }

        val commandText = stripWakePhrase(transcript) ?: transcript
        val normalized = normalizeVoiceText(commandText)

        return when (normalized) {
            "wait", "hold on", "pause", "pause listening" -> {
                allowNextWakeGatedUtterance = false
                speakLocalFeedback(
                    "Paused. Say the trigger phrase when you are ready.",
                    speak,
                    command = normalized
                )
                true
            }

            "resume", "keep going", "continue" -> {
                allowNextWakeGatedUtterance = false
                speakLocalFeedback("Listening.", speak, command = normalized)
                true
            }

            "repeat", "repeat that", "say that again", "speak again" -> {
                allowNextWakeGatedUtterance = false
                val reply = _uiState.value.agentReply
                if (reply.isBlank()) {
                    requestHandsFreeRestart(
                        status = "Nothing to repeat",
                        sessionStatus = "Listening",
                        command = normalized
                    )
                } else {
                    _uiState.update {
                        it.copy(
                            isListening = false,
                            lastVoiceCommand = normalized,
                            handsFreeSessionStatus = "Repeating"
                        )
                    }
                    speak(reply)
                }
                true
            }

            "confirm", "confirm action", "confirm queued action" -> {
                confirmNextQueuedAction(speak, normalized)
                true
            }

            "cancel", "cancel action", "cancel queued action" -> {
                cancelNextQueuedAction(speak, normalized)
                true
            }

            "read queued actions", "read queue", "what is queued",
            "whats queued", "queued actions" -> {
                readQueuedActions(speak, normalized)
                true
            }

            "help", "commands", "voice commands" -> {
                speakLocalFeedback(
                    "Try: repeat that, read queued actions, confirm, cancel, capture only, review mode, or stop hands free.",
                    speak,
                    command = normalized
                )
                true
            }

            "capture only", "capture mode" -> {
                allowNextWakeGatedUtterance = false
                updateDrivingModeByVoice(AgentMode.CaptureOnly, normalized, speak)
                true
            }

            "mobile mode", "driving mode" -> {
                allowNextWakeGatedUtterance = false
                updateDrivingModeByVoice(AgentMode.Mobile, normalized, speak)
                true
            }

            "review mode", "review required", "review required mode" -> {
                allowNextWakeGatedUtterance = false
                updateDrivingModeByVoice(AgentMode.ReviewRequired, normalized, speak)
                true
            }

            "normal mode" -> {
                allowNextWakeGatedUtterance = false
                updateDrivingModeByVoice(AgentMode.Normal, normalized, speak)
                true
            }

            else -> false
        }
    }

    private fun confirmNextQueuedAction(speak: (String) -> Unit, command: String) {
        val action = _uiState.value.queuedActions.firstOrNull {
            it.status == QueuedActionStatus.Queued
        }

        if (action == null) {
            speakLocalFeedback("No queued actions to confirm.", speak, command = command)
            return
        }

        _uiState.update {
            it.copy(
                queuedActions = it.queuedActions.map { queuedAction ->
                    if (queuedAction.id == action.id) {
                        queuedAction.copy(status = QueuedActionStatus.Confirmed)
                    } else {
                        queuedAction
                    }
                },
                handsFreeSessionConfirmedActions = it.handsFreeSessionConfirmedActions + 1,
                localQueueStatusMessage = "Locally confirmed: ${action.summary}",
                lastVoiceCommand = command,
                handsFreeSessionStatus = "Queued action confirmed locally"
            )
        }
        speakLocalFeedback(
            "Confirmed locally for later review. Nothing was executed.",
            speak,
            command = command
        )
    }

    private fun cancelNextQueuedAction(speak: (String) -> Unit, command: String) {
        val action = _uiState.value.queuedActions.firstOrNull {
            it.status == QueuedActionStatus.Queued
        }

        if (action == null) {
            speakLocalFeedback("No queued actions to cancel.", speak, command = command)
            return
        }

        _uiState.update {
            it.copy(
                queuedActions = it.queuedActions.map { queuedAction ->
                    if (queuedAction.id == action.id) {
                        queuedAction.copy(status = QueuedActionStatus.Cancelled)
                    } else {
                        queuedAction
                    }
                },
                handsFreeSessionCancelledActions = it.handsFreeSessionCancelledActions + 1,
                localQueueStatusMessage = "Cancelled locally: ${action.summary}",
                lastVoiceCommand = command,
                handsFreeSessionStatus = "Queued action cancelled"
            )
        }
        speakLocalFeedback("Cancelled.", speak, command = command)
    }

    private fun readQueuedActions(speak: (String) -> Unit, command: String) {
        val pendingActions = _uiState.value.queuedActions.filter {
            it.status == QueuedActionStatus.Queued
        }

        val message = if (pendingActions.isEmpty()) {
            "No queued actions."
        } else {
            pendingActions
                .take(3)
                .joinToString(separator = ". ", prefix = "Queued actions: ") { it.summary }
        }

        speakLocalFeedback(message, speak, command = command)
    }

    private fun updateDrivingModeByVoice(
        mode: AgentMode,
        command: String,
        speak: (String) -> Unit
    ) {
        _uiState.update {
            it.copy(
                drivingMode = mode,
                connectionStatus = "Mode changed",
                handsFreeSessionStatus = "Driving mode: ${mode.label}",
                lastVoiceCommand = command
            )
        }
        viewModelScope.launch {
            settingsRepository.setDrivingMode(mode)
        }
        speakLocalFeedback("${mode.label.replaceFirstChar { it.uppercase() }} mode.", speak, command)
    }

    private fun requestHandsFreeRestart(
        status: String,
        sessionStatus: String,
        command: String? = null,
        ignoredTranscript: String? = null
    ) {
        _uiState.update {
            it.copy(
                isListening = false,
                isLoading = false,
                isSpeaking = false,
                connectionStatus = status,
                errorMessage = null,
                lastVoiceCommand = command ?: it.lastVoiceCommand,
                lastIgnoredTranscript = ignoredTranscript ?: it.lastIgnoredTranscript,
                handsFreeRestartSignal = it.handsFreeRestartSignal + 1,
                handsFreeSessionIgnoredUtterances = if (ignoredTranscript != null) {
                    it.handsFreeSessionIgnoredUtterances + 1
                } else {
                    it.handsFreeSessionIgnoredUtterances
                },
                recentIgnoredUtterances = if (ignoredTranscript != null) {
                    appendRecent(ignoredTranscript, it.recentIgnoredUtterances)
                } else {
                    it.recentIgnoredUtterances
                },
                handsFreeSessionStatus = sessionStatus
            )
        }
    }

    private fun speakLocalFeedback(
        message: String,
        speak: (String) -> Unit,
        command: String
    ) {
        _uiState.update {
            it.copy(
                isListening = false,
                isLoading = false,
                errorMessage = null,
                agentReply = message,
                connectionStatus = "Local command",
                lastVoiceCommand = command,
                handsFreeSessionStatus = message
            )
        }
        speak(message)
    }

    private fun buildHandsFreeSummary(state: MainUiState, reason: String): String {
        return listOf(
            reason,
            "Turns: ${state.handsFreeTurns}",
            "Queued: ${state.handsFreeSessionQueuedActions}",
            "Confirmed: ${state.handsFreeSessionConfirmedActions}",
            "Cancelled: ${state.handsFreeSessionCancelledActions}",
            "Ignored: ${state.handsFreeSessionIgnoredUtterances}",
            "Recoveries: ${state.handsFreeRecoveryCount}"
        ).joinToString(". ")
    }

    private fun appendRecent(value: String, existing: List<String>): List<String> {
        val trimmed = value.trim()
        if (trimmed.isBlank()) {
            return existing
        }

        return (listOf(trimmed) + existing).distinct().take(5)
    }

    private fun modeOverrideForDrivingPhrase(transcript: String): AgentMode? {
        val normalized = normalizeVoiceText(transcript)
        return when {
            normalized.startsWith("capture this ") ||
                normalized.startsWith("capture ") ||
                normalized.startsWith("note this ") ||
                normalized.startsWith("remember this ") -> AgentMode.CaptureOnly
            normalized.startsWith("ask hermes ") -> AgentMode.Mobile
            else -> null
        }
    }

    private fun stripDrivingModePrefix(transcript: String): String {
        return transcript
            .replace(Regex("^\\s*capture\\s+this\\b[\\s,.:;-]*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\s*capture\\b[\\s,.:;-]+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\s*note\\s+this\\b[\\s,.:;-]*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\s*remember\\s+this\\b[\\s,.:;-]*", RegexOption.IGNORE_CASE), "")
            .replace(
                Regex("^\\s*ask\\s+hermes\\b[\\s,.:;-]*", RegexOption.IGNORE_CASE),
                ""
            )
    }

    private fun stripWakePhrase(transcript: String): String? {
        val regex = Regex(
            "^\\s*(hey\\s+|ok\\s+|okay\\s+)?hermes\\b[\\s,.:;!\\-]*",
            RegexOption.IGNORE_CASE
        )
        val match = regex.find(transcript) ?: return null
        return transcript.substring(match.range.last + 1).trim()
    }

    private fun normalizeVoiceText(transcript: String): String =
        transcript
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    private data class PreparedTranscript(
        val transcript: String,
        val modeOverride: AgentMode? = null
    )

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
