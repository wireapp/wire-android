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

package com.wire.android.ui.home.settings.multifactorauthentication

import androidx.navigation3.runtime.NavKey
import com.wire.android.ui.home.settings.MultiFactorAuthenticationRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavigationController
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AuthenticatorAppOperationsNavigationTest {
    private val session = WireSessionId("user", "wire.example")

    @Test
    fun `add steps restore their shared flow and never include recovery codes`() {
        val root = MultiFactorAuthenticationAddAuthenticatorAppRoute(session)
        val steps = MultiFactorAuthenticationSetupStep.entries.filterNot {
            it == MultiFactorAuthenticationSetupStep.RECOVERY_CODES
        }
        steps.forEach { step ->
            val route = root.copy(step = step, entryId = WireNavEntryId.random())
            assertEquals(route, Json.decodeFromString<MultiFactorAuthenticationAddAuthenticatorAppRoute>(Json.encodeToString(route)))
            assertEquals(root.flowId, route.flowId)
        }
        assertThrows(IllegalArgumentException::class.java) {
            root.copy(step = MultiFactorAuthenticationSetupStep.RECOVERY_CODES)
        }
    }

    @Test
    fun `add completion collapses to its original requester and removal pops both confirmation and edit`() {
        val overview = MultiFactorAuthenticationRoute(session)
        val list = MultiFactorAuthenticationAuthenticatorAppsRoute(session, overview.flowId)
        val addRoot = MultiFactorAuthenticationAddAuthenticatorAppRoute(session)
        val addName = addRoot.copy(step = MultiFactorAuthenticationSetupStep.NAME_APP, entryId = WireNavEntryId.random())
        val collapsed = addAuthenticatorRootBackStack(listOf(overview, list, addRoot, addName), addRoot.flowId)
        assertEquals(listOf(overview, list, addRoot), collapsed)

        val edit = MultiFactorAuthenticationEditAuthenticatorAppRoute(session, overview.flowId, "backup")
        val remove = MultiFactorAuthenticationRemoveAuthenticatorAppRoute(session, overview.flowId, "backup")
        assertEquals(remove, Json.decodeFromString<MultiFactorAuthenticationRemoveAuthenticatorAppRoute>(Json.encodeToString(remove)))
        val navigator = WireNavigationController(mutableListOf<NavKey>(overview, list, edit, remove))
        repeat(authenticatorAppsReturnPopCount(navigator.routes, overview.flowId)) { navigator.goBack() }
        assertEquals(listOf(overview, list), navigator.routes)
    }
}
