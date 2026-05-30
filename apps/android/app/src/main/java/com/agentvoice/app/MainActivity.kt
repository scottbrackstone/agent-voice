package com.agentvoice.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agentvoice.app.ui.MainScreen
import com.agentvoice.app.ui.SettingsScreen
import com.agentvoice.app.ui.theme.AgentVoiceTheme
import com.agentvoice.app.viewmodel.MainViewModel
import com.agentvoice.app.voice.SpeechInputManager
import com.agentvoice.app.voice.TtsManager

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var speechInputManager: SpeechInputManager
    private lateinit var ttsManager: TtsManager

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startSpeechInput()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    viewModel.onTranscriptCaptured(transcript, ttsManager::speak)
                }

                override fun onError(message: String) {
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
                            onAgentSelected = viewModel::selectAgent,
                            onClearHistory = viewModel::clearHistory,
                            onBack = viewModel::closeSettings
                        )
                    } else {
                        MainScreen(
                            uiState = uiState,
                            onModeSelected = viewModel::selectMode,
                            onTalkClick = ::handleTalkClick,
                            onRetryClick = ::handleRetryClick,
                            onHistorySelected = viewModel::selectHistory,
                            onDismissHistory = viewModel::dismissHistoryDetail,
                            onTtsToggle = ::handleTtsToggle,
                            onSettingsClick = viewModel::openSettings
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
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
        speechInputManager.startListening()
    }

    private fun handleTtsToggle(enabled: Boolean) {
        viewModel.setTtsEnabled(enabled)
        if (!enabled) {
            ttsManager.stop()
        }
    }

    private fun handleRetryClick() {
        viewModel.retryLastMessage(ttsManager::speak)
    }
}
