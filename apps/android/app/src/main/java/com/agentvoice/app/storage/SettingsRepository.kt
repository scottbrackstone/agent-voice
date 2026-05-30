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
    val selectedAgent: ConnectorType = ConnectorType.Mock,
    val defaultMode: AgentMode = AgentMode.Normal,
    val ttsEnabled: Boolean = true
)

class SettingsRepository(private val context: Context) {
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            backendUrl = preferences[BACKEND_URL] ?: DEFAULT_BACKEND_URL,
            selectedAgent = ConnectorType.fromValue(
                preferences[SELECTED_AGENT] ?: ConnectorType.Mock.value
            ),
            defaultMode = AgentMode.fromWireValue(
                preferences[DEFAULT_MODE] ?: AgentMode.Normal.wireValue
            ),
            ttsEnabled = preferences[TTS_ENABLED] ?: true
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

    companion object {
        const val DEFAULT_BACKEND_URL = "http://10.0.2.2:3001"

        private val BACKEND_URL = stringPreferencesKey("backend_url")
        private val SELECTED_AGENT = stringPreferencesKey("selected_agent")
        private val DEFAULT_MODE = stringPreferencesKey("default_mode")
        private val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
    }
}
