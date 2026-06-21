package com.agentvoice.app

import com.agentvoice.app.model.ConnectorType
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectorTypeTest {
    @Test
    fun mapsKnownValues() {
        assertEquals(ConnectorType.Hermes, ConnectorType.fromValue("hermes"))
        assertEquals(ConnectorType.OpenClaw, ConnectorType.fromValue("openclaw"))
    }

    @Test
    fun supportsHermesFallbackForInvalidStoredValues() {
        assertEquals(ConnectorType.Mock, ConnectorType.fromValue("not-real"))
        assertEquals(
            ConnectorType.Hermes,
            ConnectorType.fromValue("not-real", fallback = ConnectorType.Hermes)
        )
    }
}
