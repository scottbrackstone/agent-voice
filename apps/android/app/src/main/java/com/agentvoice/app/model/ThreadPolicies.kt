package com.agentvoice.app.model

internal object ThreadCreationPolicy {
    fun requestId(
        isCreating: Boolean,
        pendingRequestId: String?,
        generatedRequestId: String
    ): String? = if (isCreating) null else pendingRequestId ?: generatedRequestId
}

internal object ThreadTitlePolicy {
    const val MAX_LENGTH = 42

    fun suggest(text: String, fallback: String): String = text
        .trim()
        .replace(Regex("\\s+"), " ")
        .take(MAX_LENGTH)
        .ifBlank { fallback }

    fun candidates(text: String, threadId: String, fallback: String): List<String> {
        val base = suggest(text, fallback)
        return buildList {
            add(base)
            (2..5).forEach { number -> add(withSuffix(base, " ($number)")) }
            add(withSuffix(base, " ${threadId.takeLast(4)}"))
        }
    }

    private fun withSuffix(base: String, suffix: String): String =
        base.take((MAX_LENGTH - suffix.length).coerceAtLeast(1)).trimEnd() + suffix
}
