package com.agentvoice.app.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agentvoice.app.commands.CommandCatalog
import com.agentvoice.app.commands.CommandCatalogRepository
import com.agentvoice.app.commands.CommandOption
import com.agentvoice.app.commands.HermesCommand
import com.agentvoice.app.model.AgentMode
import com.agentvoice.app.model.AgentPresentation
import com.agentvoice.app.model.ChatMessage
import com.agentvoice.app.model.ChatThread
import com.agentvoice.app.model.ConnectorType
import com.agentvoice.app.model.ConversationRecord
import com.agentvoice.app.model.QueuedAction
import com.agentvoice.app.model.QueuedActionStatus
import com.agentvoice.app.model.RunStreamEvent
import com.agentvoice.app.model.ChatMessageStatus
import com.agentvoice.app.model.ChatBackground
import com.agentvoice.app.model.ThreadCreationPolicy
import com.agentvoice.app.model.ThreadTitlePolicy
import com.agentvoice.app.network.RelayClient
import com.agentvoice.app.network.RelayClientException
import com.agentvoice.app.storage.ConversationRepository
import com.agentvoice.app.storage.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class MainUiState(
    val connectionStatus: String = "Relay not connected",
    val mode: AgentMode = AgentMode.Normal,
    val isListening: Boolean = false,
    val isLoading: Boolean = false,
    val isCreatingThread: Boolean = false,
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
    val threads: List<ChatThread> = emptyList(),
    val selectedThreadId: String? = null,
    val activeThreadMessages: List<ChatMessage> = emptyList(),
    val isThreadListVisible: Boolean = true,
    val showRenameThread: Boolean = false,
    val threadTitleDraft: String = "",
    val renamingThreadId: String? = null,
    val activeThreadActionId: String? = null,
    val showThreadActions: Boolean = false,
    val showDeleteThreadConfirmation: Boolean = false,
    val activeMessageActionId: String? = null,
    val selectedMessageIds: Set<String> = emptySet(),
    val commandCatalog: CommandCatalog = CommandCatalog.Empty,
    val commandSuggestions: List<HermesCommand> = emptyList(),
    val activeCommand: HermesCommand? = null,
    val activeCommandStepIndex: Int = 0,
    val commandSelections: Map<String, String> = emptyMap(),
    val commandTextDraft: String = "",
    val commandPickerError: String? = null,
    val pendingImmediateCommand: String? = null,
    val pendingImmediateCommandSignal: Int = 0,
    val incomingNotificationTitle: String? = null,
    val incomingNotificationText: String? = null,
    val incomingNotificationSignal: Int = 0,
    val activeRunId: String? = null,
    val streamingReply: String = "",
    val toolProgressMessage: String? = null,
    val presentation: AgentPresentation = AgentPresentation(),
    val recentHistory: List<ConversationRecord> = emptyList(),
    val selectedHistory: ConversationRecord? = null,
    val backendUrl: String = SettingsRepository.DEFAULT_BACKEND_URL,
    val backendUrlDraft: String = SettingsRepository.DEFAULT_BACKEND_URL,
    val selectedAgent: ConnectorType = SettingsRepository.DEFAULT_SELECTED_AGENT,
    val showSettings: Boolean = false,
    val isDrivingMode: Boolean = false,
    val appVersion: String = "unknown",
    val lastTranscript: String? = null,
    val pendingThreadCreateId: String? = null
) {
    val canRetry: Boolean
        get() = !isLoading && errorMessage != null && !lastTranscript.isNullOrBlank()

    val canSendTypedMessage: Boolean
        get() = !isLoading && !isListening && typedMessage.isNotBlank()

    val canSpeakAgain: Boolean
        get() = !isLoading &&
            agentReply.isNotBlank() &&
            (ttsEnabled || (isDrivingMode && drivingAutoSpeak))

    val selectedThread: ChatThread?
        get() = threads.firstOrNull { it.id == selectedThreadId }

    val actionThread: ChatThread?
        get() = threads.firstOrNull { it.id == activeThreadActionId }

    val activeMessageAction: ChatMessage?
        get() = activeThreadMessages.firstOrNull { it.id == activeMessageActionId }

    val selectedMessages: List<ChatMessage>
        get() = activeThreadMessages.filter { it.id in selectedMessageIds }

    val isMessageSelectionMode: Boolean
        get() = selectedMessageIds.isNotEmpty()

    val activeCommandStep
        get() = activeCommand?.steps?.getOrNull(activeCommandStepIndex)

    val activeCommandOptions: List<CommandOption>
        get() = activeCommand?.optionsForStep(activeCommandStepIndex, commandSelections)
            .orEmpty()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application.applicationContext)
    private val conversationRepository = ConversationRepository(application.applicationContext)
    private val commandCatalogRepository =
        CommandCatalogRepository(application.applicationContext)
    private val relayClient = RelayClient()
    private var appliedInitialDrivingMode = false
    private var initialSettingsLoaded = false
    private var allowNextWakeGatedUtterance = false
    private var observedMessagesThreadId: String? = null
    private var messagesJob: Job? = null
    private var commandCatalogBackendUrl: String? = null
    private var inboxSyncInFlight = false
    private var canonicalSyncInFlight = false
    private val autoTitleThreadsInFlight = mutableSetOf<String>()
    private var pendingDeviceRegistration: Pair<String, String>? = null
    private var registeredDeviceKey: String? = null

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    init {
        _uiState.update { it.copy(appVersion = resolveAppVersion(application)) }

        viewModelScope.launch {
            settingsRepository.migrateDefaultAgentToHermes()
        }

        viewModelScope.launch(Dispatchers.IO) {
            val catalog = commandCatalogRepository.load()
            _uiState.update {
                it.copy(
                    commandCatalog = catalog,
                    commandSuggestions = commandSuggestionsFor(it.typedMessage, catalog)
                )
            }
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
                initialSettingsLoaded = true
                registerPendingNotificationDevice()
                refreshCommandCatalogFromRelay(settings.backendUrl)
                if (settings.selectedAgent == ConnectorType.Hermes) {
                    syncCanonicalThreads()
                    return@collect
                }
                if (
                    _uiState.value.threads.isEmpty() &&
                    _uiState.value.selectedThreadId == null
                ) {
                    val defaultThread = conversationRepository.ensureDefaultThread(
                        connector = settings.selectedAgent,
                        mode = settings.defaultMode
                    )
                    activateThread(defaultThread, openChat = false)
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

        viewModelScope.launch {
            conversationRepository.chatThreads.collect { threads ->
                if (threads.isEmpty()) {
                    if (!initialSettingsLoaded) {
                        return@collect
                    }

                    val state = _uiState.value
                    if (state.selectedAgent == ConnectorType.Hermes) {
                        _uiState.update {
                            it.copy(
                                threads = emptyList(),
                                selectedThreadId = null,
                                activeThreadMessages = emptyList()
                            )
                        }
                        return@collect
                    }
                    val defaultThread = conversationRepository.ensureDefaultThread(
                        connector = state.selectedAgent,
                        mode = state.mode
                    )
                    activateThread(defaultThread, openChat = false)
                    return@collect
                }

                val currentThreadId = _uiState.value.selectedThreadId
                val nextThread = threads.firstOrNull { it.id == currentThreadId } ?: threads.first()
                if (nextThread.id != currentThreadId) {
                    observeThreadMessages(nextThread.id)
                }

                _uiState.update {
                    it.copy(
                        threads = threads,
                        selectedThreadId = nextThread.id,
                        activeThreadMessages = if (nextThread.id == currentThreadId) {
                            it.activeThreadMessages
                        } else {
                            emptyList()
                        },
                        selectedAgent = if (nextThread.id == currentThreadId) {
                            it.selectedAgent
                        } else {
                            nextThread.connector
                        },
                        mode = if (nextThread.id == currentThreadId) {
                            it.mode
                        } else {
                            nextThread.mode
                        }
                    )
                }
            }
        }
    }

    fun selectMode(mode: AgentMode) {
        _uiState.update { it.copy(mode = mode) }
        viewModelScope.launch {
            _uiState.value.selectedThreadId?.let { threadId ->
                conversationRepository.updateThreadMode(threadId, mode)
            }
            settingsRepository.setDefaultMode(mode)
        }
    }

    fun selectChatBackground(backgroundId: String) {
        val selected = ChatBackground.fromId(backgroundId)
        val threadId = _uiState.value.selectedThreadId ?: return
        viewModelScope.launch {
            conversationRepository.updateThreadBackground(threadId, selected.id)
        }
    }

    fun showThreadList() {
        _uiState.update {
            it.copy(
                isThreadListVisible = true,
                selectedHistory = null,
                showThreadActions = false,
                activeThreadActionId = null,
                activeMessageActionId = null,
                selectedMessageIds = emptySet()
            )
        }
    }

    fun selectThread(thread: ChatThread) {
        activateThread(thread, openChat = true)
        if (thread.connector == ConnectorType.Hermes) {
            syncCanonicalMessages(thread.id)
        }
    }

    fun openThreadById(threadId: String) {
        viewModelScope.launch {
            conversationRepository.getThread(threadId)?.let { thread ->
                selectThread(thread)
                return@launch
            }

            val backendUrl = _uiState.value.backendUrl
            relayClient.fetchCanonicalThreads(backendUrl)
                .onSuccess { threads ->
                    conversationRepository.upsertCanonicalThreads(threads)
                    conversationRepository.getThread(threadId)?.let { thread ->
                        selectThread(thread)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(lastRelayErrorMessage = error.message)
                    }
                }
        }
    }

    fun registerNotificationDevice(installationId: String, fcmToken: String) {
        if (installationId.isBlank() || fcmToken.isBlank()) return
        pendingDeviceRegistration = installationId to fcmToken
        registerPendingNotificationDevice()
    }

    private fun registerPendingNotificationDevice() {
        val (installationId, fcmToken) = pendingDeviceRegistration ?: return
        val backendUrl = _uiState.value.backendUrl
        if (backendUrl.isBlank()) return
        val registrationKey = "$backendUrl|$installationId|$fcmToken"
        if (registrationKey == registeredDeviceKey) return

        viewModelScope.launch {
            relayClient.registerDevice(backendUrl, installationId, fcmToken)
                .onSuccess {
                    registeredDeviceKey = registrationKey
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            lastRelayErrorMessage =
                                "Notifications are not connected: ${error.message}"
                        )
                    }
                }
        }
    }

    fun createThread() {
        val state = _uiState.value
        if (state.selectedAgent == ConnectorType.Hermes) {
            val clientThreadId = ThreadCreationPolicy.requestId(
                isCreating = state.isCreatingThread,
                pendingRequestId = state.pendingThreadCreateId,
                generatedRequestId = UUID.randomUUID().toString()
            ) ?: return
            _uiState.update {
                it.copy(
                    isCreatingThread = true,
                    pendingThreadCreateId = clientThreadId,
                    connectionStatus = "Creating Hermes chat",
                    errorMessage = null
                )
            }
            viewModelScope.launch {
                relayClient.createCanonicalThread(
                    backendUrl = state.backendUrl,
                    clientThreadId = clientThreadId
                ).onSuccess { canonical ->
                    val thread = conversationRepository.upsertCanonicalThread(canonical, state.mode)
                    activateThread(thread, openChat = true)
                    _uiState.update {
                        it.copy(
                            isCreatingThread = false,
                            pendingThreadCreateId = null,
                            connectionStatus = "Hermes chat ready",
                            errorMessage = null
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isCreatingThread = false,
                            connectionStatus = "Chat not created",
                            errorMessage = error.message ?: "Unable to create Hermes chat.",
                            lastRelayErrorMessage = error.message
                        )
                    }
                }
            }
        } else {
            viewModelScope.launch {
                val thread = conversationRepository.createThread(
                    title = ConversationRepository.DEFAULT_THREAD_TITLE,
                    connector = state.selectedAgent,
                    mode = state.mode
                )
                activateThread(thread, openChat = true)
                _uiState.update { it.copy(pendingThreadCreateId = null, errorMessage = null) }
            }
        }
    }

    fun startRenameSelectedThread() {
        val threadId = _uiState.value.selectedThreadId ?: return
        startRenameThread(threadId)
    }

    fun openThreadActions(thread: ChatThread) {
        _uiState.update {
            it.copy(
                activeThreadActionId = thread.id,
                showThreadActions = true,
                showDeleteThreadConfirmation = false,
                errorMessage = null
            )
        }
    }

    fun closeThreadActions() {
        _uiState.update { it.copy(showThreadActions = false, activeThreadActionId = null) }
    }

    fun startRenameThreadFromActions() {
        val threadId = _uiState.value.activeThreadActionId ?: return
        _uiState.update { it.copy(showThreadActions = false) }
        startRenameThread(threadId)
    }

    private fun startRenameThread(threadId: String) {
        val title = _uiState.value.threads.firstOrNull { it.id == threadId }?.title ?: return
        _uiState.update {
            it.copy(
                showRenameThread = true,
                threadTitleDraft = title,
                renamingThreadId = threadId,
                errorMessage = null
            )
        }
    }

    fun updateThreadTitleDraft(title: String) {
        _uiState.update { it.copy(threadTitleDraft = title, errorMessage = null) }
    }

    fun cancelRenameThread() {
        _uiState.update {
            it.copy(
                showRenameThread = false,
                threadTitleDraft = "",
                renamingThreadId = null
            )
        }
    }

    fun saveThreadTitle() {
        val state = _uiState.value
        val threadId = state.renamingThreadId ?: state.selectedThreadId ?: return
        val title = state.threadTitleDraft.trim()
        viewModelScope.launch {
            val renameResult = if (state.selectedAgent == ConnectorType.Hermes) {
                relayClient.renameCanonicalThread(state.backendUrl, threadId, title)
                    .map { Unit }
            } else {
                Result.success(Unit)
            }
            renameResult.onSuccess {
                conversationRepository.renameThread(threadId, title)
                _uiState.update {
                    it.copy(
                        showRenameThread = false,
                        threadTitleDraft = "",
                        renamingThreadId = null,
                        activeThreadActionId = null,
                        connectionStatus = "Thread renamed",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Unable to rename Hermes chat.")
                }
            }
        }
    }

    fun requestDeleteThreadFromActions() {
        if (_uiState.value.activeThreadActionId == null) {
            return
        }

        _uiState.update {
            it.copy(
                showThreadActions = false,
                showDeleteThreadConfirmation = true,
                errorMessage = null
            )
        }
    }

    fun cancelDeleteThread() {
        _uiState.update {
            it.copy(
                showDeleteThreadConfirmation = false,
                activeThreadActionId = null
            )
        }
    }

    fun confirmDeleteThread() {
        val state = _uiState.value
        val threadId = state.activeThreadActionId ?: return
        val deletingSelectedThread = threadId == state.selectedThreadId

        viewModelScope.launch {
            val deleted = conversationRepository.deleteThreadLocally(threadId)
            _uiState.update {
                it.copy(
                    showDeleteThreadConfirmation = false,
                    activeThreadActionId = null,
                    selectedThreadId = if (deletingSelectedThread) null else it.selectedThreadId,
                    activeThreadMessages = if (deletingSelectedThread) {
                        emptyList()
                    } else {
                        it.activeThreadMessages
                    },
                    isThreadListVisible = true,
                    connectionStatus = if (deleted) {
                        "Chat deleted from this app"
                    } else {
                        "Chat already removed"
                    },
                    errorMessage = null
                )
            }
        }
    }

    fun openMessageActions(message: ChatMessage) {
        _uiState.update {
            it.copy(activeMessageActionId = message.id)
        }
    }

    fun closeMessageActions() {
        _uiState.update { it.copy(activeMessageActionId = null) }
    }

    fun startMessageSelection(message: ChatMessage) {
        _uiState.update {
            it.copy(
                activeMessageActionId = null,
                selectedMessageIds = setOf(message.id)
            )
        }
    }

    fun toggleMessageSelection(message: ChatMessage) {
        _uiState.update {
            val next = if (message.id in it.selectedMessageIds) {
                it.selectedMessageIds - message.id
            } else {
                it.selectedMessageIds + message.id
            }
            it.copy(selectedMessageIds = next)
        }
    }

    fun clearMessageSelection() {
        _uiState.update { it.copy(selectedMessageIds = emptySet()) }
    }

    fun openCommandPicker(command: HermesCommand) {
        if (command.steps.isEmpty()) {
            completeCommand(command, emptyMap())
            return
        }

        _uiState.update {
            it.copy(
                activeCommand = command,
                activeCommandStepIndex = 0,
                commandSelections = emptyMap(),
                commandTextDraft = "",
                commandPickerError = null,
                commandSuggestions = emptyList(),
                errorMessage = null
            )
        }
    }

    fun cancelCommandPicker() {
        _uiState.update {
            it.copy(
                activeCommand = null,
                activeCommandStepIndex = 0,
                commandSelections = emptyMap(),
                commandTextDraft = "",
                commandPickerError = null
            )
        }
    }

    fun selectCommandOption(option: CommandOption) {
        val state = _uiState.value
        val command = state.activeCommand ?: return
        val step = state.activeCommandStep ?: return
        val nextSelections = selectionsBeforeActiveStep(command, state) + (step.id to option.value)
        advanceCommand(command, nextSelections)
    }

    fun updateCommandTextDraft(value: String) {
        _uiState.update {
            it.copy(commandTextDraft = value, commandPickerError = null)
        }
    }

    fun submitCommandTextStep() {
        val state = _uiState.value
        val command = state.activeCommand ?: return
        val step = state.activeCommandStep ?: return
        val value = state.commandTextDraft.trim()
        if (step.required && value.isBlank()) {
            _uiState.update {
                it.copy(commandPickerError = "Enter ${step.label.lowercase()}.")
            }
            return
        }

        val baseSelections = selectionsBeforeActiveStep(command, state)
        val nextSelections = if (value.isBlank()) {
            baseSelections
        } else {
            baseSelections + (step.id to value)
        }
        advanceCommand(command, nextSelections)
    }

    fun skipCommandTextStep() {
        val state = _uiState.value
        val command = state.activeCommand ?: return
        val step = state.activeCommandStep ?: return
        if (step.required) {
            return
        }

        advanceCommand(command, selectionsBeforeActiveStep(command, state))
    }

    fun backCommandStep() {
        _uiState.update {
            if (it.activeCommandStepIndex <= 0) {
                it
            } else {
                it.copy(
                    activeCommandStepIndex = it.activeCommandStepIndex - 1,
                    commandTextDraft = "",
                    commandPickerError = null
                )
            }
        }
    }

    private fun advanceCommand(
        command: HermesCommand,
        selections: Map<String, String>
    ) {
        val nextStepIndex = _uiState.value.activeCommandStepIndex + 1
        if (nextStepIndex >= command.steps.size) {
            completeCommand(command, selections)
            return
        }

        _uiState.update {
            it.copy(
                commandSelections = selections,
                activeCommandStepIndex = nextStepIndex,
                commandTextDraft = "",
                commandPickerError = null
            )
        }
    }

    private fun completeCommand(
        command: HermesCommand,
        selections: Map<String, String>
    ) {
        val renderedCommand = command.render(selections)
        val shouldSendImmediately = command.sendMode == COMMAND_SEND_IMMEDIATELY
        _uiState.update {
            it.copy(
                typedMessage = if (shouldSendImmediately) "" else renderedCommand,
                activeCommand = null,
                activeCommandStepIndex = 0,
                commandSelections = emptyMap(),
                commandTextDraft = "",
                commandPickerError = null,
                commandSuggestions = emptyList(),
                pendingImmediateCommand = if (shouldSendImmediately) renderedCommand else it.pendingImmediateCommand,
                pendingImmediateCommandSignal = if (shouldSendImmediately) {
                    it.pendingImmediateCommandSignal + 1
                } else {
                    it.pendingImmediateCommandSignal
                },
                errorMessage = null
            )
        }
    }

    fun sendPendingImmediateCommand(speak: (String) -> Unit) {
        val command = _uiState.value.pendingImmediateCommand ?: return
        _uiState.update { it.copy(pendingImmediateCommand = null) }
        submitTranscript(command, speak, inputType = "text")
    }

    fun syncPendingMessages() {
        val snapshot = _uiState.value
        if (snapshot.selectedAgent == ConnectorType.Hermes) {
            syncCanonicalThreads(syncActiveMessages = true)
            return
        }
        val threadId = snapshot.selectedThreadId ?: return
        val backendUrl = snapshot.backendUrl
        if (inboxSyncInFlight || backendUrl.isBlank()) {
            return
        }

        inboxSyncInFlight = true
        viewModelScope.launch {
            try {
                relayClient
                    .fetchPendingMessages(
                        backendUrl = backendUrl,
                        conversationId = threadId
                    )
                    .onSuccess { messages ->
                        val inserted = conversationRepository.saveIncomingRelayMessages(
                            threadId = threadId,
                            messages = messages
                        )
                        messages.forEach { message ->
                            relayClient.acknowledgeMessage(backendUrl, message.id)
                        }
                        if (inserted.isNotEmpty()) {
                            val latest = inserted.last()
                            _uiState.update {
                                it.copy(
                                    connectionStatus = if (inserted.size == 1) {
                                        "Received 1 new message"
                                    } else {
                                        "Received ${inserted.size} new messages"
                                    },
                                    incomingNotificationTitle = latest.role.label,
                                    incomingNotificationText = latest.text,
                                    incomingNotificationSignal = it.incomingNotificationSignal + 1
                                )
                            }
                        }
                    }
            } finally {
                inboxSyncInFlight = false
            }
        }
    }

    private fun syncCanonicalThreads(syncActiveMessages: Boolean = false) {
        val snapshot = _uiState.value
        if (canonicalSyncInFlight || snapshot.backendUrl.isBlank()) return
        canonicalSyncInFlight = true
        viewModelScope.launch {
            try {
                relayClient.fetchCanonicalThreads(snapshot.backendUrl)
                    .onSuccess { threads ->
                        conversationRepository.upsertCanonicalThreads(threads)
                        _uiState.update {
                            it.copy(
                                connectionStatus = "Hermes synced",
                                errorMessage = null
                            )
                        }
                        if (syncActiveMessages) {
                            val activeId = _uiState.value.selectedThreadId
                            if (activeId != null) syncCanonicalMessages(activeId)
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                connectionStatus = "Hermes sync unavailable",
                                lastRelayErrorMessage = error.message
                            )
                        }
                    }
            } finally {
                canonicalSyncInFlight = false
            }
        }
    }

    private fun syncCanonicalMessages(threadId: String) {
        val backendUrl = _uiState.value.backendUrl
        viewModelScope.launch {
            relayClient.fetchCanonicalMessages(backendUrl, threadId)
                .onSuccess { messages ->
                    conversationRepository.replaceWithCanonicalMessages(threadId, messages)
                    _uiState.update {
                        it.copy(
                            connectionStatus = "Hermes history synced",
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(lastRelayErrorMessage = error.message)
                    }
                }
        }
    }

    private fun selectionsBeforeActiveStep(
        command: HermesCommand,
        state: MainUiState
    ): Map<String, String> {
        val previousStepIds = command.steps
            .take(state.activeCommandStepIndex)
            .map { it.id }
            .toSet()
        return state.commandSelections.filterKeys { it in previousStepIds }
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
        _uiState.update {
            it.copy(
                typedMessage = message,
                commandSuggestions = commandSuggestionsFor(message, it.commandCatalog),
                errorMessage = null
            )
        }
    }

    fun sendTypedMessage(speak: (String) -> Unit) {
        submitTranscript(
            _uiState.value.typedMessage,
            speak,
            clearTypedMessage = true,
            inputType = "text"
        )
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
        modeOverride: AgentMode? = null,
        inputType: String = "voice"
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
        if (agent == ConnectorType.Hermes) {
            submitCanonicalTranscript(
                transcript = trimmedTranscript,
                mode = mode,
                inputType = inputType,
                clearTypedMessage = clearTypedMessage,
                speak = speak
            )
            return
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
            val activeThreadId = snapshot.selectedThreadId ?: conversationRepository
                .ensureDefaultThread(agent, mode)
                .also { activateThread(it, openChat = false) }
                .id

            conversationRepository.saveUserMessage(activeThreadId, trimmedTranscript)

            relayClient
                .sendMessage(
                    backendUrl = snapshot.backendUrl,
                    agent = agent,
                    message = trimmedTranscript,
                    mode = mode,
                    conversationId = activeThreadId
                )
                .onSuccess { response ->
                    conversationRepository.saveResponse(trimmedTranscript, response)
                    conversationRepository.saveAssistantResponse(activeThreadId, response)
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
                    conversationRepository.saveAssistantFailure(
                        threadId = activeThreadId,
                        connector = agent,
                        mode = mode,
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

    private fun submitCanonicalTranscript(
        transcript: String,
        mode: AgentMode,
        inputType: String,
        clearTypedMessage: Boolean,
        speak: (String) -> Unit
    ) {
        val snapshot = _uiState.value
        _uiState.update {
            it.copy(
                isListening = false,
                isLoading = true,
                transcript = transcript,
                agentReply = "",
                streamingReply = "",
                toolProgressMessage = null,
                presentation = AgentPresentation(state = "thinking", motionHint = "think"),
                errorMessage = null,
                lastTranscript = transcript,
                typedMessage = if (clearTypedMessage) "" else it.typedMessage,
                connectionStatus = if (it.isHandsFreeSessionActive) "Thinking" else "Starting Hermes run",
                handsFreeSessionStatus = if (it.isHandsFreeSessionActive) "Thinking" else it.handsFreeSessionStatus
            )
        }

        viewModelScope.launch {
            val thread = snapshot.selectedThreadId?.let { conversationRepository.getThread(it) }
                ?: relayClient.createCanonicalThread(
                    backendUrl = snapshot.backendUrl,
                    clientThreadId = UUID.randomUUID().toString()
                ).getOrElse { error ->
                    finishCanonicalFailure(error.message ?: "Unable to create Hermes chat.")
                    return@launch
                }.let { canonical ->
                    conversationRepository.upsertCanonicalThread(canonical, mode).also {
                        activateThread(it, openChat = !snapshot.isDrivingMode)
                    }
                }

            val clientMessageId = "client_${UUID.randomUUID()}"
            conversationRepository.saveCanonicalUserMessage(thread.id, clientMessageId, transcript)
            if (
                thread.title == ConversationRepository.DEFAULT_THREAD_TITLE &&
                !transcript.trimStart().startsWith("/") &&
                autoTitleThreadsInFlight.add(thread.id)
            ) {
                viewModelScope.launch {
                    try {
                        autoTitleCanonicalThread(
                            threadId = thread.id,
                            text = transcript,
                            backendUrl = snapshot.backendUrl
                        )
                    } finally {
                        autoTitleThreadsInFlight.remove(thread.id)
                    }
                }
            }
            val run = relayClient.startMessageRun(
                backendUrl = snapshot.backendUrl,
                threadId = thread.id,
                clientMessageId = clientMessageId,
                content = transcript,
                mode = mode,
                inputType = inputType
            ).getOrElse { error ->
                conversationRepository.updateCanonicalUserStatus(clientMessageId, ChatMessageStatus.Failed)
                finishCanonicalFailure(error.message ?: "Unable to start Hermes response.")
                return@launch
            }

            conversationRepository.updateCanonicalUserStatus(clientMessageId, ChatMessageStatus.Sent)
            _uiState.update {
                it.copy(
                    activeRunId = run.runId,
                    connectionStatus = "Solara is thinking",
                    presentation = AgentPresentation(state = "thinking", motionHint = "think")
                )
            }
            var streamedText = ""
            relayClient.streamRunEvents(snapshot.backendUrl, run.runId) { event ->
                when (event) {
                    is RunStreamEvent.Started -> Unit
                    is RunStreamEvent.Delta -> {
                        streamedText += event.text
                        _uiState.update {
                            it.copy(
                                streamingReply = streamedText,
                                agentReply = streamedText,
                                connectionStatus = "Solara is replying",
                                presentation = it.presentation.copy(
                                    state = "speaking",
                                    motionHint = "speak"
                                )
                            )
                        }
                        viewModelScope.launch {
                            conversationRepository.saveStreamingAssistantMessage(
                                threadId = thread.id,
                                runId = run.runId,
                                text = streamedText,
                                status = ChatMessageStatus.Partial
                            )
                        }
                    }
                    is RunStreamEvent.ToolProgress -> {
                        _uiState.update {
                            it.copy(
                                toolProgressMessage = "${event.label} - ${event.status}",
                                connectionStatus = event.label,
                                presentation = it.presentation.copy(
                                    state = "tool",
                                    motionHint = "think"
                                )
                            )
                        }
                    }
                    is RunStreamEvent.Presentation -> {
                        _uiState.update { it.copy(presentation = event.value) }
                    }
                    is RunStreamEvent.Completed -> {
                        streamedText = event.text.ifBlank { streamedText }
                        _uiState.update {
                            it.copy(
                                presentation = event.presentation,
                                streamingReply = streamedText,
                                agentReply = streamedText
                            )
                        }
                    }
                    is RunStreamEvent.Failed -> {
                        _uiState.update { it.copy(lastRelayErrorMessage = event.error) }
                    }
                    RunStreamEvent.Cancelled -> Unit
                }
            }

            val finalRun = relayClient.fetchMessageRun(snapshot.backendUrl, run.runId)
                .getOrNull()
            when (finalRun?.status) {
                "completed" -> {
                    val replyText = finalRun.replyText.ifBlank { streamedText }
                    conversationRepository.saveStreamingAssistantMessage(
                        threadId = thread.id,
                        runId = run.runId,
                        text = replyText,
                        status = ChatMessageStatus.Completed
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeRunId = null,
                            streamingReply = "",
                            toolProgressMessage = null,
                            agentReply = replyText,
                            connectionStatus = "Solara replied",
                            presentation = finalRun.presentation,
                            lastConnector = ConnectorType.Hermes,
                            lastRequestId = run.runId,
                            errorMessage = null,
                            handsFreeTurns = if (it.isHandsFreeSessionActive) it.handsFreeTurns + 1 else it.handsFreeTurns,
                            handsFreeSessionStatus = if (it.isHandsFreeSessionActive) "Reply received" else it.handsFreeSessionStatus
                        )
                    }
                    syncCanonicalMessages(thread.id)
                    val latestState = _uiState.value
                    if (
                        latestState.ttsEnabled ||
                        (latestState.isDrivingMode && latestState.drivingAutoSpeak) ||
                        latestState.isHandsFreeSessionActive
                    ) {
                        speak(replyText)
                    }
                }
                "failed" -> finishCanonicalFailure(finalRun.error ?: "Hermes response failed.")
                "cancelled" -> finishCanonicalFailure("Hermes response was cancelled.")
                else -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            activeRunId = run.runId,
                            connectionStatus = "Solara is continuing in the background",
                            errorMessage = null
                        )
                    }
                }
            }
        }
    }

    private suspend fun autoTitleCanonicalThread(
        threadId: String,
        text: String,
        backendUrl: String
    ) {
        val candidates = ThreadTitlePolicy.candidates(
            text = text,
            threadId = threadId,
            fallback = ConversationRepository.DEFAULT_THREAD_TITLE
        )

        for (candidate in candidates) {
            val result = relayClient.renameCanonicalThread(backendUrl, threadId, candidate)
            result.onSuccess {
                conversationRepository.renameThread(threadId, candidate)
                return
            }
            val error = result.exceptionOrNull()
            if (error !is RelayClientException || error.statusCode != 409) {
                _uiState.update { it.copy(lastRelayErrorMessage = error?.message) }
                return
            }
        }
    }

    private fun finishCanonicalFailure(message: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                activeRunId = null,
                streamingReply = "",
                toolProgressMessage = null,
                connectionStatus = "Hermes error",
                errorMessage = message,
                lastErrorMessage = message,
                lastRelayErrorMessage = message,
                presentation = AgentPresentation(
                    mood = "concerned",
                    intensity = 0.6f,
                    state = "error",
                    motionHint = "reassure"
                ),
                handsFreeRecoveryCount = if (it.isHandsFreeSessionActive) {
                    it.handsFreeRecoveryCount + 1
                } else {
                    it.handsFreeRecoveryCount
                },
                handsFreeRestartSignal = if (it.isHandsFreeSessionActive) {
                    it.handsFreeRestartSignal + 1
                } else {
                    it.handsFreeRestartSignal
                }
            )
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
        _uiState.update {
            it.copy(
                selectedAgent = agent,
                connectionStatus = when (agent) {
                    ConnectorType.Hermes -> "Connecting to Hermes"
                    ConnectorType.Mock -> "Mock ready"
                    ConnectorType.OpenClaw -> "OpenClaw selected"
                    ConnectorType.Relay -> "Relay selected"
                    ConnectorType.Webhook -> "Webhook selected"
                },
                lastRelayErrorMessage = null
            )
        }
        viewModelScope.launch {
            _uiState.value.selectedThreadId?.let { threadId ->
                conversationRepository.updateThreadConnector(threadId, agent)
            }
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

    private fun activateThread(thread: ChatThread, openChat: Boolean) {
        observeThreadMessages(thread.id)
        _uiState.update {
            val isChangingThread = it.selectedThreadId != thread.id
            it.copy(
                selectedThreadId = thread.id,
                activeThreadMessages = if (!isChangingThread) {
                    it.activeThreadMessages
                } else {
                    emptyList()
                },
                isThreadListVisible = if (openChat) false else it.isThreadListVisible,
                selectedAgent = thread.connector,
                mode = thread.mode,
                typedMessage = "",
                transcript = if (isChangingThread) "" else it.transcript,
                agentReply = if (isChangingThread) "" else it.agentReply,
                queuedActions = if (isChangingThread) emptyList() else it.queuedActions,
                warnings = if (isChangingThread) emptyList() else it.warnings,
                selectedHistory = null,
                errorMessage = null,
                selectedMessageIds = emptySet(),
                activeMessageActionId = null
            )
        }
    }

    private fun observeThreadMessages(threadId: String) {
        if (observedMessagesThreadId == threadId) {
            return
        }

        observedMessagesThreadId = threadId
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            conversationRepository.messagesForThread(threadId).collect { messages ->
                _uiState.update {
                    if (it.selectedThreadId == threadId) {
                        it.copy(activeThreadMessages = messages)
                    } else {
                        it
                    }
                }
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

    private fun commandSuggestionsFor(
        message: String,
        catalog: CommandCatalog
    ): List<HermesCommand> {
        val trimmed = message.trimStart()
        if (!trimmed.startsWith("/") || trimmed.contains(Regex("\\s"))) {
            return emptyList()
        }

        return catalog.matchingCommands(trimmed).take(MAX_COMMAND_SUGGESTIONS)
    }

    private fun refreshCommandCatalogFromRelay(backendUrl: String) {
        val trimmedBackendUrl = backendUrl.trim()
        if (
            trimmedBackendUrl.isBlank() ||
            trimmedBackendUrl == commandCatalogBackendUrl
        ) {
            return
        }

        commandCatalogBackendUrl = trimmedBackendUrl
        viewModelScope.launch(Dispatchers.IO) {
            relayClient
                .fetchCommandCatalog(trimmedBackendUrl)
                .mapCatching { catalogJson -> commandCatalogRepository.parse(catalogJson) }
                .onSuccess { catalog ->
                    if (catalog.commands.isEmpty()) {
                        return@onSuccess
                    }
                    _uiState.update {
                        it.copy(
                            commandCatalog = catalog,
                            commandSuggestions = commandSuggestionsFor(it.typedMessage, catalog)
                        )
                    }
                }
        }
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

    companion object {
        private const val MAX_COMMAND_SUGGESTIONS = 6
        private const val COMMAND_SEND_IMMEDIATELY = "send_immediately"
    }
}
