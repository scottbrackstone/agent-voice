package com.agentvoice.app.notifications

import com.agentvoice.app.network.RelayClient
import com.agentvoice.app.shortcut.AgentMessageNotifier
import com.agentvoice.app.storage.ConversationRepository
import com.agentvoice.app.storage.InstallationIdRepository
import com.agentvoice.app.storage.SettingsRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AgentVoiceMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        registerToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val threadId = message.data["threadId"] ?: return
        val title = message.notification?.title ?: "Solara"
        val body = message.notification?.body ?: "A new message is ready."
        scope.launch {
            val settings = SettingsRepository(applicationContext).settings.first()
            val relay = RelayClient()
            val repository = ConversationRepository(applicationContext)
            relay.fetchCanonicalThreads(settings.backendUrl).onSuccess { threads ->
                repository.upsertCanonicalThreads(threads)
            }
            relay.fetchCanonicalMessages(settings.backendUrl, threadId).onSuccess { messages ->
                repository.replaceWithCanonicalMessages(threadId, messages)
            }
            relay.close()
            AgentMessageNotifier(applicationContext).showMessage(title, body, threadId)
        }
    }

    private fun registerToken(token: String) {
        scope.launch {
            val settings = SettingsRepository(applicationContext).settings.first()
            val installationId = InstallationIdRepository(applicationContext).getOrCreate()
            RelayClient().also { relay ->
                relay.registerDevice(settings.backendUrl, installationId, token)
                relay.close()
            }
        }
    }
}

