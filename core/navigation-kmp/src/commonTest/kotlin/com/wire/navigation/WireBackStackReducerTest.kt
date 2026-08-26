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

import kotlin.test.Test
import kotlin.test.assertEquals

class WireBackStackReducerTest {

    @Test
    fun givenExistingStack_whenClearingWholeStack_thenOnlyDestinationRemains() {
        val result = reduceBackStack(
            backStack = listOf(route("start"), route("details")),
            command = command("login", WireBackStackMode.CLEAR_WHOLE),
        )

        assertEquals(listOf(route("login")), result)
    }

    @Test
    fun givenExistingStack_whenClearingTillStart_thenStartAndDestinationRemain() {
        val result = reduceBackStack(
            backStack = listOf(route("start"), route("details"), route("profile")),
            command = command("settings", WireBackStackMode.CLEAR_TILL_START),
        )

        assertEquals(listOf(route("start"), route("settings")), result)
    }

    @Test
    fun givenNestedFlowOnTop_whenRemovingCurrentFlow_thenWholeFlowIsRemoved() {
        val result = reduceBackStack(
            backStack = listOf(
                route("home"),
                route("settings"),
                route("create-name", flowId = "create-conversation"),
                route("create-members", flowId = "create-conversation"),
            ),
            command = command("conversation", WireBackStackMode.REMOVE_CURRENT_NESTED_FLOW),
        )

        assertEquals(listOf(route("home"), route("settings"), route("conversation")), result)
    }

    @Test
    fun givenDestinationAlreadyExists_whenUpdatingExisting_thenOldDestinationAndFollowingRoutesAreReplaced() {
        val result = reduceBackStack(
            backStack = listOf(route("home"), route("profile", "old"), route("details")),
            command = WireNavigationCommand(
                destination = route("profile", "new"),
                backStackMode = WireBackStackMode.UPDATE_EXISTING,
            ),
        )

        assertEquals(listOf(route("home"), route("profile", "new")), result)
    }

    @Test
    fun givenConsecutiveCopiesOnTop_whenPoppingConsecutiveRoutes_thenSingleNewCopyRemains() {
        val result = reduceBackStack(
            backStack = listOf(
                route("home"),
                route("details", "first"),
                route("details", "second"),
                route("details", "third"),
            ),
            command = WireNavigationCommand(
                destination = route("details", "replacement"),
                backStackMode = WireBackStackMode.POP_CONSECUTIVE_SAME_ROUTES,
            ),
        )

        assertEquals(listOf(route("home"), route("details", "replacement")), result)
    }

    @Test
    fun givenSameRouteOnTop_whenLaunchingSingleTop_thenArgumentsAreUpdatedWithoutAddingEntry() {
        val result = reduceBackStack(
            backStack = listOf(route("home"), route("profile", "old")),
            command = WireNavigationCommand(destination = route("profile", "new")),
        )

        assertEquals(listOf(route("home"), route("profile", "new")), result)
    }

    private fun command(routeId: String, mode: WireBackStackMode) =
        WireNavigationCommand(destination = route(routeId), backStackMode = mode)

    private fun route(routeId: String, argument: String = "", flowId: String? = null): TestRoute =
        TestRoute(routeId = routeId, argument = argument, flowId = flowId)

    private data class TestRoute(
        override val routeId: String,
        val argument: String,
        override val flowId: String?,
        override val entryId: WireNavEntryId = WireNavEntryId("$routeId-$argument-${flowId.orEmpty()}"),
    ) : WireRoute
}
