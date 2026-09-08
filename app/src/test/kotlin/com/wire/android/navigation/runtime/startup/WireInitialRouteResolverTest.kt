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

package com.wire.android.navigation.runtime.startup

import com.wire.android.navigation.routes.auth.NewWelcomeEmptyStartRoute
import com.wire.android.navigation.routes.auth.WelcomeRoute
import com.wire.android.ui.InitialAppState
import com.wire.android.ui.e2eiEnrollment.E2EIEnrollmentRoute
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class WireInitialRouteResolverTest {

    @Test
    fun givenLoggedOutAndNewLoginEnabled_whenResolving_thenNewWelcomeRouteIsReturned() {
        val result = WireInitialRouteResolver.resolve(
            initialAppState = InitialAppState.NotLoggedIn,
            loginType = WireStartupLoginType.NEW,
            activeSessionId = null,
        )

        assertEquals(NewWelcomeEmptyStartRoute.ROUTE_ID, result.routeId)
        assertEquals(NewWelcomeEmptyStartRoute::class, result::class)
    }

    @Test
    fun givenLoggedOutAndLegacyLoginSelected_whenResolving_thenLegacyWelcomeRouteIsReturned() {
        val result = WireInitialRouteResolver.resolve(
            initialAppState = InitialAppState.NotLoggedIn,
            loginType = WireStartupLoginType.LEGACY,
            activeSessionId = null,
        )

        assertEquals(WelcomeRoute.ROUTE_ID, result.routeId)
        assertEquals(WelcomeRoute::class, result::class)
    }

    @Test
    fun givenE2eiEnrollmentRequired_whenResolving_thenRouteOwnsTheRequestedSession() {
        val userId = UserId("enrolling-user", "wire.test")

        val result = WireInitialRouteResolver.resolve(
            initialAppState = InitialAppState.EnrollE2EI(userId),
            loginType = WireStartupLoginType.NEW,
            activeSessionId = WireSessionId("different-user", "wire.test"),
        ) as E2EIEnrollmentRoute

        assertEquals(WireSessionId("enrolling-user", "wire.test"), result.sessionId)
        assertEquals(E2EIEnrollmentRoute.ROUTE_ID, result.routeId)
    }

    @Test
    fun givenLoggedIn_whenResolving_thenHomeRouteOwnsTheActiveSession() {
        val sessionId = WireSessionId("active-user", "wire.test")

        val result = WireInitialRouteResolver.resolve(
            initialAppState = InitialAppState.LoggedIn,
            loginType = WireStartupLoginType.NEW,
            activeSessionId = sessionId,
        ) as HomeRoute

        assertEquals(sessionId, result.sessionId)
        assertEquals("app/home_screen", result.routeId)
    }

    @Test
    fun givenLoggedInWithoutActiveSession_whenResolving_thenInvalidStateIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            WireInitialRouteResolver.resolve(
                initialAppState = InitialAppState.LoggedIn,
                loginType = WireStartupLoginType.NEW,
                activeSessionId = null,
            )
        }
    }

    @Test
    fun givenLoginCapability_whenSelectingLoginType_thenBooleanMeaningIsPreserved() {
        assertEquals(
            WireStartupLoginType.NEW,
            WireStartupLoginType.fromCanUseNewLogin(true),
        )
        assertEquals(
            WireStartupLoginType.LEGACY,
            WireStartupLoginType.fromCanUseNewLogin(false),
        )
    }

    @Test
    fun givenHomeRoute_whenSerializedAndRestored_thenSessionAndEntryIdentityArePreserved() {
        val route = HomeRoute(
            sessionId = WireSessionId("active-user", "wire.test"),
            entryId = WireNavEntryId("home-entry"),
        )

        val restored = Json.decodeFromString<HomeRoute>(Json.encodeToString(route))

        assertEquals(route, restored)
        assertEquals("app/home_screen", restored.routeId)
    }
}
