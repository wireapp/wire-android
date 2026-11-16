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

package com.wire.android.ui.home.settings.mfa

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireSessionId
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MFARouteSerializationTest {
    private val session = WireSessionId("user", "wire.example")

    @Test
    fun `MFA routes restore through the Android navigation serializer`() {
        val routes = listOf<SessionRoute>(
            MFARoute(session),
            MFASetupRoute(session, step = MFASetupStep.VERIFY_CODE),
            MFAAddAuthenticatorAppRoute(session, step = MFASetupStep.NAME_APP),
            MFAAuthenticatorAppsRoute(session, "mfa-area"),
            MFAEditAuthenticatorAppRoute(session, "mfa-area", "phone"),
            MFARemoveAuthenticatorAppRoute(session, "mfa-area", "phone"),
            MFARecoveryCodesRoute(
                session,
                remaining = 3,
                total = 10,
                step = MFARecoveryCodesStep.NEW_CODES,
                generatedCount = 10,
            ),
            MFATurnOffRoute(session),
        )
        val serializer = NavKeySerializer<NavKey>()

        routes.forEach { route ->
            val encoded = Json.encodeToString(serializer, route)

            assertEquals(route, Json.decodeFromString(serializer, encoded))
        }
    }
}
