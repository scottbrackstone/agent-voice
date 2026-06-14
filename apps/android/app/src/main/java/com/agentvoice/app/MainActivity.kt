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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
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
import com.agentvoice.app.voice.VoiceClipRecorder
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val handsFreeHandler = Handler(Looper.getMainLooper())
    private lateinit var speechInputManager: SpeechInputManager
    private lateinit var shortcutNotifier: VoiceShortcutNotifier
    private lateinit var ttsManager: TtsManager
    private lateinit var voiceClipRecorder: VoiceClipRecorder

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
                        if (isFatalSpeechError(message)) {
                            handsFreeHandler.removeCallbacks(handsFreeRestartRunnable)
                            handsFreeHandler.removeCallbacks(handsFreeTimeoutRunnable)
                            ttsManager.stop()
                            viewModel.onSpeechError(message)
                        } else {
                            viewModel.onHandsFreeRecoverableError(message)
                        }
                        return
                    }
                    viewModel.onSpeechError(message)
                }
            }
        )
        ttsManager = TtsManager(this)
        voiceClipRecorder = VoiceClipRecorder(
            context = this,
            listener = object : VoiceClipRecorder.Listener {
                override fun onRecordingStarted() {
                    viewModel.onVoiceClipRecordingStarted()
                }

                override fun onRecordingFinished(file: File) {
                    handleVoiceClipRecorded(file)
                }

                override fun onRecordingError(message: String) {
                    viewModel.onVoiceClipError(message)
                }
            }
        )

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
                            onSendHermesTestPrompt = ::handleHermesTestPrompt,
                            onShowDrivingNotification = ::handleShowDrivingNotification,
                            onHideDrivingNotification = ::handleHideDrivingNotification,
                            onStartInDrivingModeToggle = viewModel::setStartInDrivingMode,
                            onKeepScreenAwakeToggle = viewModel::setKeepScreenAwakeInDrivingMode,
                            onDrivingAutoSpeakToggle = viewModel::setDrivingAutoSpeak,
                            onDrivingRequireWakeWordToggle = viewModel::setDrivingRequireWakeWord,
                            onDrivingUseVoxtralTranscriptionToggle =
                                viewModel::setDrivingUseVoxtralTranscription,
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

                    LaunchedEffect(uiState.handsFreeRestartSignal) {
                        if (
                            uiState.handsFreeRestartSignal > 0 &&
                            uiState.isHandsFreeSessionActive
                        ) {
                            scheduleHandsFreeRestart(HANDS_FREE_RECOVERY_DELAY_MS)
                        }
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
        voiceClipRecorder.destroy()
        ttsManager.shutdown()
        super.onDestroy()
    }

    private fun handleTalkClick() {
        val state = viewModel.uiState.value
        if (state.isHandsFreeSessionActive && state.isSpeaking) {
            handleInterruptClick()
            return
        }
        if (state.isRecordingVoiceClip) {
            voiceClipRecorder.stop()
            return
        }

        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            startSpeechInput()
        } else {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startSpeechInput() {
        val state = viewModel.uiState.value
        if (
            state.isListening ||
            state.isLoading ||
            state.isRecordingVoiceClip ||
            state.isTranscribingVoiceClip
        ) {
            return
        }

        if (state.isDrivingMode && state.drivingUseVoxtralTranscription) {
            voiceClipRecorder.start()
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
        voiceClipRecorder.cancel()
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
        scheduleHandsFreeRestart(HANDS_FREE_RESTART_DELAY_MS)
    }

    private fun scheduleHandsFreeRestart(delayMs: Long) {
        handsFreeHandler.removeCallbacks(handsFreeRestartRunnable)
        handsFreeHandler.postDelayed(handsFreeRestartRunnable, delayMs)
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
            !state.isLoading &&
            !state.isRecordingVoiceClip &&
            !state.isTranscribingVoiceClip
        ) {
            viewModel.onHandsFreeWaitingForNextTurn()
            startSpeechInput()
        }
    }

    private fun stopHandsFreeSession(reason: String = "Stopped") {
        handsFreeHandler.removeCallbacks(handsFreeRestartRunnable)
        handsFreeHandler.removeCallbacks(handsFreeTimeoutRunnable)
        ttsManager.stop()
        voiceClipRecorder.cancel()
        speechInputManager.cancelListening()
        viewModel.stopHandsFreeSession(reason)
    }

    private fun handleVoiceClipRecorded(file: File) {
        lifecycleScope.launch {
            val audioBytes = runCatching {
                withContext(Dispatchers.IO) {
                    file.readBytes()
                }
            }
            file.delete()

            audioBytes
                .onSuccess { bytes ->
                    viewModel.transcribeVoiceClip(bytes, ::handleAgentSpeech)
                }
                .onFailure { error ->
                    viewModel.onVoiceClipError(error.message ?: "Unable to read voice recording.")
                }
        }
    }

    private fun isFatalSpeechError(message: String): Boolean {
        return message.contains("permission", ignoreCase = true) ||
            message.contains("not available", ignoreCase = true)
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

    private fun handleHermesTestPrompt() {
        viewModel.sendTestPrompt(ConnectorType.Hermes, ::handleAgentSpeech)
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
        private const val HANDS_FREE_RECOVERY_DELAY_MS = 900L
        private const val HANDS_FREE_TIMEOUT_MS = 10 * 60 * 1000L
    }
}
