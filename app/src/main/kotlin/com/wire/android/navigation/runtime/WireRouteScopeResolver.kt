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
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireRoute
import com.wire.navigation.WireSessionId

/**
 * Describes which Metro graph owns the dependencies used by a route.
 *
 * This intentionally contains no graph instance. Resolving route ownership is pure, while graph
 * creation and lifecycle retention remain the responsibility of the application runtime.
 */
internal sealed interface WireRouteScope {

    data object Authentication : WireRouteScope

    data class Session(val sessionId: WireSessionId) : WireRouteScope

    data class Unavailable(val reason: Reason) : WireRouteScope {
        enum class Reason {
            ROUTE_HAS_NO_SCOPE,
            ROUTE_HAS_MULTIPLE_SCOPES,
            SESSION_GRAPH_INVALIDATING,
            SESSION_GRAPH_REMOVED,
        }
    }
}

/**
 * Replaces generated destination-name matching with an exhaustive typed route-scope decision.
 *
 * Returning [WireRouteScope.Unavailable] is deliberate: an invalid route must never silently
 * inherit the authentication graph or a graph belonging to another session.
 */
internal object WireRouteScopeResolver {

    fun resolve(route: WireRoute): WireRouteScope {
        val isAuthenticationRoute = route is AuthenticationRoute
        val isSessionRoute = route is SessionRoute

        return when {
            isAuthenticationRoute && isSessionRoute ->
                WireRouteScope.Unavailable(WireRouteScope.Unavailable.Reason.ROUTE_HAS_MULTIPLE_SCOPES)

            isAuthenticationRoute -> WireRouteScope.Authentication

            isSessionRoute -> WireRouteScope.Session(route.sessionId)

            else -> WireRouteScope.Unavailable(WireRouteScope.Unavailable.Reason.ROUTE_HAS_NO_SCOPE)
        }
    }
}
