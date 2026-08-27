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

import com.wire.navigation.AuthenticationRoute
import com.wire.navigation.AuthenticationScreenRoute
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireRoute
import com.wire.navigation.WireSessionId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WireRouteScopeResolverTest {

    @Test
    fun givenAuthenticationRoute_whenResolvingScope_thenAuthenticationScopeIsReturned() {
        val result = WireRouteScopeResolver.resolve(TestAuthenticationRoute)

        assertEquals(WireRouteScope.Authentication, result)
    }

    @Test
    fun givenSessionRoute_whenResolvingScope_thenMatchingSessionScopeIsReturned() {
        val sessionId = WireSessionId(value = "user", domain = "wire.test")
        val result = WireRouteScopeResolver.resolve(TestSessionRoute(sessionId))

        assertEquals(WireRouteScope.Session(sessionId), result)
    }

    @Test
    fun givenSessionBackedAuthenticationScreen_whenResolvingScope_thenSessionScopeIsReturned() {
        val result = WireRouteScopeResolver.resolve(TestSessionBackedAuthenticationRoute)

        assertEquals(
            WireRouteScope.Session(TestSessionBackedAuthenticationRoute.sessionId),
            result,
        )
    }

    @Test
    fun givenUnscopedRoute_whenResolvingScope_thenUnavailableIsReturned() {
        val result = WireRouteScopeResolver.resolve(TestUnscopedRoute)

        assertEquals(
            WireRouteScope.Unavailable(WireRouteScope.Unavailable.Reason.ROUTE_HAS_NO_SCOPE),
            result,
        )
    }

    @Test
    fun givenRouteWithBothScopeMarkers_whenResolvingScope_thenUnavailableIsReturned() {
        val result = WireRouteScopeResolver.resolve(TestAmbiguousRoute)

        assertEquals(
            WireRouteScope.Unavailable(WireRouteScope.Unavailable.Reason.ROUTE_HAS_MULTIPLE_SCOPES),
            result,
        )
    }

    private data object TestAuthenticationRoute : AuthenticationRoute {
        override val routeId: String = "authentication"
        override val entryId: WireNavEntryId = WireNavEntryId("authentication-entry")
    }

    private data class TestSessionRoute(override val sessionId: WireSessionId) : SessionRoute {
        override val routeId: String = "session"
        override val entryId: WireNavEntryId = WireNavEntryId.random()
    }

    private data object TestSessionBackedAuthenticationRoute :
        SessionRoute,
        AuthenticationScreenRoute {
        override val routeId: String = "session-backed-authentication"
        override val entryId = WireNavEntryId("session-backed-authentication-entry")
        override val sessionId = WireSessionId(value = "user", domain = "wire.test")
    }

    private data object TestUnscopedRoute : WireRoute {
        override val routeId: String = "unscoped"
        override val entryId: WireNavEntryId = WireNavEntryId("unscoped-entry")
    }

    private data object TestAmbiguousRoute : AuthenticationRoute, SessionRoute {
        override val routeId: String = "ambiguous"
        override val entryId: WireNavEntryId = WireNavEntryId("ambiguous-entry")
        override val sessionId: WireSessionId = WireSessionId(value = "user", domain = "wire.test")
    }
}
