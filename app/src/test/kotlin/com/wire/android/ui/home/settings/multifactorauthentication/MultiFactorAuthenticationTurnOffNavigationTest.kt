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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MultiFactorAuthenticationTurnOffNavigationTest {
    private val session = WireSessionId("user", "wire.example")

    @Test
    fun `route restoration preserves account and entry identity`() {
        val route = MultiFactorAuthenticationTurnOffRoute(session)
        assertEquals(route, Json.decodeFromString<MultiFactorAuthenticationTurnOffRoute>(Json.encodeToString(route)))
        assertEquals(
            MultiFactorAuthenticationTurnOffResult,
            Json.decodeFromString<MultiFactorAuthenticationTurnOffResult>(Json.encodeToString(MultiFactorAuthenticationTurnOffResult)),
        )
    }

    @Test
    fun `leaving the confirmation screen returns to the original overview`() {
        val overview = MultiFactorAuthenticationRoute(session)
        val navigator = WireNavigationController(mutableListOf<NavKey>(overview))
        navigator.navigate(WireNavigationCommand(MultiFactorAuthenticationTurnOffRoute(session)))

        navigator.goBack()

        assertEquals(listOf(overview), navigator.routes)
        assertEquals(overview.entryId, navigator.currentRoute!!.entryId)
    }
}
