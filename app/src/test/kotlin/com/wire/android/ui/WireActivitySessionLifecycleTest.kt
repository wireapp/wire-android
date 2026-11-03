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

import com.wire.android.di.metro.AppSessionViewModelGraph
import com.wire.android.navigation.runtime.SessionGraphStoreViewModel
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.WireSessionId
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
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

    @Test
    fun givenStaleInvalidatingSession_whenGenerationIsConfirmed_thenTeardownCompletesBeforeReopening() {
        val userId = UserId("route-user", "wire.test")
        val events = mutableListOf<String>()

        reopenConfirmedWireActivitySession(
            userId = userId,
            lifecycle = { SessionGraphStoreViewModel.Lifecycle.INVALIDATING },
            teardown = { events += "teardown:${it.value}@${it.domain}" },
            markAvailable = { events += "available:$it" },
        )

        assertEquals(listOf("teardown:route-user@wire.test", "available:$userId"), events)
    }

    @Test
    fun givenSessionIsNotInvalidating_whenGenerationIsConfirmed_thenSessionIsOnlyReopened() {
        val userId = UserId("route-user", "wire.test")
        listOf(
            null,
            SessionGraphStoreViewModel.Lifecycle.ACTIVE,
            SessionGraphStoreViewModel.Lifecycle.REMOVED,
        ).forEach { lifecycle ->
            val events = mutableListOf<String>()

            reopenConfirmedWireActivitySession(
                userId = userId,
                lifecycle = { lifecycle },
                teardown = { events += "teardown" },
                markAvailable = { events += "available" },
            )

            assertEquals(listOf("available"), events, "lifecycle=$lifecycle")
        }
    }

    @Test
    fun givenLogoutOutsideHostTransition_whenSameSessionIsConfirmedAgain_thenFreshGraphIsCreated() {
        val userId = UserId("route-user", "wire.test")
        val store = SessionGraphStoreViewModel(createSessionGraph = { mockk<AppSessionViewModelGraph>(relaxed = true) })
        val staleGraph = store.graphFor(userId)
        // Kalium logout callback, without a host-driven teardown (e.g. logout from self profile).
        store.markInvalidating(userId)

        reopenConfirmedWireActivitySession(
            userId = userId,
            lifecycle = store::lifecycle,
            teardown = { sessionId ->
                teardownWireActivitySession(
                    sessionId = sessionId,
                    markInvalidating = store::markInvalidating,
                    clearOwner = {},
                    markRemoved = store::markRemoved,
                )
            },
            markAvailable = store::markAvailable,
        )

        assertNull(store.lifecycle(userId))
        assertNotSame(staleGraph, store.graphFor(userId))
    }
}
