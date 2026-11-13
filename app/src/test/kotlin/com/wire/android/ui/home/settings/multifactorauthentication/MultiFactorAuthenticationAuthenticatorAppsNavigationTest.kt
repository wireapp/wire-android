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
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireNavigationController
import com.wire.navigation.WireSessionId
import com.wire.navigation.WireViewModelOwner
import com.wire.navigation.availableSharedViewModelOwners
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MultiFactorAuthenticationAuthenticatorAppsNavigationTest {
    private val session = WireSessionId("user", "wire.example")

    @Test
    fun `list and editor retain the overview owner while keeping independent screen identities`() {
        val overview = MultiFactorAuthenticationRoute(session)
        val list = MultiFactorAuthenticationAuthenticatorAppsRoute(session, overview.flowId)
        val edit = MultiFactorAuthenticationEditAuthenticatorAppRoute(session, overview.flowId, "phone")

        assertTrue(list.availableSharedViewModelOwners().contains(WireViewModelOwner.Flow(overview.flowId)))
        assertTrue(edit.availableSharedViewModelOwners().contains(WireViewModelOwner.Flow(overview.flowId)))
        assertNotEquals(list.entryId, edit.entryId)
        assertEquals(list, Json.decodeFromString<MultiFactorAuthenticationAuthenticatorAppsRoute>(Json.encodeToString(list)))
        assertEquals(edit, Json.decodeFromString<MultiFactorAuthenticationEditAuthenticatorAppRoute>(Json.encodeToString(edit)))
    }

    @Test
    fun `back from editing reveals the original list then the original overview`() {
        val overview = MultiFactorAuthenticationRoute(session)
        val list = MultiFactorAuthenticationAuthenticatorAppsRoute(session, overview.flowId)
        val navigator = WireNavigationController(mutableListOf<NavKey>(overview))
        navigator.navigate(WireNavigationCommand(list))
        navigator.navigate(WireNavigationCommand(MultiFactorAuthenticationEditAuthenticatorAppRoute(session, overview.flowId, "phone")))

        navigator.goBack()
        assertEquals(list, navigator.currentRoute)
        navigator.goBack()
        assertEquals(listOf(overview), navigator.routes)
    }
}
