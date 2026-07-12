package com.agentvoice.app

import com.agentvoice.app.model.AgentPresentation
import com.agentvoice.app.ui.presentation.CompanionPhase
import com.agentvoice.app.ui.presentation.resolveCompanionVisualState
import org.junit.Assert.assertEquals
import org.junit.Test

class CompanionVisualStateTest {
    @Test
    fun listeningAndSpeakingOverrideReplyMood() {
        val happy = AgentPresentation(
            mood = "happy",
            intensity = 0.8f,
            state = "success",
            motionHint = "celebrate"
        )

        assertEquals(
            CompanionPhase.Listening,
            resolveCompanionVisualState(happy, isListening = true, isSpeaking = true, isActive = true).phase
        )
        assertEquals(
            CompanionPhase.Speaking,
            resolveCompanionVisualState(happy, isListening = false, isSpeaking = true, isActive = true).phase
        )
    }

    @Test
    fun presentationMapsToDistinctExpressionPhases() {
        assertEquals(
            CompanionPhase.Thinking,
            resolveCompanionVisualState(
                AgentPresentation(state = "tool", motionHint = "think"),
                isListening = false,
                isSpeaking = false,
                isActive = true
            ).phase
        )
        assertEquals(
            CompanionPhase.Concerned,
            resolveCompanionVisualState(
                AgentPresentation(mood = "concerned", state = "error", motionHint = "reassure"),
                isListening = false,
                isSpeaking = false,
                isActive = false
            ).phase
        )
        assertEquals(
            CompanionPhase.Happy,
            resolveCompanionVisualState(
                AgentPresentation(mood = "excited", state = "success", motionHint = "celebrate"),
                isListening = false,
                isSpeaking = false,
                isActive = false
            ).phase
        )
    }
}
