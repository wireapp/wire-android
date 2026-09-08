/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.routes.auth

import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceRoute
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireNavigationController
import com.wire.navigation.WireSessionId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AuthenticationNavigation3RouterTest {

    @Test
    fun givenRegisterDeviceTerminalStateReplays_whenCompleting_thenBackStackIsMutatedOnlyOnce() {
        val runtime = mockk<WireNavigation3Runtime>()
        val navigator = mockk<WireNavigationController>()
        val command = slot<WireNavigationCommand>()
        every { runtime.navigator } returns navigator
        every { navigator.navigate(capture(command)) } returns true
        val router = AuthenticationNavigation3Router(runtime)
        val sessionId = WireSessionId("self", "wire.test")

        repeat(10) {
            assertTrue(
                router.completeRegisterDevice(
                    eventId = "register-entry:terminal",
                    routeSessionId = sessionId,
                    flowId = "register-flow",
                    completion = RegisterDeviceCompletion.RemoveDevice,
                )
            )
        }

        verify(exactly = 1) { navigator.navigate(any()) }
        val destination = assertInstanceOf(RemoveDeviceRoute::class.java, command.captured.destination)
        assertEquals(sessionId, destination.sessionId)
        assertEquals("register-flow", destination.flowId)
    }
}
