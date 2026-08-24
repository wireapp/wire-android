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

import com.wire.android.ui.authentication.devices.remove.RemoveDeviceRoute
import com.wire.android.ui.e2eiEnrollment.E2EIEnrollmentRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PostLoginAuthenticationRouteTest {

    @Test
    fun givenInitialSyncRoute_whenSerializedAndRestored_thenIdentityAndOwnershipArePreserved() {
        val route = InitialSyncRoute(SESSION_ID, WireNavEntryId("initial-sync-entry"))

        val restored = Json.decodeFromString<InitialSyncRoute>(Json.encodeToString(route))

        assertEquals(route, restored)
        assertEquals("app/initial_sync_screen", restored.routeId)
    }

    @Test
    fun givenRemoveDeviceRoute_whenSerializedAndRestored_thenIdentityAndOwnershipArePreserved() {
        val route = RemoveDeviceRoute(
            sessionId = SESSION_ID,
            flowId = "authentication:user@wire.test",
            entryId = WireNavEntryId("remove-device-entry"),
        )

        val restored = Json.decodeFromString<RemoveDeviceRoute>(Json.encodeToString(route))

        assertEquals(route, restored)
        assertEquals("app/remove_device_screen", restored.routeId)
    }

    @Test
    fun givenE2eiEnrollmentRoute_whenSerializedAndRestored_thenIdentityAndOwnershipArePreserved() {
        val route = E2EIEnrollmentRoute(
            sessionId = SESSION_ID,
            flowId = "authentication:user@wire.test",
            entryId = WireNavEntryId("e2ei-enrollment-entry"),
        )

        val restored = Json.decodeFromString<E2EIEnrollmentRoute>(Json.encodeToString(route))

        assertEquals(route, restored)
        assertEquals("app/e2_e_i_enrollment_screen", restored.routeId)
    }

    private companion object {
        val SESSION_ID = WireSessionId("user", "wire.test")
    }
}
