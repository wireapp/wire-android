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

import com.wire.android.navigation.routes.auth.AuthenticationServerLinks
import com.wire.android.navigation.routes.auth.NewLoginRoute
import com.wire.android.navigation.runtime.startup.HomeRoute
import com.wire.navigation.WireSessionId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WireNavigation3SwitchAccountActionsTest {

    @Test
    fun givenAccountWasSwitched_whenNotified_thenOnlyTypedSwitchCallbackRuns() {
        var switched = 0
        var empty = 0
        val actions = WireNavigation3SwitchAccountActions({ switched++ }, { empty++ })

        actions.switchedToAnotherAccount()

        assertEquals(1, switched)
        assertEquals(0, empty)
    }

    @Test
    fun givenNoAccountRemains_whenNotified_thenOnlyTypedEmptyCallbackRuns() {
        var switched = 0
        var empty = 0
        val actions = WireNavigation3SwitchAccountActions({ switched++ }, { empty++ })

        actions.noOtherAccountToSwitch()

        assertEquals(0, switched)
        assertEquals(1, empty)
    }

    @Test
    fun givenNewLoginIsAlreadyVisible_whenNoOtherAccountRemains_thenDoNotNavigateAgain() {
        val command = noOtherAccountNavigationCommand(
            currentRoute = NewLoginRoute.start(),
            useNewLogin = true,
        )

        assertNull(command)
    }

    @Test
    fun givenSessionRouteIsVisible_whenNoOtherAccountRemains_thenClearStackToNewLogin() {
        val command = noOtherAccountNavigationCommand(
            currentRoute = HomeRoute(WireSessionId("user", "example.com")),
            useNewLogin = true,
        )

        assertInstanceOf(NewLoginRoute::class.java, command?.destination)
    }

    @Test
    fun givenRemovedSessionBackend_whenNoOtherAccountRemains_thenPreserveBackendInNewLogin() {
        val serverLinks = AuthenticationServerLinks(
            api = "https://staging.example.com",
            accounts = "https://accounts.example.com",
            webSocket = "https://websocket.example.com",
            blackList = "https://blacklist.example.com",
            teams = "https://teams.example.com",
            website = "https://www.example.com",
            title = "Staging",
            isOnPremises = false,
            apiProxy = null,
            supportEmail = null,
        )

        val command = noOtherAccountNavigationCommand(
            currentRoute = HomeRoute(WireSessionId("user", "example.com")),
            useNewLogin = true,
            recoveryServerLinks = serverLinks,
        )

        val destination = assertInstanceOf(NewLoginRoute::class.java, command?.destination)
        assertEquals(serverLinks, destination.args.loginPasswordPath?.customServerConfig)
    }
}
