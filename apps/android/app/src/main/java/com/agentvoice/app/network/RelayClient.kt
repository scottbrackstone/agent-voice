package com.agentvoice.app.network

import com.agentvoice.app.BuildConfig
import com.agentvoice.app.model.AgentMode
import com.agentvoice.app.model.AgentPresentation
import com.agentvoice.app.model.AgentResponse
import com.agentvoice.app.model.AgentStatus
import com.agentvoice.app.model.ConnectorType
import com.agentvoice.app.model.CanonicalMessage
import com.agentvoice.app.model.CanonicalThread
import com.agentvoice.app.model.MessageRunSnapshot
import com.agentvoice.app.model.MessageRunStart
import com.agentvoice.app.model.QueuedAction
import com.agentvoice.app.model.QueuedActionStatus
import com.agentvoice.app.model.QueuedActionType
import com.agentvoice.app.model.RelayInboxMessage
import com.agentvoice.app.model.RunStreamEvent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.patch
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import io.ktor.utils.io.readUTF8Line
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.time.Instant
import java.util.concurrent.TimeUnit

class RelayClient(
    private val client: HttpClient = HttpClient(OkHttp) {
        engine {
            config {
                retryOnConnectionFailure(true)
                connectTimeout(12, TimeUnit.SECONDS)
                readTimeout(45, TimeUnit.SECONDS)
                writeTimeout(30, TimeUnit.SECONDS)
            }
        }
        defaultRequest {
            if (BuildConfig.RELAY_TOKEN.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer ${BuildConfig.RELAY_TOKEN}")
            }
        }
    }
) {
    suspend fun sendMessage(
        backendUrl: String,
        agent: ConnectorType,
        message: String,
        mode: AgentMode,
        conversationId: String? = null
    ): Result<AgentResponse> {
        return try {
            Result.success(
                withTimeout(SEND_TIMEOUT_MS) {
                    val baseUrl = normalizeBackendUrl(backendUrl)
                    val requestBody = JSONObject()
                        .put("agent", agent.value)
                        .put("message", message)
                        .put("mode", mode.wireValue)
                        .apply {
                            val cleanedConversationId = conversationId?.trim()
                            if (!cleanedConversationId.isNullOrBlank()) {
                                put("conversationId", cleanedConversationId)
                            }
                        }
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
        } catch (error: TimeoutCancellationException) {
            Result.failure(RelayClientException("Timed out waiting for AgentVoice relay."))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(toRelayError(error))
        }
    }

    suspend fun testConnection(backendUrl: String): Result<String> {
        return try {
            Result.success(
                withTimeout(8_000) {
                    val baseUrl = normalizeBackendUrl(backendUrl)
                    val response = client.get("$baseUrl/health")
                    val responseBody = response.body<String>()

                    if (!response.status.isSuccess()) {
                        throw RelayClientException(
                            "Relay health check returned HTTP ${response.status.value}: ${responseBody.take(240)}"
                        )
                    }

                    parseHealthResponse(responseBody)
                }
            )
        } catch (error: TimeoutCancellationException) {
            Result.failure(RelayClientException("Timed out checking AgentVoice relay."))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(toRelayError(error))
        }
    }

    suspend fun fetchCommandCatalog(backendUrl: String): Result<String> {
        return try {
            Result.success(
                withTimeout(COMMAND_CATALOG_TIMEOUT_MS) {
                    val baseUrl = normalizeBackendUrl(backendUrl)
                    val response = client.get("$baseUrl/api/commands")
                    val responseBody = response.body<String>()

                    if (!response.status.isSuccess()) {
                        throw RelayClientException(
                            "Command catalog returned HTTP ${response.status.value}: ${responseBody.take(240)}"
                        )
                    }

                    responseBody
                }
            )
        } catch (error: TimeoutCancellationException) {
            Result.failure(RelayClientException("Timed out loading the Hermes command catalog."))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(toRelayError(error))
        }
    }

    suspend fun fetchPendingMessages(
        backendUrl: String,
        conversationId: String
    ): Result<List<RelayInboxMessage>> {
        return try {
            Result.success(
                withTimeout(INBOX_TIMEOUT_MS) {
                    val baseUrl = normalizeBackendUrl(backendUrl)
                    val encodedConversationId = encodeQuery(conversationId)
                    val response = client.get(
                        "$baseUrl/api/messages?conversationId=$encodedConversationId&limit=100"
                    )
                    val responseBody = response.body<String>()

                    if (!response.status.isSuccess()) {
                        throw RelayClientException(
                            "Inbox returned HTTP ${response.status.value}: ${responseBody.take(240)}"
                        )
                    }

                    parseInboxResponse(responseBody)
                }
            )
        } catch (error: TimeoutCancellationException) {
            Result.failure(RelayClientException("Timed out checking AgentVoice inbox."))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(toRelayError(error))
        }
    }

    suspend fun acknowledgeMessage(
        backendUrl: String,
        messageId: String
    ): Result<Unit> {
        return try {
            Result.success(
                withTimeout(INBOX_TIMEOUT_MS) {
                    val baseUrl = normalizeBackendUrl(backendUrl)
                    val encodedMessageId = encodePath(messageId)
                    val response = client.post("$baseUrl/api/messages/$encodedMessageId/ack")
                    val responseBody = response.body<String>()

                    if (!response.status.isSuccess()) {
                        throw RelayClientException(
                            "Inbox ack returned HTTP ${response.status.value}: ${responseBody.take(240)}"
                        )
                    }
                }
            )
        } catch (error: TimeoutCancellationException) {
            Result.failure(RelayClientException("Timed out acknowledging AgentVoice inbox message."))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(toRelayError(error))
        }
    }

    suspend fun createCanonicalThread(
        backendUrl: String,
        clientThreadId: String,
        title: String? = null
    ): Result<CanonicalThread> = jsonRequest(
        timeoutMs = THREAD_TIMEOUT_MS,
        timeoutMessage = "Timed out creating Hermes chat."
    ) {
        val response = client.post("${normalizeBackendUrl(backendUrl)}/api/threads") {
            contentType(ContentType.Application.Json)
            setBody(
                JSONObject()
                    .put("clientThreadId", clientThreadId)
                    .apply {
                        title?.trim()?.takeIf { it.isNotBlank() }?.let { put("title", it) }
                    }
                    .toString()
            )
        }
        parseCanonicalThread(requireSuccessBody(response.status.value, response.body()))
    }

    suspend fun fetchCanonicalThreads(backendUrl: String): Result<List<CanonicalThread>> =
        jsonRequest(
            timeoutMs = THREAD_TIMEOUT_MS,
            timeoutMessage = "Timed out loading Hermes chats."
        ) {
            val response = client.get("${normalizeBackendUrl(backendUrl)}/api/threads")
            parseCanonicalThreads(requireSuccessBody(response.status.value, response.body()))
        }

    suspend fun renameCanonicalThread(
        backendUrl: String,
        threadId: String,
        title: String
    ): Result<CanonicalThread> = jsonRequest(
        timeoutMs = THREAD_TIMEOUT_MS,
        timeoutMessage = "Timed out renaming Hermes chat."
    ) {
        val response = client.patch(
            "${normalizeBackendUrl(backendUrl)}/api/threads/${encodePath(threadId)}"
        ) {
            contentType(ContentType.Application.Json)
            setBody(JSONObject().put("title", title).toString())
        }
        parseCanonicalThread(requireSuccessBody(response.status.value, response.body()))
    }

    suspend fun fetchCanonicalMessages(
        backendUrl: String,
        threadId: String,
        limit: Int = 100
    ): Result<List<CanonicalMessage>> = jsonRequest(
        timeoutMs = THREAD_TIMEOUT_MS,
        timeoutMessage = "Timed out loading Hermes messages."
    ) {
        val response = client.get(
            "${normalizeBackendUrl(backendUrl)}/api/threads/${encodePath(threadId)}/messages" +
                "?limit=${limit.coerceIn(1, 100)}"
        )
        parseCanonicalMessages(
            body = requireSuccessBody(response.status.value, response.body()),
            threadId = threadId
        )
    }

    suspend fun startMessageRun(
        backendUrl: String,
        threadId: String,
        clientMessageId: String,
        content: String,
        mode: AgentMode,
        inputType: String
    ): Result<MessageRunStart> = jsonRequest(
        timeoutMs = THREAD_TIMEOUT_MS,
        timeoutMessage = "Timed out starting Hermes response."
    ) {
        val response = client.post(
            "${normalizeBackendUrl(backendUrl)}/api/threads/${encodePath(threadId)}/messages"
        ) {
            contentType(ContentType.Application.Json)
            setBody(
                JSONObject()
                    .put("clientMessageId", clientMessageId)
                    .put("content", content)
                    .put("mode", mode.wireValue)
                    .put("inputType", inputType)
                    .toString()
            )
        }
        parseRunStart(requireSuccessBody(response.status.value, response.body()))
    }

    suspend fun fetchMessageRun(
        backendUrl: String,
        runId: String
    ): Result<MessageRunSnapshot> = jsonRequest(
        timeoutMs = INBOX_TIMEOUT_MS,
        timeoutMessage = "Timed out checking Hermes response."
    ) {
        val response = client.get(
            "${normalizeBackendUrl(backendUrl)}/api/runs/${encodePath(runId)}"
        )
        parseRunSnapshot(requireSuccessBody(response.status.value, response.body()))
    }

    suspend fun streamRunEvents(
        backendUrl: String,
        runId: String,
        onEvent: (RunStreamEvent) -> Unit
    ): Result<Unit> {
        return try {
            client.prepareGet(
                "${normalizeBackendUrl(backendUrl)}/api/runs/${encodePath(runId)}/events"
            ).execute { response ->
                if (!response.status.isSuccess()) {
                    throw RelayClientException(
                        "Run stream returned HTTP ${response.status.value}: ${response.body<String>().take(240)}"
                    )
                }
                val channel = response.bodyAsChannel()
                var eventName = "message"
                val dataLines = mutableListOf<String>()
                var finished = false
                while (!finished && !channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    when {
                        line.startsWith("event:") -> eventName = line.substringAfter("event:").trim()
                        line.startsWith("data:") -> dataLines += line.substringAfter("data:").trimStart()
                        line.isBlank() -> {
                            if (dataLines.isNotEmpty() && eventName != "heartbeat") {
                                val event = parseRunEvent(eventName, dataLines.joinToString("\n"))
                                if (event != null) {
                                    onEvent(event)
                                    finished = event is RunStreamEvent.Completed ||
                                        event is RunStreamEvent.Failed ||
                                        event is RunStreamEvent.Cancelled
                                }
                            }
                            eventName = "message"
                            dataLines.clear()
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(toRelayError(error))
        }
    }

    suspend fun registerDevice(
        backendUrl: String,
        installationId: String,
        fcmToken: String
    ): Result<Unit> = jsonRequest(
        timeoutMs = INBOX_TIMEOUT_MS,
        timeoutMessage = "Timed out registering notifications."
    ) {
        val response = client.post("${normalizeBackendUrl(backendUrl)}/api/devices/register") {
            contentType(ContentType.Application.Json)
            setBody(
                JSONObject()
                    .put("installationId", installationId)
                    .put("fcmToken", fcmToken)
                    .put("platform", "android")
                    .toString()
            )
        }
        requireSuccessBody(response.status.value, response.body<String>())
        Unit
    }

    suspend fun transcribeAudio(
        backendUrl: String,
        audioBytes: ByteArray,
        contentType: String = "audio/mp4"
    ): Result<String> {
        return try {
            Result.success(
                withTimeout(TRANSCRIBE_TIMEOUT_MS) {
                    val baseUrl = normalizeBackendUrl(backendUrl)
                    val response = client.post("$baseUrl/api/transcribe") {
                        header(HttpHeaders.ContentType, contentType)
                        setBody(audioBytes)
                    }
                    val responseBody = response.body<String>()

                    if (!response.status.isSuccess()) {
                        throw RelayClientException(
                            "Transcription returned HTTP ${response.status.value}: ${
                                responseBody.take(240)
                            }"
                        )
                    }

                    parseTranscriptionResponse(responseBody)
                }
            )
        } catch (error: TimeoutCancellationException) {
            Result.failure(RelayClientException("Timed out waiting for Voxtral transcription."))
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

    private fun parseHealthResponse(body: String): String {
        try {
            val json = JSONObject(body)
            if (!json.optBoolean("ok", false)) {
                throw RelayClientException("Relay health check did not report ready.")
            }

            val service = json.optString("service", "agentvoice-relay")
            return "$service is reachable."
        } catch (error: JSONException) {
            throw RelayClientException("Relay health response was not in the expected shape.")
        }
    }

    private fun parseTranscriptionResponse(body: String): String {
        try {
            val json = JSONObject(body)
            return json.optString("transcript").trim()
        } catch (error: JSONException) {
            throw RelayClientException("Transcription response was not in the expected shape.")
        }
    }

    private fun parseInboxResponse(body: String): List<RelayInboxMessage> {
        try {
            val json = JSONObject(body)
            val conversationId = json.getString("conversationId")
            val messages = json.optJSONArray("messages") ?: json.optJSONArray("pending")
            return (0 until (messages?.length() ?: 0)).map { index ->
                val item = messages!!.getJSONObject(index)
                RelayInboxMessage(
                    id = item.getString("id"),
                    conversationId = conversationId,
                    direction = item.optString("direction", "outgoing"),
                    content = item.getString("content"),
                    source = item.optString("source", "hermes"),
                    status = item.optString("status", "pending"),
                    timestampMillis = parseIsoTimestampMillis(item.getString("timestamp")),
                    metadataSummary = metadataSummary(item.optJSONObject("metadata"))
                )
            }
        } catch (error: JSONException) {
            throw RelayClientException("Inbox response was not in the expected shape.")
        }
    }

    private suspend fun <T> jsonRequest(
        timeoutMs: Long,
        timeoutMessage: String,
        block: suspend () -> T
    ): Result<T> = try {
        Result.success(withTimeout(timeoutMs) { block() })
    } catch (error: TimeoutCancellationException) {
        Result.failure(RelayClientException(timeoutMessage))
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(toRelayError(error))
    }

    private fun requireSuccessBody(status: Int, body: String): String {
        if (status !in 200..299) {
            val errorBody = runCatching { JSONObject(body) }.getOrNull()
            val message = errorBody
                ?.optString("detail")
                ?.takeIf { it.isNotBlank() }
                ?: errorBody?.optString("error")?.takeIf { it.isNotBlank() }
            val requestId = errorBody?.optString("requestId")?.takeIf { it.isNotBlank() }
            val code = errorBody?.optString("code")?.takeIf { it.isNotBlank() }
            val displayMessage = buildString {
                append(message ?: "Relay returned HTTP $status: ${body.take(240)}")
                requestId?.let { append(" (request $it)") }
            }
            throw RelayClientException(
                message = displayMessage,
                statusCode = status,
                requestId = requestId,
                code = code
            )
        }
        return body
    }

    private fun parseCanonicalThread(body: String): CanonicalThread {
        val json = JSONObject(body)
        val now = System.currentTimeMillis()
        return CanonicalThread(
            id = json.getString("id"),
            title = json.optString("title", "New chat"),
            isUntitled = json.optBoolean("isUntitled", false),
            duplicate = json.optBoolean("duplicate", false),
            createdAtMillis = parseFlexibleTimestamp(json.opt("createdAt"), now),
            updatedAtMillis = parseFlexibleTimestamp(json.opt("updatedAt"), now),
            messageCount = json.optInt("messageCount", 0)
        )
    }

    private fun parseCanonicalThreads(body: String): List<CanonicalThread> {
        val array = JSONObject(body).optJSONArray("threads") ?: return emptyList()
        return (0 until array.length()).map { parseCanonicalThread(array.getJSONObject(it).toString()) }
    }

    private fun parseCanonicalMessages(body: String, threadId: String): List<CanonicalMessage> {
        val array = JSONObject(body).optJSONArray("messages") ?: return emptyList()
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            CanonicalMessage(
                id = item.getString("id"),
                threadId = threadId,
                role = item.optString("role", "system"),
                content = item.optString("content"),
                timestampMillis = parseFlexibleTimestamp(item.opt("timestamp"), System.currentTimeMillis()),
                finishReason = item.optString("finishReason").ifBlank { null },
                toolName = item.optString("toolName").ifBlank { null }
            )
        }
    }

    private fun parseRunStart(body: String): MessageRunStart {
        val json = JSONObject(body)
        return MessageRunStart(
            runId = json.getString("runId"),
            threadId = json.getString("threadId"),
            clientMessageId = json.getString("clientMessageId"),
            status = json.getString("status"),
            duplicate = json.optBoolean("duplicate", false)
        )
    }

    private fun parseRunSnapshot(body: String): MessageRunSnapshot {
        val json = JSONObject(body)
        return MessageRunSnapshot(
            id = json.getString("id"),
            sessionId = json.getString("sessionId"),
            clientMessageId = json.getString("clientMessageId"),
            status = json.getString("status"),
            replyText = json.optString("replyText"),
            error = json.optString("error").ifBlank { null },
            presentation = parsePresentation(json.optJSONObject("presentation"))
        )
    }

    private fun parseRunEvent(eventName: String, body: String): RunStreamEvent? {
        val json = JSONObject(body)
        return when (eventName) {
            "run.started" -> RunStreamEvent.Started(json.optString("runId"))
            "reply.delta" -> RunStreamEvent.Delta(json.optString("text"))
            "tool.progress" -> RunStreamEvent.ToolProgress(
                label = json.optString("label", json.optString("tool", "Working")),
                status = json.optString("status", "running")
            )
            "presentation" -> RunStreamEvent.Presentation(
                parsePresentation(json.optJSONObject("presentation"))
            )
            "reply.completed" -> RunStreamEvent.Completed(
                text = json.optString("text"),
                presentation = parsePresentation(json.optJSONObject("presentation"))
            )
            "run.failed" -> RunStreamEvent.Failed(json.optString("error", "Hermes run failed."))
            "run.cancelled" -> RunStreamEvent.Cancelled
            else -> null
        }
    }

    private fun parsePresentation(json: JSONObject?): AgentPresentation = AgentPresentation(
        mood = json?.optString("mood", "neutral") ?: "neutral",
        intensity = (json?.optDouble("intensity", 0.0) ?: 0.0).toFloat().coerceIn(0f, 1f),
        state = json?.optString("state", "idle") ?: "idle",
        motionHint = json?.optString("motionHint", "idle") ?: "idle"
    )

    private fun parseFlexibleTimestamp(value: Any?, fallback: Long): Long = when (value) {
        is Number -> if (value.toDouble() < 10_000_000_000.0) {
            (value.toDouble() * 1000).toLong()
        } else {
            value.toLong()
        }
        is String -> runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(fallback)
        else -> fallback
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
        when {
            error is RelayClientException -> error
            error is SocketTimeoutException -> RelayClientException(
                "Timed out waiting for AgentVoice relay. Check the relay, Wi-Fi, or Tailscale connection."
            )
            error.message?.contains("timeout", ignoreCase = true) == true -> RelayClientException(
                "Timed out waiting for AgentVoice relay. Check the relay, Wi-Fi, or Tailscale connection."
            )
            else -> RelayClientException(error.message ?: "Unable to reach AgentVoice relay.")
        }

    private fun parseIsoTimestampMillis(value: String): Long =
        runCatching { Instant.parse(value).toEpochMilli() }
            .getOrDefault(System.currentTimeMillis())

    private fun metadataSummary(json: JSONObject?): String {
        if (json == null || json.length() == 0) {
            return ""
        }

        val priorityKeys = listOf("cronJobName", "cronJobId", "scheduledAt", "requestId")
        val parts = priorityKeys.mapNotNull { key ->
            json.optString(key).takeIf { it.isNotBlank() }?.let { value -> "$key: $value" }
        }
        return parts.ifEmpty {
            json.keys().asSequence().take(4).map { key ->
                "$key: ${json.optString(key)}"
            }.toList()
        }.joinToString(separator = "\n")
    }

    private fun encodeQuery(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun encodePath(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    companion object {
        private const val SEND_TIMEOUT_MS = 45_000L
        private const val COMMAND_CATALOG_TIMEOUT_MS = 8_000L
        private const val INBOX_TIMEOUT_MS = 8_000L
        private const val TRANSCRIBE_TIMEOUT_MS = 90_000L
        private const val THREAD_TIMEOUT_MS = 15_000L
    }
}

class RelayClientException(
    message: String,
    val statusCode: Int? = null,
    val requestId: String? = null,
    val code: String? = null
) : Exception(message)
