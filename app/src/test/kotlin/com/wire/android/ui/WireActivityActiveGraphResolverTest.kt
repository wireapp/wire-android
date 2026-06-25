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

import com.ramcosta.composedestinations.generated.app.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.LoginScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.NewLoginScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.NewLoginPasswordScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.NewWelcomeEmptyStartScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.RegisterDeviceScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.WelcomeScreenDestination
import com.wire.android.di.metro.AppAuthenticationViewModelGraph
import com.wire.android.di.metro.AppSessionViewModelGraph
import com.wire.android.navigation.baseRoute
import com.wire.kalium.logic.data.user.UserId
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

    @Test
    fun givenCurrentRouteIsUnknownAndLoggedOutStartIsAuth_whenResolvingActiveGraph_thenAuthenticationGraphIsUsed() {
        val graph = resolveWireActivityActiveGraph(
            WireActivityActiveGraphRequest(
                authenticationViewModelGraph = authenticationViewModelGraph,
                sessionGraph = null,
                effectiveBaseRoute = NewWelcomeEmptyStartScreenDestination.baseRoute,
                currentBaseRoute = null,
                usesAuthenticationGraph = true,
                usesNoSessionAuthenticationGraph = false,
                usesInvalidSessionBackedAuthenticationGraph = false,
                isSessionTransitionInProgress = false,
            )
        )

        assertSame(authenticationViewModelGraph, graph)
    }

    @Test
    fun givenCurrentRouteIsUnknownAndLoggedOutStartIsSessionRoute_whenResolvingActiveGraph_thenNoGraphIsUsed() {
        val graph = resolveWireActivityActiveGraph(
            WireActivityActiveGraphRequest(
                authenticationViewModelGraph = authenticationViewModelGraph,
                sessionGraph = null,
                effectiveBaseRoute = HomeScreenDestination.baseRoute,
                currentBaseRoute = null,
                usesAuthenticationGraph = false,
                usesNoSessionAuthenticationGraph = false,
                usesInvalidSessionBackedAuthenticationGraph = false,
                isSessionTransitionInProgress = false,
            )
        )

        assertNull(graph)
    }

    @Test
    fun givenCurrentRouteIsUnknownAndSessionGraphExists_whenResolvingActiveGraph_thenSessionGraphIsUsed() {
        val graph = resolveWireActivityActiveGraph(
            WireActivityActiveGraphRequest(
                authenticationViewModelGraph = authenticationViewModelGraph,
                sessionGraph = sessionGraph,
                effectiveBaseRoute = HomeScreenDestination.baseRoute,
                currentBaseRoute = null,
                usesAuthenticationGraph = false,
                usesNoSessionAuthenticationGraph = false,
                usesInvalidSessionBackedAuthenticationGraph = false,
                isSessionTransitionInProgress = false,
            )
        )

        assertSame(sessionGraph, graph)
    }

    @Test
    fun givenSessionTransitionIsInProgressOnSessionRoute_whenResolvingActiveGraph_thenNoGraphIsUsed() {
        val graph = resolveWireActivityActiveGraph(
            WireActivityActiveGraphRequest(
                authenticationViewModelGraph = authenticationViewModelGraph,
                sessionGraph = sessionGraph,
                effectiveBaseRoute = HomeScreenDestination.baseRoute,
                currentBaseRoute = HomeScreenDestination.baseRoute,
                usesAuthenticationGraph = false,
                usesNoSessionAuthenticationGraph = false,
                usesInvalidSessionBackedAuthenticationGraph = false,
                isSessionTransitionInProgress = true,
            )
        )

        assertNull(graph)
    }

    @Test
    fun givenUserLogsOutFromSessionRoute_whenResolvingTransientStates_thenSessionGraphIsDroppedAndAuthRootIsUsed() {
        val loggedInGraph = resolveActiveGraph(
            sessionGraph = sessionGraph,
            effectiveBaseRoute = HomeScreenDestination.baseRoute,
            currentBaseRoute = HomeScreenDestination.baseRoute,
        )
        val transitioningGraph = resolveActiveGraph(
            sessionGraph = null,
            effectiveBaseRoute = HomeScreenDestination.baseRoute,
            currentBaseRoute = HomeScreenDestination.baseRoute,
            isSessionTransitionInProgress = true,
        )
        val loggedOutStartDestination = resolveWireActivityNavHostStartDestination(
            initialStartDestination = HomeScreenDestination,
            currentUserId = null,
            currentBaseRoute = null,
            canUseNewLogin = true,
        )
        val loggedOutGraph = resolveActiveGraph(
            sessionGraph = null,
            effectiveBaseRoute = loggedOutStartDestination.baseRoute,
            currentBaseRoute = null,
            usesAuthenticationGraph = true,
        )

        assertSame(sessionGraph, loggedInGraph)
        assertNull(transitioningGraph)
        assertEquals(NewWelcomeEmptyStartScreenDestination.baseRoute, loggedOutStartDestination.baseRoute)
        assertSame(authenticationViewModelGraph, loggedOutGraph)
    }

    @Test
    fun givenUserSwitchesAccountsFromSessionRoute_whenResolvingTransientStates_thenGraphIsDroppedUntilNewSessionGraphExists() {
        val newSessionGraph = mockk<AppSessionViewModelGraph>()
        val currentUserId = UserId("current-user", "domain")
        val switchedUserId = UserId("switched-user", "domain")

        val currentAccountGraph = resolveActiveGraph(
            sessionGraph = sessionGraph,
            effectiveBaseRoute = HomeScreenDestination.baseRoute,
            currentBaseRoute = HomeScreenDestination.baseRoute,
        )
        val switchingGraph = resolveActiveGraph(
            sessionGraph = null,
            effectiveBaseRoute = HomeScreenDestination.baseRoute,
            currentBaseRoute = HomeScreenDestination.baseRoute,
            isSessionTransitionInProgress = true,
        )
        val switchedStartDestination = resolveWireActivityNavHostStartDestination(
            initialStartDestination = HomeScreenDestination,
            currentUserId = switchedUserId,
            currentBaseRoute = HomeScreenDestination.baseRoute,
            canUseNewLogin = true,
        )
        val switchedAccountGraph = resolveActiveGraph(
            sessionGraph = newSessionGraph,
            effectiveBaseRoute = switchedStartDestination.baseRoute,
            currentBaseRoute = HomeScreenDestination.baseRoute,
        )

        assertEquals(
            HomeScreenDestination.baseRoute,
            resolveWireActivityNavHostStartDestination(
                initialStartDestination = HomeScreenDestination,
                currentUserId = currentUserId,
                currentBaseRoute = HomeScreenDestination.baseRoute,
                canUseNewLogin = true,
            ).baseRoute
        )
        assertSame(sessionGraph, currentAccountGraph)
        assertNull(switchingGraph)
        assertEquals(HomeScreenDestination.baseRoute, switchedStartDestination.baseRoute)
        assertSame(newSessionGraph, switchedAccountGraph)
    }

    @Test
    fun givenLoggedOutUserHasUnknownRouteAndNewLoginEnabled_whenResolvingNavHostStartDestination_thenEmptyWelcomeRootIsUsed() {
        val destination = resolveWireActivityNavHostStartDestination(
            initialStartDestination = HomeScreenDestination,
            currentUserId = null,
            currentBaseRoute = null,
            canUseNewLogin = true,
        )

        assertEquals(NewWelcomeEmptyStartScreenDestination.baseRoute, destination.baseRoute)
    }

    @Test
    fun givenLoggedOutUserHasUnknownRouteAndNewLoginDisabled_whenResolvingNavHostStartDestination_thenWelcomeRootIsUsed() {
        val destination = resolveWireActivityNavHostStartDestination(
            initialStartDestination = HomeScreenDestination,
            currentUserId = null,
            currentBaseRoute = null,
            canUseNewLogin = false,
        )

        assertEquals(WelcomeScreenDestination.baseRoute, destination.baseRoute)
    }

    @Test
    fun givenLoggedOutUserIsOnNewLoginRoute_whenResolvingNavHostStartDestination_thenEmptyWelcomeRootIsUsed() {
        val destination = resolveWireActivityNavHostStartDestination(
            initialStartDestination = HomeScreenDestination,
            currentUserId = null,
            currentBaseRoute = NewLoginScreenDestination.baseRoute,
            canUseNewLogin = true,
        )

        assertEquals(NewWelcomeEmptyStartScreenDestination.baseRoute, destination.baseRoute)
    }

    @Test
    fun givenLoggedOutUserIsOnOldLoginRoute_whenResolvingNavHostStartDestination_thenWelcomeRootIsUsed() {
        val destination = resolveWireActivityNavHostStartDestination(
            initialStartDestination = HomeScreenDestination,
            currentUserId = null,
            currentBaseRoute = LoginScreenDestination.baseRoute,
            canUseNewLogin = false,
        )

        assertEquals(WelcomeScreenDestination.baseRoute, destination.baseRoute)
    }

    @Test
    fun givenLoggedInUserHasUnknownRoute_whenResolvingNavHostStartDestination_thenInitialStartDestinationIsUsed() {
        val destination = resolveWireActivityNavHostStartDestination(
            initialStartDestination = HomeScreenDestination,
            currentUserId = UserId("user", "domain"),
            currentBaseRoute = null,
            canUseNewLogin = true,
        )

        assertEquals(HomeScreenDestination.baseRoute, destination.baseRoute)
    }

    @Test
    fun givenLoggedInUserIsOnNestedNoSessionAuthRoute_whenResolvingNavHostStartDestination_thenInitialStartDestinationIsKept() {
        val destination = resolveWireActivityNavHostStartDestination(
            initialStartDestination = HomeScreenDestination,
            currentUserId = UserId("user", "domain"),
            currentBaseRoute = NewLoginPasswordScreenDestination.baseRoute,
            canUseNewLogin = true,
        )

        assertEquals(HomeScreenDestination.baseRoute, destination.baseRoute)
    }

    @Test
    fun givenNoSessionLoginFlowReceivesCurrentUserOnPasswordRoute_whenResolvingNavHostStartDestination_thenAuthRootIsKept() {
        val destination = resolveWireActivityNavHostStartDestination(
            initialStartDestination = HomeScreenDestination,
            currentUserId = UserId("user", "domain"),
            currentBaseRoute = NewLoginPasswordScreenDestination.baseRoute,
            canUseNewLogin = true,
            noSessionAuthenticationStartedWithoutSession = true,
        )

        assertEquals(NewWelcomeEmptyStartScreenDestination.baseRoute, destination.baseRoute)
    }

    @Test
    fun givenAddAccountFlowReceivesCurrentUserOnPasswordRoute_whenResolvingNavHostStartDestination_thenSessionRootIsKept() {
        val destination = resolveWireActivityNavHostStartDestination(
            initialStartDestination = HomeScreenDestination,
            currentUserId = UserId("user", "domain"),
            currentBaseRoute = NewLoginPasswordScreenDestination.baseRoute,
            canUseNewLogin = true,
            noSessionAuthenticationStartedWithoutSession = false,
        )

        assertEquals(HomeScreenDestination.baseRoute, destination.baseRoute)
    }

    @Test
    fun givenLoggedInUserIsOnNewLoginRoute_whenResolvingNavHostStartDestination_thenInitialStartDestinationIsKept() {
        val destination = resolveWireActivityNavHostStartDestination(
            initialStartDestination = HomeScreenDestination,
            currentUserId = UserId("user", "domain"),
            currentBaseRoute = NewLoginScreenDestination.baseRoute,
            canUseNewLogin = true,
        )

        assertEquals(HomeScreenDestination.baseRoute, destination.baseRoute)
    }

    @Test
    fun givenInitialStartDestinationIsAuthenticationRoot_whenResolvingNavHostStartDestination_thenInitialStartDestinationIsUsed() {
        val destination = resolveWireActivityNavHostStartDestination(
            initialStartDestination = NewWelcomeEmptyStartScreenDestination,
            currentUserId = null,
            currentBaseRoute = null,
            canUseNewLogin = false,
        )

        assertEquals(NewWelcomeEmptyStartScreenDestination.baseRoute, destination.baseRoute)
    }

    @Test
    fun givenLoggedOutUserIsOnNestedNoSessionAuthRoute_whenResolvingNavHostStartDestination_thenEmptyWelcomeRootIsUsed() {
        val destination = resolveWireActivityNavHostStartDestination(
            initialStartDestination = HomeScreenDestination,
            currentUserId = null,
            currentBaseRoute = NewLoginPasswordScreenDestination.baseRoute,
            canUseNewLogin = true,
        )

        assertEquals(NewWelcomeEmptyStartScreenDestination.baseRoute, destination.baseRoute)
    }

    @Test
    fun givenActiveSessionGraphExists_whenResolvingImageLoaderSessionGraph_thenActiveSessionGraphIsUsed() {
        val retainedSessionGraph = mockk<AppSessionViewModelGraph>()

        val imageLoaderSessionGraph = resolveWireActivityImageLoaderSessionGraph(
            activeSessionGraph = sessionGraph,
            retainedSessionGraph = retainedSessionGraph,
        )

        assertSame(sessionGraph, imageLoaderSessionGraph)
    }

    @Test
    fun givenAuthGraphIsActiveAndSessionContentIsRetained_whenResolvingImageLoaderSessionGraph_thenRetainedSessionGraphIsUsed() {
        val imageLoaderSessionGraph = resolveWireActivityImageLoaderSessionGraph(
            activeSessionGraph = null,
            retainedSessionGraph = sessionGraph,
        )

        assertSame(sessionGraph, imageLoaderSessionGraph)
    }

    private fun resolveActiveGraph(
        sessionGraph: AppSessionViewModelGraph?,
        effectiveBaseRoute: String,
        currentBaseRoute: String?,
        usesAuthenticationGraph: Boolean = false,
        isSessionTransitionInProgress: Boolean = false,
    ) = resolveWireActivityActiveGraph(
        WireActivityActiveGraphRequest(
            authenticationViewModelGraph = authenticationViewModelGraph,
            sessionGraph = sessionGraph,
            effectiveBaseRoute = effectiveBaseRoute,
            currentBaseRoute = currentBaseRoute,
            usesAuthenticationGraph = usesAuthenticationGraph,
            usesNoSessionAuthenticationGraph = false,
            usesInvalidSessionBackedAuthenticationGraph = false,
            isSessionTransitionInProgress = isSessionTransitionInProgress,
        )
    )
}
