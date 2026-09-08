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

package com.wire.android.ui.settings.devices

import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DeviceManagementNavigation3Test {

    @Test
    fun givenDeviceDetailsRoute_whenSerializedAndRestored_thenScopeTargetAndIdentityArePreserved() {
        val route = DeviceDetailsRoute(
            sessionId = WireSessionId("current", "wire.example"),
            targetUserId = DeviceTargetUserId("other", "remote.example"),
            clientId = "client-42",
            entryId = WireNavEntryId("device-entry"),
        )

        val restored = Json.decodeFromString<DeviceDetailsRoute>(
            Json.encodeToString(route)
        )

        assertEquals(route, restored)
        assertEquals("current", restored.sessionId.value)
        assertEquals("other", restored.targetUserId.value)
        assertEquals("device-entry", restored.entryId.value)
        assertEquals("app/device_details_screen", restored.routeId)
    }

    @Test
    fun givenLegacyDeviceArgs_whenMappingToNavigationAndBack_thenTargetUserAndClientAreUnchanged() {
        val legacy = DeviceDetailsNavArgs(
            userId = UserId("other", "remote.example"),
            clientId = ClientId("client-42"),
        )

        val route = legacy.toDeviceDetailsRoute(
            sessionId = WireSessionId("current", "wire.example"),
            entryId = WireNavEntryId("device-entry"),
        )

        assertEquals(legacy, route.toLegacyNavArgs())
        assertEquals("current", route.sessionId.value)
        assertEquals("other", route.targetUserId.value)
        assertEquals(
            DeviceDetailsViewModelArgs(
                userId = UserId("other", "remote.example"),
                clientId = ClientId("client-42"),
            ),
            route.toViewModelArgs(),
        )
    }

    @Test
    fun givenSameArguments_whenCreatingTwoSelfDeviceEntries_thenEntryIdentityIsDifferent() {
        val first = SelfDevicesRoute(WireSessionId("user", "wire.example"))
        val second = SelfDevicesRoute(WireSessionId("user", "wire.example"))

        assertNotEquals(first.entryId, second.entryId)
        assertEquals("app/self_devices_screen", first.routeId)
        assertEquals(first.routeId, second.routeId)
    }

    @Test
    fun givenBlankTargetOrClientIdentity_whenCreatingDeviceRoute_thenItIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            DeviceTargetUserId("", "wire.example")
        }
        assertThrows(IllegalArgumentException::class.java) {
            DeviceDetailsRoute(
                sessionId = WireSessionId("current", "wire.example"),
                targetUserId = DeviceTargetUserId("other", "remote.example"),
                clientId = "",
            )
        }
    }
}
