package com.agentvoice.app

import com.agentvoice.app.voice.VoiceClipRecorder
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceClipRecorderTest {
    @Test
    fun defaultMaxDurationAllowsSixtySecondClips() {
        assertEquals(60_000, VoiceClipRecorder.DEFAULT_MAX_DURATION_MS)
    }
}
