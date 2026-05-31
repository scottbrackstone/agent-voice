package com.agentvoice.app.model

data class AgentResponse(
    val reply: String,
    val status: AgentStatus,
    val mode: AgentMode,
    val queuedActions: List<QueuedAction>,
    val warnings: List<String>,
    val connector: ConnectorType,
    val requestId: String
)

enum class AgentStatus(val wireValue: String) {
    Completed("completed"),
    Queued("queued"),
    Failed("failed"),
    Partial("partial");

    companion object {
        fun fromWireValue(value: String): AgentStatus =
            entries.firstOrNull { it.wireValue == value } ?: Failed
    }
}

enum class ConnectorType(val value: String, val label: String) {
    Mock("mock", "mock"),
    OpenClaw("openclaw", "OpenClaw"),
    Relay("relay", "AgentVoice Relay"),
    Webhook("webhook", "webhook"),
    Hermes("hermes", "Hermes");

    companion object {
        fun fromValue(value: String): ConnectorType =
            entries.firstOrNull { it.value == value } ?: Mock
    }
}
