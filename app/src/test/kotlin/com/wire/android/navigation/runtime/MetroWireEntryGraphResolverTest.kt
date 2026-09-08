/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.navigation.runtime

import com.wire.android.di.metro.AppAuthenticationViewModelGraph
import com.wire.android.di.metro.AppSessionViewModelGraph
import com.wire.android.di.metro.WireApplicationGraph
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.AuthenticationRoute
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireRoute
import com.wire.navigation.WireSessionId
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class MetroWireEntryGraphResolverTest {

    private val appGraph = mockk<WireApplicationGraph>()
    private val authenticationGraph = AppAuthenticationViewModelGraph()
    private val sessionGraphStore = mockk<SessionGraphStoreViewModel>()
    private val resolver = MetroWireEntryGraphResolver(
        appGraph = appGraph,
        authenticationGraph = authenticationGraph,
        sessionGraphStore = sessionGraphStore,
    )

    @Test
    fun givenAuthenticationRoute_whenResolvingGraph_thenAuthenticationGraphAndApplicationFactoryAreUsed() {
        val applicationFactory = mockk<MetroViewModelFactory>()
        every { appGraph.metroViewModelFactory } returns applicationFactory

        val result = resolver.resolve(TestAuthenticationRoute)

        val entryGraph = (result as MetroWireEntryGraphResolution.Available).entryGraph
        assertSame(authenticationGraph, entryGraph.graph)
        assertSame(applicationFactory, entryGraph.viewModelFactory)
        assertEquals(null, entryGraph.sessionGraph)
    }

    @Test
    fun givenSessionRoute_whenResolvingGraph_thenMatchingRetainedSessionGraphAndFactoryAreUsed() {
        val sessionId = WireSessionId(value = "user", domain = "wire.test")
        val userId = UserId(value = "user", domain = "wire.test")
        val sessionGraph = mockk<AppSessionViewModelGraph>()
        val sessionFactory = mockk<MetroViewModelFactory>()
        every { sessionGraphStore.graphFor(userId) } returns sessionGraph
        every { sessionGraph.metroViewModelFactory } returns sessionFactory

        val result = resolver.resolve(TestSessionRoute(sessionId))

        val entryGraph = (result as MetroWireEntryGraphResolution.Available).entryGraph
        assertSame(sessionGraph, entryGraph.graph)
        assertSame(sessionFactory, entryGraph.viewModelFactory)
        assertSame(sessionGraph, entryGraph.sessionGraph)
    }

    @Test
    fun givenUnscopedRoute_whenResolvingGraph_thenUnavailableIsReturned() {
        val result = resolver.resolve(TestUnscopedRoute)

        assertEquals(
            MetroWireEntryGraphResolution.Unavailable(
                WireRouteScope.Unavailable(WireRouteScope.Unavailable.Reason.ROUTE_HAS_NO_SCOPE)
            ),
            result,
        )
    }

    @Test
    fun givenRemovedSessionRoute_whenResolvingGraph_thenTypedUnavailableIsReturned() {
        val sessionId = WireSessionId(value = "removed", domain = "wire.test")
        val userId = UserId(value = "removed", domain = "wire.test")
        every { sessionGraphStore.graphFor(userId) } throws
            SessionGraphUnavailableException("session:<redacted>", SessionGraphStoreViewModel.Lifecycle.REMOVED)

        val result = resolver.resolve(TestSessionRoute(sessionId))

        assertEquals(
            MetroWireEntryGraphResolution.Unavailable(
                WireRouteScope.Unavailable(WireRouteScope.Unavailable.Reason.SESSION_GRAPH_REMOVED)
            ),
            result,
        )
        verify(exactly = 0) { sessionGraphStore.lifecycle(userId) }
    }

    @Test
    fun givenInvalidatingSessionRoute_whenLifecycleChangesAfterFailure_thenOriginalFailureIsMapped() {
        val sessionId = WireSessionId(value = "invalidating", domain = "wire.test")
        val userId = UserId(value = "invalidating", domain = "wire.test")
        every { sessionGraphStore.graphFor(userId) } throws SessionGraphUnavailableException(
            "session:<redacted>",
            SessionGraphStoreViewModel.Lifecycle.INVALIDATING,
        )
        val result = resolver.resolve(TestSessionRoute(sessionId))

        assertEquals(
            MetroWireEntryGraphResolution.Unavailable(
                WireRouteScope.Unavailable(WireRouteScope.Unavailable.Reason.SESSION_GRAPH_INVALIDATING)
            ),
            result,
        )
        verify(exactly = 0) { sessionGraphStore.lifecycle(userId) }
    }

    private data object TestAuthenticationRoute : AuthenticationRoute {
        override val entryId: WireNavEntryId = WireNavEntryId("authentication-entry")
        override val routeId: String = "authentication"
    }

    private data class TestSessionRoute(
        override val sessionId: WireSessionId,
    ) : SessionRoute {
        override val entryId: WireNavEntryId = WireNavEntryId("session-entry")
        override val routeId: String = "session"
    }

    private data object TestUnscopedRoute : WireRoute {
        override val entryId: WireNavEntryId = WireNavEntryId("unscoped-entry")
        override val routeId: String = "unscoped"
    }
}
