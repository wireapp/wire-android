/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui

import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.WireSessionId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WireActivitySessionLifecycleTest {

    @Test
    fun givenSessionErrorBlocksTheUi_whenCheckingSessionGraphInvalidation_thenGraphIsInvalidated() {
        assertEquals(
            true,
            shouldInvalidateWireActivitySessionGraph(
                isUserUiBlocked = true,
                sessionTransitionReason = null,
            ),
        )
    }

    @Test
    fun givenSelfLogoutIsInProgress_whenCheckingSessionGraphInvalidation_thenGraphIsInvalidated() {
        assertEquals(
            true,
            shouldInvalidateWireActivitySessionGraph(
                isUserUiBlocked = false,
                sessionTransitionReason = SessionTransitionReason.SELF_LOGOUT,
            ),
        )
    }

    @Test
    fun givenSessionIsActive_whenCheckingSessionGraphInvalidation_thenGraphIsRetained() {
        assertEquals(
            false,
            shouldInvalidateWireActivitySessionGraph(
                isUserUiBlocked = false,
                sessionTransitionReason = null,
            ),
        )
    }

    @Test
    fun givenTypedSession_whenTearingDown_thenOwnerAndGraphAreClearedInOrderForThatSession() {
        val sessionId = WireSessionId("route-user", "wire.test")
        val expectedUserId = UserId("route-user", "wire.test")
        val events = mutableListOf<String>()

        teardownWireActivitySession(
            sessionId = sessionId,
            markInvalidating = { events += "invalidating:$it" },
            clearOwner = { events += "clear:$it" },
            markRemoved = { events += "removed:$it" },
        )

        assertEquals(
            listOf(
                "invalidating:$expectedUserId",
                "clear:session:route-user@wire.test",
                "removed:$expectedUserId",
            ),
            events,
        )
    }
}
