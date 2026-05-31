package com.agentvoice.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agentvoice.app.model.ConnectorType
import com.agentvoice.app.shortcut.VoiceShortcutNotifier
import com.agentvoice.app.ui.DrivingScreen
import com.agentvoice.app.ui.MainScreen
import com.agentvoice.app.ui.SettingsScreen
import com.agentvoice.app.ui.theme.AgentVoiceTheme
import com.agentvoice.app.viewmodel.MainViewModel
import com.agentvoice.app.voice.SpeechInputManager
import com.agentvoice.app.voice.TtsManager

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val handsFreeHandler = Handler(Looper.getMainLooper())
    private lateinit var speechInputManager: SpeechInputManager
    private lateinit var shortcutNotifier: VoiceShortcutNotifier
    private lateinit var ttsManager: TtsManager

    private val handsFreeRestartRunnable = Runnable {
        maybeStartHandsFreeListening()
    }

    private val handsFreeTimeoutRunnable = Runnable {
        stopHandsFreeSession("Stopped after 10 minutes")
    }

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startSpeechInput()
        } else {
            handsFreeHandler.removeCallbacksAndMessages(null)
            viewModel.onPermissionDenied()
        }
    }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showDrivingShortcutNotification()
        } else {
            viewModel.onNotificationPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        shortcutNotifier = VoiceShortcutNotifier(this)
        shortcutNotifier.ensureChannel()
        speechInputManager = SpeechInputManager(
            context = this,
            listener = object : SpeechInputManager.Listener {
                override fun onListeningStarted() {
                    viewModel.onListeningStarted()
                }

                override fun onListeningFinished() {
                    viewModel.onListeningFinished()
                }

                override fun onTranscript(transcript: String) {
                    viewModel.onTranscriptCaptured(transcript, ::handleAgentSpeech)
                }

                override fun onError(message: String) {
                    if (viewModel.uiState.value.isHandsFreeSessionActive) {
                        handsFreeHandler.removeCallbacks(handsFreeRestartRunnable)
                        handsFreeHandler.removeCallbacks(handsFreeTimeoutRunnable)
                        ttsManager.stop()
                    }
                    viewModel.onSpeechError(message)
                }
            }
        )
        ttsManager = TtsManager(this)

        setContent {
            AgentVoiceTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    if (uiState.showSettings) {
                        SettingsScreen(
                            uiState = uiState,
                            onBackendUrlChange = viewModel::updateBackendUrlDraft,
                            onSaveBackendUrl = viewModel::saveBackendUrl,
                            onTestConnection = viewModel::testConnection,
                            onSendMockTestPrompt = ::handleMockTestPrompt,
                            onSendOpenClawTestPrompt = ::handleOpenClawTestPrompt,
                            onShowDrivingNotification = ::handleShowDrivingNotification,
                            onHideDrivingNotification = ::handleHideDrivingNotification,
                            onStartInDrivingModeToggle = viewModel::setStartInDrivingMode,
                            onKeepScreenAwakeToggle = viewModel::setKeepScreenAwakeInDrivingMode,
                            onDrivingAutoSpeakToggle = viewModel::setDrivingAutoSpeak,
                            onDrivingModeSelected = viewModel::setDrivingMode,
                            onAgentSelected = viewModel::selectAgent,
                            onClearHistory = viewModel::clearHistory,
                            onBack = viewModel::closeSettings
                        )
                    } else if (uiState.isDrivingMode) {
                        DrivingScreen(
                            uiState = uiState,
                            onTalkClick = ::handleTalkClick,
                            onRetryClick = ::handleRetryClick,
                            onSpeakAgainClick = ::handleSpeakAgainClick,
                            onStartHandsFreeClick = ::handleStartHandsFreeSession,
                            onStopHandsFreeClick = ::handleStopHandsFreeSession,
                            onInterruptClick = ::handleInterruptClick,
                            onSettingsClick = viewModel::openSettings,
                            onExitClick = ::handleExitDrivingMode
                        )
                    } else {
                        MainScreen(
                            uiState = uiState,
                            onModeSelected = viewModel::selectMode,
                            onTalkClick = ::handleTalkClick,
                            onTypedMessageChange = viewModel::updateTypedMessage,
                            onSendTypedMessage = ::handleSendTypedMessage,
                            onRetryClick = ::handleRetryClick,
                            onSpeakAgainClick = ::handleSpeakAgainClick,
                            onDrivingModeClick = viewModel::openDrivingMode,
                            onHistorySelected = viewModel::selectHistory,
                            onDismissHistory = viewModel::dismissHistoryDetail,
                            onTtsToggle = ::handleTtsToggle,
                            onSettingsClick = viewModel::openSettings
                        )
                    }

                    SideEffect {
                        applyDrivingWindowFlags(
                            enabled = uiState.isDrivingMode &&
                                uiState.keepScreenAwakeInDrivingMode
                        )
                    }
                }
            }
        }

        handleShortcutIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
    }

    override fun onDestroy() {
        handsFreeHandler.removeCallbacksAndMessages(null)
        speechInputManager.destroy()
        ttsManager.shutdown()
        super.onDestroy()
    }

    private fun handleTalkClick() {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            startSpeechInput()
        } else {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startSpeechInput() {
        val state = viewModel.uiState.value
        if (state.isListening || state.isLoading) {
            return
        }

        speechInputManager.startListening()
    }

    private fun handleTtsToggle(enabled: Boolean) {
        viewModel.setTtsEnabled(enabled)
        if (!enabled) {
            ttsManager.stop()
        }
    }

    private fun handleRetryClick() {
        viewModel.retryLastMessage(::handleAgentSpeech)
    }

    private fun handleSendTypedMessage() {
        viewModel.sendTypedMessage(::handleAgentSpeech)
    }

    private fun handleSpeakAgainClick() {
        ttsManager.speak(viewModel.uiState.value.agentReply)
    }

    private fun handleAgentSpeech(text: String) {
        if (viewModel.uiState.value.isHandsFreeSessionActive) {
            ttsManager.speak(
                text = text,
                onStart = viewModel::onSpeakingStarted,
                onDone = {
                    viewModel.onSpeakingFinished()
                    scheduleHandsFreeRestart()
                }
            )
        } else {
            ttsManager.speak(
                text = text,
                onStart = viewModel::onSpeakingStarted,
                onDone = viewModel::onSpeakingFinished
            )
        }
    }

    private fun handleStartHandsFreeSession() {
        viewModel.startHandsFreeSession()
        scheduleHandsFreeTimeout()
        handleTalkClick()
    }

    private fun handleStopHandsFreeSession() {
        stopHandsFreeSession()
    }

    private fun handleInterruptClick() {
        if (!viewModel.uiState.value.isHandsFreeSessionActive) {
            return
        }

        handsFreeHandler.removeCallbacks(handsFreeRestartRunnable)
        ttsManager.stop()
        viewModel.onSpeakingFinished()
        viewModel.onHandsFreeWaitingForNextTurn()
        startSpeechInput()
    }

    private fun handleExitDrivingMode() {
        if (viewModel.uiState.value.isHandsFreeSessionActive) {
            stopHandsFreeSession()
        }
        viewModel.closeDrivingMode()
    }

    private fun scheduleHandsFreeRestart() {
        handsFreeHandler.removeCallbacks(handsFreeRestartRunnable)
        handsFreeHandler.postDelayed(handsFreeRestartRunnable, HANDS_FREE_RESTART_DELAY_MS)
    }

    private fun scheduleHandsFreeTimeout() {
        handsFreeHandler.removeCallbacks(handsFreeTimeoutRunnable)
        handsFreeHandler.postDelayed(handsFreeTimeoutRunnable, HANDS_FREE_TIMEOUT_MS)
    }

    private fun maybeStartHandsFreeListening() {
        val state = viewModel.uiState.value
        if (
            state.isHandsFreeSessionActive &&
            state.isDrivingMode &&
            !state.isListening &&
            !state.isLoading
        ) {
            viewModel.onHandsFreeWaitingForNextTurn()
            startSpeechInput()
        }
    }

    private fun stopHandsFreeSession(reason: String = "Stopped") {
        handsFreeHandler.removeCallbacks(handsFreeRestartRunnable)
        handsFreeHandler.removeCallbacks(handsFreeTimeoutRunnable)
        ttsManager.stop()
        speechInputManager.cancelListening()
        viewModel.stopHandsFreeSession(reason)
    }

    private fun applyDrivingWindowFlags(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun handleMockTestPrompt() {
        viewModel.sendTestPrompt(ConnectorType.Mock, ::handleAgentSpeech)
    }

    private fun handleOpenClawTestPrompt() {
        viewModel.sendTestPrompt(ConnectorType.OpenClaw, ::handleAgentSpeech)
    }

    private fun handleShowDrivingNotification() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        showDrivingShortcutNotification()
    }

    private fun showDrivingShortcutNotification() {
        shortcutNotifier.showDrivingShortcut()
        viewModel.onShortcutNotificationShown()
    }

    private fun handleHideDrivingNotification() {
        shortcutNotifier.hideDrivingShortcut()
        viewModel.onShortcutNotificationHidden()
    }

    private fun handleShortcutIntent(intent: Intent?) {
        when (intent?.action) {
            VoiceShortcutNotifier.ACTION_OPEN_DRIVING -> {
                viewModel.openDrivingMode()
            }

            VoiceShortcutNotifier.ACTION_START_LISTENING -> {
                viewModel.openDrivingMode()
                handleStartHandsFreeSession()
            }
        }
    }

    companion object {
        private const val HANDS_FREE_RESTART_DELAY_MS = 650L
        private const val HANDS_FREE_TIMEOUT_MS = 10 * 60 * 1000L
    }
}
