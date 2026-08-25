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

package com.wire.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WireNavigationControllerTest {

    @Test
    fun givenAcceptedCommand_whenNavigating_thenBackStackIsUpdated() {
        val backStack = mutableListOf<NavKey>(route("home"))
        val controller = WireNavigationController(backStack)

        val accepted = controller.navigate(WireNavigationCommand(route("settings")))

        assertTrue(accepted)
        assertEquals(listOf(route("home"), route("settings")), controller.routes)
    }

    @Test
    fun givenRejectedCommand_whenNavigating_thenBackStackIsUnchanged() {
        val backStack = mutableListOf<NavKey>(route("home"))
        val controller = WireNavigationController(backStack, canNavigate = { false })

        val accepted = controller.navigate(WireNavigationCommand(route("settings")))

        assertFalse(accepted)
        assertEquals(listOf(route("home")), controller.routes)
    }

    @Test
    fun givenOnlyStartRoute_whenGoingBack_thenStartRouteIsRetained() {
        val controller = WireNavigationController(mutableListOf<NavKey>(route("home")))

        assertFalse(controller.goBack())
        assertEquals(listOf(route("home")), controller.routes)
    }

    @Test
    fun givenResolvedDeepLinkStack_whenReplacingBackStack_thenWholeStackIsReplaced() {
        val controller = WireNavigationController(mutableListOf<NavKey>(route("home"), route("settings")))

        controller.replaceBackStack(listOf(route("home"), route("conversation")))

        assertEquals(listOf(route("home"), route("conversation")), controller.routes)
    }

    @Test
    fun givenEmptyStack_whenReplacingBackStack_thenOperationIsRejected() {
        val controller = WireNavigationController(mutableListOf<NavKey>(route("home")))

        assertFailsWith<IllegalArgumentException> {
            controller.replaceBackStack(emptyList())
        }
    }

    @Test
    fun givenExistingEntryId_whenNavigatingToSameEntry_thenOperationIsRejected() {
        val existingRoute = route("home")
        val controller = WireNavigationController(mutableListOf<NavKey>(existingRoute))

        assertFailsWith<IllegalArgumentException> {
            controller.navigate(WireNavigationCommand(existingRoute, launchSingleTop = false))
        }
        assertEquals(listOf(existingRoute), controller.routes)
    }

    @Test
    fun givenDuplicateEntryIds_whenReplacingBackStack_thenOperationIsRejected() {
        val controller = WireNavigationController(mutableListOf<NavKey>(route("home")))
        val duplicatedEntryId = WireNavEntryId("duplicate-entry")

        assertFailsWith<IllegalArgumentException> {
            controller.replaceBackStack(
                listOf(
                    TestRoute("home", duplicatedEntryId),
                    TestRoute("settings", duplicatedEntryId),
                )
            )
        }
        assertEquals(listOf(route("home")), controller.routes)
    }

    private fun route(id: String) = TestRoute(id)

    private data class TestRoute(
        override val routeId: String,
        override val entryId: WireNavEntryId = WireNavEntryId("$routeId-entry"),
    ) : WireRoute
}
