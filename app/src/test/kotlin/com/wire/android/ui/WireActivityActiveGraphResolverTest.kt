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

package com.wire.android.ui

import com.ramcosta.composedestinations.generated.app.destinations.LoginScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.RegisterDeviceScreenDestination
import com.wire.android.di.metro.AppAuthenticationViewModelGraph
import com.wire.android.di.metro.AppSessionViewModelGraph
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class WireActivityActiveGraphResolverTest {

    private val authenticationViewModelGraph = AppAuthenticationViewModelGraph()
    private val sessionGraph = mockk<AppSessionViewModelGraph>()

    @Test
    fun givenOldLoginRouteHasSessionGraph_whenResolvingActiveGraph_thenAuthenticationGraphIsUsed() {
        val graph = resolveWireActivityActiveGraph(
            WireActivityActiveGraphRequest(
                authenticationViewModelGraph = authenticationViewModelGraph,
                sessionGraph = sessionGraph,
                effectiveBaseRoute = LoginScreenDestination.baseRoute,
                currentBaseRoute = LoginScreenDestination.baseRoute,
                usesAuthenticationGraph = true,
                usesNoSessionAuthenticationGraph = false,
                usesInvalidSessionBackedAuthenticationGraph = false,
                isSessionTransitionInProgress = false,
            )
        )

        assertSame(authenticationViewModelGraph, graph)
    }

    @Test
    fun givenSessionBackedAuthenticationRouteHasSessionGraph_whenResolvingActiveGraph_thenSessionGraphIsUsed() {
        val graph = resolveWireActivityActiveGraph(
            WireActivityActiveGraphRequest(
                authenticationViewModelGraph = authenticationViewModelGraph,
                sessionGraph = sessionGraph,
                effectiveBaseRoute = RegisterDeviceScreenDestination.baseRoute,
                currentBaseRoute = RegisterDeviceScreenDestination.baseRoute,
                usesAuthenticationGraph = true,
                usesNoSessionAuthenticationGraph = false,
                usesInvalidSessionBackedAuthenticationGraph = false,
                isSessionTransitionInProgress = false,
            )
        )

        assertSame(sessionGraph, graph)
    }
}
