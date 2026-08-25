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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WireSessionNavigationPolicyTest {

    @Test
    fun givenUserUiIsBlocked_whenResolvingSessionNavigation_thenWaitForDialogTakesPrecedence() {
        val decision = resolve {
            copy(hasCurrentSession = true, isEmptyWelcomeRoute = true, isUserUiBlocked = true)
        }

        assertEquals(WireSessionNavigationDecision.WAIT_FOR_BLOCKING_DIALOG, decision)
    }

    @Test
    fun givenSessionExistsOnEmptyWelcome_whenResolvingSessionNavigation_thenNavigateHomeClearingStack() {
        val decision = resolve {
            copy(hasCurrentSession = true, isEmptyWelcomeRoute = true)
        }

        assertEquals(WireSessionNavigationDecision.NAVIGATE_HOME_CLEAR_STACK, decision)
    }

    @Test
    fun givenSessionIsMissingOnEmptyWelcome_whenResolvingSessionNavigation_thenNavigateLoginClearingStack() {
        val decision = resolve { copy(isEmptyWelcomeRoute = true) }

        assertEquals(WireSessionNavigationDecision.NAVIGATE_LOGIN_CLEAR_STACK, decision)
    }

    @Test
    fun givenSessionIsMissingOnSessionBackedAuthentication_whenResolvingSessionNavigation_thenNavigateLogin() {
        val decision = resolve {
            copy(
                isAuthenticationRoute = true,
                isSessionBackedAuthenticationRoute = true,
                isSessionTransitionInProgress = true,
            )
        }

        assertEquals(WireSessionNavigationDecision.NAVIGATE_LOGIN_CLEAR_STACK, decision)
    }

    @Test
    fun givenNonAuthenticationRouteDuringAccountTransition_whenResolvingSessionNavigation_thenResolveCurrentSession() {
        val decision = resolve {
            copy(
                hasCurrentSession = true,
                hasCurrentRoute = true,
                isSessionTransitionInProgress = true,
            )
        }

        assertEquals(WireSessionNavigationDecision.RESOLVE_CURRENT_SESSION, decision)
    }

    @Test
    fun givenSelfLogoutTransition_whenResolvingSessionNavigation_thenDoNothing() {
        val decision = resolve {
            copy(
                hasCurrentRoute = true,
                isSessionTransitionInProgress = true,
                isSelfLogoutTransition = true,
            )
        }

        assertEquals(WireSessionNavigationDecision.NONE, decision)
    }

    @Test
    fun givenAuthenticationRouteDuringTransition_whenResolvingSessionNavigation_thenDoNothing() {
        val decision = resolve {
            copy(
                hasCurrentRoute = true,
                isAuthenticationRoute = true,
                isSessionTransitionInProgress = true,
            )
        }

        assertEquals(WireSessionNavigationDecision.NONE, decision)
    }

    @Test
    fun givenSessionIsMissingOnOrdinaryRoute_whenResolvingSessionNavigation_thenResolveCurrentSession() {
        val decision = resolve { copy(hasCurrentRoute = true) }

        assertEquals(WireSessionNavigationDecision.RESOLVE_CURRENT_SESSION, decision)
    }

    @Test
    fun givenCurrentRouteIsMissing_whenResolvingSessionNavigation_thenDoNothing() {
        val decision = resolve()

        assertEquals(WireSessionNavigationDecision.NONE, decision)
    }

    @Test
    fun givenValidSessionOnOrdinaryRoute_whenResolvingSessionNavigation_thenDoNothing() {
        val decision = resolve { copy(hasCurrentSession = true, hasCurrentRoute = true) }

        assertEquals(WireSessionNavigationDecision.NONE, decision)
    }

    private fun resolve(
        transform: WireSessionNavigationSnapshot.() -> WireSessionNavigationSnapshot = { this },
    ): WireSessionNavigationDecision = WireSessionNavigationPolicy.resolve(DEFAULT_SNAPSHOT.transform())

    private companion object {
        val DEFAULT_SNAPSHOT = WireSessionNavigationSnapshot(
            hasCurrentSession = false,
            hasCurrentRoute = false,
            isEmptyWelcomeRoute = false,
            isAuthenticationRoute = false,
            isSessionBackedAuthenticationRoute = false,
            isUserUiBlocked = false,
            isSessionTransitionInProgress = false,
            isSelfLogoutTransition = false,
        )
    }
}
