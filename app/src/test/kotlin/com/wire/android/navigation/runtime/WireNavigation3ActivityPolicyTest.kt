/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.runtime

import com.wire.android.navigation.routes.auth.NewWelcomeEmptyStartRoute
import com.wire.android.navigation.routes.auth.WelcomeRoute
import com.wire.android.navigation.style.BackgroundType
import com.wire.navigation.AuthenticationScreenRoute
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WireNavigation3ActivityPolicyTest {

    @Test
    fun givenAuthenticationRoute_whenResolvingActivityPolicy_thenAuthBackgroundAndFlagsAreUsed() {
        val snapshot = WireNavigation3ActivityPolicy.sessionSnapshot(
            route = WelcomeRoute(),
            hasCurrentSession = false,
            isUserUiBlocked = false,
            isSessionTransitionInProgress = false,
            isSelfLogoutTransition = false,
        )

        assertEquals(BackgroundType.Auth, WireNavigation3ActivityPolicy.backgroundType(WelcomeRoute()))
        assertTrue(snapshot.isAuthenticationRoute)
        assertFalse(snapshot.isSessionBackedAuthenticationRoute)
    }

    @Test
    fun givenSessionBackedAuthenticationRoute_whenResolvingPolicy_thenBothScopesArePreserved() {
        val route = TestSessionAuthenticationRoute

        val snapshot = WireNavigation3ActivityPolicy.sessionSnapshot(
            route = route,
            hasCurrentSession = false,
            isUserUiBlocked = true,
            isSessionTransitionInProgress = true,
            isSelfLogoutTransition = true,
        )

        assertEquals(BackgroundType.Auth, WireNavigation3ActivityPolicy.backgroundType(route))
        assertTrue(snapshot.isAuthenticationRoute)
        assertTrue(snapshot.isSessionBackedAuthenticationRoute)
        assertTrue(snapshot.isUserUiBlocked)
        assertTrue(snapshot.isSessionTransitionInProgress)
        assertTrue(snapshot.isSelfLogoutTransition)
    }

    @Test
    fun givenEmptyWelcomeOrNoRoute_whenResolvingPolicy_thenStartupFlagsAreExact() {
        val empty = WireNavigation3ActivityPolicy.sessionSnapshot(
            route = NewWelcomeEmptyStartRoute(),
            hasCurrentSession = false,
            isUserUiBlocked = false,
            isSessionTransitionInProgress = false,
            isSelfLogoutTransition = false,
        )
        val absent = WireNavigation3ActivityPolicy.sessionSnapshot(
            route = null,
            hasCurrentSession = false,
            isUserUiBlocked = false,
            isSessionTransitionInProgress = false,
            isSelfLogoutTransition = false,
        )

        assertTrue(empty.isEmptyWelcomeRoute)
        assertTrue(empty.hasCurrentRoute)
        assertFalse(absent.hasCurrentRoute)
        assertEquals(BackgroundType.Default, WireNavigation3ActivityPolicy.backgroundType(null))
    }

    private data object TestSessionAuthenticationRoute :
        AuthenticationScreenRoute,
        SessionRoute {
        override val sessionId = WireSessionId("user", "wire.example")
        override val entryId = WireNavEntryId("entry")
        override val routeId = "session-auth"
    }
}
