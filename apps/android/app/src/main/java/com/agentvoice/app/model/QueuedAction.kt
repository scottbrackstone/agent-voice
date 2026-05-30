package com.agentvoice.app.model

data class QueuedAction(
    val id: String,
    val type: QueuedActionType,
    val summary: String,
    val requiresConfirmation: Boolean,
    val status: QueuedActionStatus
)

enum class QueuedActionType(val wireValue: String, val label: String) {
    DraftMessage("draft_message", "Draft message"),
    DraftEmail("draft_email", "Draft email"),
    Reminder("reminder", "Reminder"),
    Note("note", "Note"),
    Task("task", "Task"),
    Unknown("unknown", "Unknown");

    companion object {
        fun fromWireValue(value: String): QueuedActionType =
            entries.firstOrNull { it.wireValue == value } ?: Unknown
    }
}

enum class QueuedActionStatus(val wireValue: String) {
    Queued("queued"),
    Confirmed("confirmed"),
    Cancelled("cancelled"),
    Blocked("blocked");

    companion object {
        fun fromWireValue(value: String): QueuedActionStatus =
            entries.firstOrNull { it.wireValue == value } ?: Blocked
    }
}

