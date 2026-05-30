package com.agentvoice.app

import com.agentvoice.app.model.AgentMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AgentModeTest {
    @Test
    fun mapsWireValues() {
        assertEquals(AgentMode.CaptureOnly, AgentMode.fromWireValue("capture_only"))
        assertEquals(AgentMode.ReviewRequired, AgentMode.fromWireValue("review_required"))
        assertEquals(AgentMode.Normal, AgentMode.fromWireValue("not-real"))
    }
}

