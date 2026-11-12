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
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireNavigationController
import com.wire.navigation.WireSessionId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class MultiFactorAuthenticationRecoveryCodesNavigationTest {
    private val session = WireSessionId("user", "wire.example")

    @Test
    fun `restoring routes keeps the flow owner and non-secret generation count`() {
        val root = MultiFactorAuthenticationRecoveryCodesRoute(session, remaining = 3, total = 10)
        MultiFactorAuthenticationRecoveryCodesStep.entries.forEach { step ->
            val route = root.copy(step = step, generatedCount = 10, entryId = WireNavEntryId.random())
            val restored = Json.decodeFromString<MultiFactorAuthenticationRecoveryCodesRoute>(Json.encodeToString(route))
            assertEquals(route, restored)
            assertEquals(root.flowId, restored.flowId)
            assertEquals(10, restored.generatedCount)
        }
        assertNotEquals(root.flowId, MultiFactorAuthenticationRecoveryCodesRoute(session, 3, 10).flowId)
    }

    @Test
    fun `closing the codes flow returns to the existing overview without leaving recovery screens behind`() {
        val overview = MultiFactorAuthenticationRoute(session)
        val root = MultiFactorAuthenticationRecoveryCodesRoute(session, 3, 10)
        val stack = mutableListOf<NavKey>(overview, root)
        val navigator = WireNavigationController(stack)
        navigator.navigate(
            WireNavigationCommand(root.copy(step = MultiFactorAuthenticationRecoveryCodesStep.SUMMARY, entryId = WireNavEntryId.random()))
        )
        navigator.navigate(
            WireNavigationCommand(
                root.copy(
                    step = MultiFactorAuthenticationRecoveryCodesStep.NEW_CODES,
                    generatedCount = 10,
                    entryId = WireNavEntryId.random(),
                )
            )
        )

        navigator.replaceBackStack(recoveryCodesRootBackStack(navigator.routes, root.flowId))
        assertEquals(root, navigator.currentRoute)
        navigator.goBack()
        assertEquals(listOf(overview), navigator.routes)
    }
}
