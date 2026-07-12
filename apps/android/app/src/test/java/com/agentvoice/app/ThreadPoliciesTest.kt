package com.agentvoice.app

import com.agentvoice.app.model.ThreadCreationPolicy
import com.agentvoice.app.model.ThreadTitlePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadPoliciesTest {
    @Test
    fun `create debounce ignores taps while a request is running`() {
        assertNull(
            ThreadCreationPolicy.requestId(
                isCreating = true,
                pendingRequestId = "stable-id",
                generatedRequestId = "new-id"
            )
        )
    }

    @Test
    fun `create retry reuses the pending client id`() {
        assertEquals(
            "stable-id",
            ThreadCreationPolicy.requestId(
                isCreating = false,
                pendingRequestId = "stable-id",
                generatedRequestId = "new-id"
            )
        )
    }

    @Test
    fun `auto title candidates are short unique and conflict safe`() {
        val candidates = ThreadTitlePolicy.candidates(
            text = "  A very long   first message about the neon harbour navigation system  ",
            threadId = "agentvoice-9876",
            fallback = "New chat"
        )

        assertEquals(candidates.size, candidates.distinct().size)
        assertTrue(candidates.all { it.length <= ThreadTitlePolicy.MAX_LENGTH })
        assertTrue(candidates[1].endsWith(" (2)"))
        assertTrue(candidates.last().endsWith(" 9876"))
    }
}
