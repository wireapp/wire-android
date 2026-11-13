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
import com.wire.android.ui.home.settings.SettingsRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireNavigationController
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class MultiFactorAuthenticationSetupNavigationTest {
    private val session = WireSessionId("user", "wire.example")

    @Test
    fun `each step keeps the setup owner and survives route restoration`() {
        val root = MultiFactorAuthenticationSetupRoute(session)
        val steps = MultiFactorAuthenticationSetupStep.entries.filterNot { it == MultiFactorAuthenticationSetupStep.NAME_APP }
        steps.forEach { step ->
            val route = root.copy(step = step, entryId = WireNavEntryId.random())
            assertEquals(root.flowId, route.flowId)
            assertEquals(route, Json.decodeFromString<MultiFactorAuthenticationSetupRoute>(Json.encodeToString(route)))
        }
        assertNotEquals(root.flowId, MultiFactorAuthenticationSetupRoute(session).flowId)
    }

    @Test
    fun `completion removes every setup screen and returns to the original overview`() {
        val settings = SettingsRoute(session)
        val overview = MultiFactorAuthenticationRoute(session)
        val root = MultiFactorAuthenticationSetupRoute(session)
        val stack = mutableListOf<NavKey>(settings, overview, root)
        val navigator = WireNavigationController(stack)
        listOf(
            MultiFactorAuthenticationSetupStep.LINK_AUTHENTICATOR,
            MultiFactorAuthenticationSetupStep.VERIFY_CODE,
            MultiFactorAuthenticationSetupStep.RECOVERY_CODES,
        ).forEach { step ->
            navigator.navigate(WireNavigationCommand(root.copy(step = step, entryId = WireNavEntryId.random())))
        }

        navigator.replaceBackStack(setupRootBackStack(navigator.routes, root.flowId))
        assertEquals(root.entryId, navigator.currentRoute!!.entryId)
        navigator.goBack()

        assertEquals(listOf(settings, overview), navigator.routes)
        assertEquals(overview.entryId, navigator.currentRoute!!.entryId)
    }

    @Test
    fun `a restored step without in-memory enrollment can restart at password confirmation`() {
        val overview = MultiFactorAuthenticationRoute(session)
        val restored = MultiFactorAuthenticationSetupRoute(session, step = MultiFactorAuthenticationSetupStep.RECOVERY_CODES)

        val stack = setupRootBackStack(listOf(overview, restored), restored.flowId)

        val root = stack.last() as MultiFactorAuthenticationSetupRoute
        assertEquals(MultiFactorAuthenticationSetupStep.CONFIRM_PASSWORD, root.step)
        assertEquals(restored.flowId, root.flowId)
        assertEquals(restored.entryId, root.entryId)
    }
}
