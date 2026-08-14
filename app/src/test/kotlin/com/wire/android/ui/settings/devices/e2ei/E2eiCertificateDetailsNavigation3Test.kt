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

package com.wire.android.ui.settings.devices.e2ei

import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class E2eiCertificateDetailsNavigation3Test {

    @Test
    fun givenDuringLoginLegacyDetails_whenMapping_thenCertificateAndSessionArePreserved() {
        val legacy = E2eiCertificateDetailsScreenNavArgs(
            E2EICertificateDetails.DuringLoginCertificateDetails("certificate-data")
        )

        val route = legacy.toE2eiCertificateDetailsRoute(
            sessionId = WireSessionId("user", "wire.example"),
            entryId = WireNavEntryId("certificate-entry"),
        )

        assertEquals(
            E2eiCertificateDetailsPayload.DuringLogin("certificate-data"),
            route.details,
        )
        assertEquals("app/e2ei_certificate_details_screen", route.routeId)
        assertEquals("user", route.sessionId.value)
        assertEquals(
            E2eiCertificateDetailsViewModelArgs.DuringLogin("certificate-data"),
            route.toViewModelArgs(),
        )
    }

    @Test
    fun givenAfterLoginRoute_whenSerializedAndRestored_thenKmpSafePayloadAndIdentityArePreserved() {
        val route = E2eiCertificateDetailsRoute(
            sessionId = WireSessionId("user", "wire.example"),
            details = E2eiCertificateDetailsPayload.AfterLogin(
                certificate = "certificate-data",
                userHandle = "alice",
            ),
            entryId = WireNavEntryId("certificate-entry"),
        )

        val restored = Json.decodeFromString<E2eiCertificateDetailsRoute>(
            Json.encodeToString(route)
        )

        assertEquals(route, restored)
        assertEquals("certificate-entry", restored.entryId.value)
        assertEquals(
            E2eiCertificateDetailsViewModelArgs.AfterLogin(
                certificate = "certificate-data",
                userHandle = "alice",
            ),
            restored.toViewModelArgs(),
        )
    }

    @Test
    fun givenSameArguments_whenCreatingTwoCertificateEntries_thenEntryIdentityIsDifferent() {
        val first = E2eiCertificateDetailsRoute(
            WireSessionId("user", "wire.example"),
            E2eiCertificateDetailsPayload.DuringLogin("certificate"),
        )
        val second = E2eiCertificateDetailsRoute(
            WireSessionId("user", "wire.example"),
            E2eiCertificateDetailsPayload.DuringLogin("certificate"),
        )

        assertNotEquals(first.entryId, second.entryId)
        assertEquals(first.routeId, second.routeId)
    }
}
