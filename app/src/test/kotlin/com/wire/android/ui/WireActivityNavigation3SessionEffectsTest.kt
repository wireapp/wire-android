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

import com.wire.android.navigation.routes.auth.NewLoginRoute
import com.wire.android.navigation.runtime.WireSessionNavigationSnapshot
import com.wire.android.navigation.runtime.startup.HomeRoute
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireSessionId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class WireActivityNavigation3SessionEffectsTest {

    @Test
    fun givenSessionOnEmptyWelcome_whenResolving_thenClearsStackToTypedHome() {
        val sessionId = WireSessionId("user", "example.com")

        val effect = WireActivityNavigation3SessionEffectResolver.resolve(
            snapshot = snapshot(
                hasCurrentSession = true,
                isEmptyWelcomeRoute = true,
                isAuthenticationRoute = true,
            ),
            currentSessionId = sessionId,
        )

        val command = assertInstanceOf(WireActivityNavigation3SessionEffect.Navigate::class.java, effect).command
        assertEquals(sessionId, assertInstanceOf(HomeRoute::class.java, command.destination).sessionId)
        assertEquals(WireBackStackMode.CLEAR_WHOLE, command.backStackMode)
    }

    @Test
    fun givenNoSessionOnEmptyWelcome_whenResolving_thenClearsStackToTypedLogin() {
        val effect = WireActivityNavigation3SessionEffectResolver.resolve(
            snapshot = snapshot(
                hasCurrentSession = false,
                isEmptyWelcomeRoute = true,
                isAuthenticationRoute = true,
            ),
            currentSessionId = null,
        )

        val command = assertInstanceOf(WireActivityNavigation3SessionEffect.Navigate::class.java, effect).command
        assertInstanceOf(NewLoginRoute::class.java, command.destination)
        assertEquals(WireBackStackMode.CLEAR_WHOLE, command.backStackMode)
    }

    @Test
    fun givenNoSessionOnSessionBackedAuthentication_whenResolving_thenClearsStackToTypedLogin() {
        val effect = WireActivityNavigation3SessionEffectResolver.resolve(
            snapshot = snapshot(
                hasCurrentSession = false,
                isAuthenticationRoute = true,
                isSessionBackedAuthenticationRoute = true,
            ),
            currentSessionId = null,
        )

        val command = assertInstanceOf(WireActivityNavigation3SessionEffect.Navigate::class.java, effect).command
        assertInstanceOf(NewLoginRoute::class.java, command.destination)
        assertEquals(WireBackStackMode.CLEAR_WHOLE, command.backStackMode)
    }

    @Test
    fun givenMissingSessionOnSessionRoute_whenResolving_thenRequestsAccountRecovery() {
        val effect = WireActivityNavigation3SessionEffectResolver.resolve(
            snapshot = snapshot(
                hasCurrentSession = false,
                isAuthenticationRoute = false,
            ),
            currentSessionId = null,
        )

        assertEquals(WireActivityNavigation3SessionEffect.ResolveCurrentSession, effect)
    }

    @Test
    fun givenSelectedAccountChangedBehindCurrentRoute_whenResolving_thenClearsStackToNewHome() {
        val previousSession = WireSessionId("previous", "example.com")
        val currentSession = WireSessionId("current", "example.com")

        val effect = WireActivityNavigation3SessionEffectResolver.resolve(
            snapshot = snapshot(
                hasCurrentSession = true,
                isAuthenticationRoute = false,
            ),
            currentSessionId = currentSession,
            routeSessionId = previousSession,
        )

        val command = assertInstanceOf(WireActivityNavigation3SessionEffect.Navigate::class.java, effect).command
        assertEquals(currentSession, assertInstanceOf(HomeRoute::class.java, command.destination).sessionId)
        assertEquals(WireBackStackMode.CLEAR_WHOLE, command.backStackMode)
    }

    @Test
    fun givenBlockedUiAndSelectedAccountChanged_whenResolving_thenWaitForDialogTakesPrecedence() {
        val effect = WireActivityNavigation3SessionEffectResolver.resolve(
            snapshot = snapshot(
                hasCurrentSession = true,
                isAuthenticationRoute = false,
                isUserUiBlocked = true,
            ),
            currentSessionId = WireSessionId("current", "example.com"),
            routeSessionId = WireSessionId("previous", "example.com"),
        )

        assertEquals(WireActivityNavigation3SessionEffect.WaitForBlockingDialog, effect)
    }

    @Test
    fun givenAuthenticationRouteDuringSessionTransition_whenResolving_thenFinishesTransitionBeforeOtherEffects() {
        val effect = WireActivityNavigation3SessionEffectResolver.resolve(
            snapshot = snapshot(
                hasCurrentSession = false,
                isAuthenticationRoute = true,
                isUserUiBlocked = true,
                isSessionTransitionInProgress = true,
            ),
            currentSessionId = null,
        )

        assertEquals(WireActivityNavigation3SessionEffect.FinishTransition, effect)
    }

    @Suppress("LongParameterList")
    private fun snapshot(
        hasCurrentSession: Boolean,
        isEmptyWelcomeRoute: Boolean = false,
        isAuthenticationRoute: Boolean,
        isUserUiBlocked: Boolean = false,
        isSessionBackedAuthenticationRoute: Boolean = false,
        isSessionTransitionInProgress: Boolean = false,
    ) = WireSessionNavigationSnapshot(
        hasCurrentSession = hasCurrentSession,
        hasCurrentRoute = true,
        isEmptyWelcomeRoute = isEmptyWelcomeRoute,
        isAuthenticationRoute = isAuthenticationRoute,
        isSessionBackedAuthenticationRoute = isSessionBackedAuthenticationRoute,
        isUserUiBlocked = isUserUiBlocked,
        isSessionTransitionInProgress = isSessionTransitionInProgress,
        isSelfLogoutTransition = false,
    )
}
