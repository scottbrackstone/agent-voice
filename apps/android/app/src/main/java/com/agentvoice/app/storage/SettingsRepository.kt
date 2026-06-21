package com.agentvoice.app.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agentvoice.app.model.AgentMode
import com.agentvoice.app.model.ConnectorType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "agentvoice_settings")

data class AppSettings(
    val backendUrl: String = SettingsRepository.DEFAULT_BACKEND_URL,
    val selectedAgent: ConnectorType = SettingsRepository.DEFAULT_SELECTED_AGENT,
    val defaultMode: AgentMode = AgentMode.Normal,
    val ttsEnabled: Boolean = true,
    val startInDrivingMode: Boolean = false,
    val keepScreenAwakeInDrivingMode: Boolean = true,
    val drivingAutoSpeak: Boolean = true,
    val drivingRequireWakeWord: Boolean = false,
    val drivingUseVoxtralTranscription: Boolean = false,
    val drivingMode: AgentMode = AgentMode.Mobile
)

class SettingsRepository(private val context: Context) {
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            backendUrl = preferences[BACKEND_URL] ?: DEFAULT_BACKEND_URL,
            selectedAgent = ConnectorType.fromValue(
                preferences[SELECTED_AGENT] ?: DEFAULT_SELECTED_AGENT.value,
                fallback = DEFAULT_SELECTED_AGENT
            ),
            defaultMode = AgentMode.fromWireValue(
                preferences[DEFAULT_MODE] ?: AgentMode.Normal.wireValue
            ),
            ttsEnabled = preferences[TTS_ENABLED] ?: true,
            startInDrivingMode = preferences[START_IN_DRIVING_MODE] ?: false,
            keepScreenAwakeInDrivingMode = preferences[KEEP_SCREEN_AWAKE_IN_DRIVING_MODE] ?: true,
            drivingAutoSpeak = preferences[DRIVING_AUTO_SPEAK] ?: true,
            drivingRequireWakeWord = preferences[DRIVING_REQUIRE_WAKE_WORD] ?: false,
            drivingUseVoxtralTranscription = preferences[DRIVING_USE_VOXTRAL_TRANSCRIPTION]
                ?: false,
            drivingMode = AgentMode.fromWireValue(
                preferences[DRIVING_MODE] ?: AgentMode.Mobile.wireValue
            )
        )
    }

    suspend fun setBackendUrl(url: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[BACKEND_URL] = url
        }
    }

    suspend fun setSelectedAgent(agent: ConnectorType) {
        context.settingsDataStore.edit { preferences ->
            preferences[SELECTED_AGENT] = agent.value
        }
    }

    suspend fun migrateDefaultAgentToHermes() {
        context.settingsDataStore.edit { preferences ->
            if (preferences[HERMES_DEFAULT_MIGRATION_COMPLETE] == true) {
                return@edit
            }

            val selectedAgent = preferences[SELECTED_AGENT]
            if (
                selectedAgent == null ||
                selectedAgent == ConnectorType.Mock.value ||
                selectedAgent == ConnectorType.OpenClaw.value ||
                ConnectorType.entries.none { it.value == selectedAgent }
            ) {
                preferences[SELECTED_AGENT] = DEFAULT_SELECTED_AGENT.value
            }
            preferences[HERMES_DEFAULT_MIGRATION_COMPLETE] = true
        }
    }

    suspend fun setDefaultMode(mode: AgentMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[DEFAULT_MODE] = mode.wireValue
        }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[TTS_ENABLED] = enabled
        }
    }

    suspend fun setStartInDrivingMode(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[START_IN_DRIVING_MODE] = enabled
        }
    }

    suspend fun setKeepScreenAwakeInDrivingMode(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEEP_SCREEN_AWAKE_IN_DRIVING_MODE] = enabled
        }
    }

    suspend fun setDrivingAutoSpeak(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[DRIVING_AUTO_SPEAK] = enabled
        }
    }

    suspend fun setDrivingRequireWakeWord(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[DRIVING_REQUIRE_WAKE_WORD] = enabled
        }
    }

    suspend fun setDrivingUseVoxtralTranscription(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[DRIVING_USE_VOXTRAL_TRANSCRIPTION] = enabled
        }
    }

    suspend fun setDrivingMode(mode: AgentMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[DRIVING_MODE] = mode.wireValue
        }
    }

    companion object {
        const val DEFAULT_BACKEND_URL = "http://10.0.2.2:3001"
        val DEFAULT_SELECTED_AGENT = ConnectorType.Hermes

        private val BACKEND_URL = stringPreferencesKey("backend_url")
        private val SELECTED_AGENT = stringPreferencesKey("selected_agent")
        private val HERMES_DEFAULT_MIGRATION_COMPLETE =
            booleanPreferencesKey("hermes_default_migration_complete")
        private val DEFAULT_MODE = stringPreferencesKey("default_mode")
        private val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        private val START_IN_DRIVING_MODE = booleanPreferencesKey("start_in_driving_mode")
        private val KEEP_SCREEN_AWAKE_IN_DRIVING_MODE =
            booleanPreferencesKey("keep_screen_awake_in_driving_mode")
        private val DRIVING_AUTO_SPEAK = booleanPreferencesKey("driving_auto_speak")
        private val DRIVING_REQUIRE_WAKE_WORD = booleanPreferencesKey("driving_require_wake_word")
        private val DRIVING_USE_VOXTRAL_TRANSCRIPTION =
            booleanPreferencesKey("driving_use_voxtral_transcription")
        private val DRIVING_MODE = stringPreferencesKey("driving_mode")
    }
}
