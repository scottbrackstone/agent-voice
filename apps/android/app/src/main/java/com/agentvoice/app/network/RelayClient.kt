package com.agentvoice.app.network

import com.agentvoice.app.model.AgentMode
import com.agentvoice.app.model.AgentResponse
import com.agentvoice.app.model.AgentStatus
import com.agentvoice.app.model.ConnectorType
import com.agentvoice.app.model.QueuedAction
import com.agentvoice.app.model.QueuedActionStatus
import com.agentvoice.app.model.QueuedActionType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URI

class RelayClient(
    private val client: HttpClient = HttpClient(OkHttp)
) {
    suspend fun sendMessage(
        backendUrl: String,
        agent: ConnectorType,
        message: String,
        mode: AgentMode
    ): Result<AgentResponse> {
        return try {
            Result.success(
                withTimeout(15_000) {
                    val baseUrl = normalizeBackendUrl(backendUrl)
                    val requestBody = JSONObject()
                        .put("agent", agent.value)
                        .put("message", message)
                        .put("mode", mode.wireValue)
                        .toString()

                    val response = client.post("$baseUrl/api/message") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                        setBody(requestBody)
                    }
                    val responseBody = response.body<String>()

                    if (!response.status.isSuccess()) {
                        throw RelayClientException(
                            "Relay returned HTTP ${response.status.value}: ${responseBody.take(240)}"
                        )
                    }

                    parseAgentResponse(responseBody)
                }
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(toRelayError(error))
        }
    }

    fun close() {
        client.close()
    }

    private fun normalizeBackendUrl(url: String): String {
        val trimmed = url.trim().removeSuffix("/")
        if (trimmed.isBlank()) {
            throw RelayClientException("Set a backend URL before sending a message.")
        }

        val parsed = try {
            URI(trimmed)
        } catch (error: Exception) {
            throw RelayClientException("Backend URL is not valid.")
        }

        if (parsed.scheme !in setOf("http", "https") || parsed.host.isNullOrBlank()) {
            throw RelayClientException("Backend URL must start with http:// or https:// and include a host.")
        }

        return trimmed
    }

    private fun parseAgentResponse(body: String): AgentResponse {
        try {
            val json = JSONObject(body)
            return AgentResponse(
                reply = json.getString("reply"),
                status = AgentStatus.fromWireValue(json.getString("status")),
                mode = AgentMode.fromWireValue(json.getString("mode")),
                queuedActions = json.optJSONArray("queuedActions").toQueuedActions(),
                warnings = json.optJSONArray("warnings").toStringList(),
                connector = ConnectorType.fromValue(json.getString("connector")),
                requestId = json.getString("requestId")
            )
        } catch (error: JSONException) {
            throw RelayClientException("Relay response was not in the expected AgentVoice shape.")
        }
    }

    private fun JSONArray?.toQueuedActions(): List<QueuedAction> {
        if (this == null) {
            return emptyList()
        }

        return (0 until length()).map { index ->
            val item = getJSONObject(index)
            QueuedAction(
                id = item.getString("id"),
                type = QueuedActionType.fromWireValue(item.getString("type")),
                summary = item.getString("summary"),
                requiresConfirmation = item.getBoolean("requiresConfirmation"),
                status = QueuedActionStatus.fromWireValue(item.getString("status"))
            )
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) {
            return emptyList()
        }

        return (0 until length()).map { index -> getString(index) }
    }

    private fun toRelayError(error: Exception): Exception =
        if (error is RelayClientException) {
            error
        } else {
            RelayClientException(error.message ?: "Unable to reach AgentVoice relay.")
        }
}

class RelayClientException(message: String) : Exception(message)
