package com.agentvoice.app.model

enum class AgentMode(val wireValue: String, val label: String) {
    Normal("normal", "normal"),
    Mobile("mobile", "mobile"),
    CaptureOnly("capture_only", "capture only"),
    ReviewRequired("review_required", "review required");

    val showsCaptureNotice: Boolean
        get() = this == Mobile || this == CaptureOnly || this == ReviewRequired

    companion object {
        fun fromWireValue(value: String): AgentMode =
            entries.firstOrNull { it.wireValue == value } ?: Normal
    }
}
