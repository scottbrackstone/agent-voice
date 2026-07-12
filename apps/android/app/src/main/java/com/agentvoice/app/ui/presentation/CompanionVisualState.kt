package com.agentvoice.app.ui.presentation

import com.agentvoice.app.model.AgentPresentation

enum class CompanionPhase {
    Idle,
    Listening,
    Thinking,
    Speaking,
    Happy,
    Concerned
}

data class CompanionVisualState(
    val phase: CompanionPhase,
    val mood: String,
    val intensity: Float
)

fun resolveCompanionVisualState(
    presentation: AgentPresentation,
    isListening: Boolean,
    isSpeaking: Boolean,
    isActive: Boolean
): CompanionVisualState {
    val phase = when {
        isListening -> CompanionPhase.Listening
        isSpeaking -> CompanionPhase.Speaking
        presentation.state == "thinking" || presentation.state == "tool" -> CompanionPhase.Thinking
        presentation.state == "speaking" -> CompanionPhase.Speaking
        presentation.state == "error" ||
            presentation.mood == "concerned" ||
            presentation.motionHint == "reassure" -> CompanionPhase.Concerned
        presentation.mood in setOf("happy", "excited", "playful") ||
            presentation.motionHint == "celebrate" -> CompanionPhase.Happy
        isActive -> CompanionPhase.Thinking
        else -> CompanionPhase.Idle
    }

    return CompanionVisualState(
        phase = phase,
        mood = presentation.mood,
        intensity = presentation.intensity.coerceIn(0f, 1f)
    )
}
