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

package com.wire.android.navigation.routes.auth

import com.wire.android.ui.authentication.devices.register.RegisterDeviceRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RegisterDeviceRouteTest {

    @Test
    fun givenRegisterDeviceRoute_whenSerializedAndRestored_thenIdentityAndOwnershipArePreserved() {
        val route = RegisterDeviceRoute(
            sessionId = WireSessionId("user", "wire.test"),
            flowId = "authentication:user@wire.test",
            entryId = WireNavEntryId("register-device-entry"),
        )

        val restored = Json.decodeFromString<RegisterDeviceRoute>(Json.encodeToString(route))

        assertEquals(route, restored)
        assertEquals("app/register_device_screen", restored.routeId)
    }
}
