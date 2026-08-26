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

/**
 * The single mutation point for an application-owned Navigation 3 back stack.
 *
 * Navigation 3's saveable back stack is typed as [NavKey]. Wire intentionally narrows every
 * mutation to [WireRoute], so foreign keys cannot silently enter the production stack.
 */
class WireNavigationController(
    private val backStack: MutableList<NavKey>,
    private val canNavigate: (WireNavigationCommand) -> Boolean = { true },
    private val onBackStackChanged: (
        previous: List<WireRoute>,
        current: List<WireRoute>,
        change: WireBackStackChange,
    ) -> Unit = { _, _, _ -> },
) {
    init {
        require(backStack.all { it is WireRoute }) {
            "Wire navigation back stack can only contain WireRoute keys"
        }
        requireUniqueEntryIds(backStack.map { it as WireRoute })
    }

    val currentRoute: WireRoute?
        get() = backStack.lastOrNull() as? WireRoute

    val routes: List<WireRoute>
        get() = backStack.map { it as WireRoute }

    /**
     * Applies [command] atomically and reports whether it was accepted.
     */
    fun navigate(command: WireNavigationCommand): Boolean {
        if (!canNavigate(command)) return false

        val previous = routes
        val updatedStack = reduceBackStack(previous, command)
        requireUniqueEntryIds(updatedStack)
        backStack.clear()
        backStack.addAll(updatedStack)
        onBackStackChanged(previous, updatedStack, WireBackStackChange.NAVIGATE)
        return true
    }

    /**
     * Pops one route. The start route is retained unless [allowEmpty] is explicitly requested.
     */
    fun goBack(allowEmpty: Boolean = false): Boolean {
        if (backStack.isEmpty() || (!allowEmpty && backStack.size == 1)) return false
        val previous = routes
        backStack.removeLast()
        onBackStackChanged(previous, routes, WireBackStackChange.BACK)
        return true
    }

    /**
     * Replaces the complete stack, for example after resolving a deep link or session transition.
     */
    fun replaceBackStack(routes: List<WireRoute>) {
        require(routes.isNotEmpty()) { "Wire navigation back stack cannot be replaced with an empty stack" }
        requireUniqueEntryIds(routes)
        val previous = this.routes
        backStack.clear()
        backStack.addAll(routes)
        onBackStackChanged(previous, routes, WireBackStackChange.REPLACE)
    }

    private fun requireUniqueEntryIds(routes: List<WireRoute>) {
        require(routes.map(WireRoute::entryId).distinct().size == routes.size) {
            "Wire navigation back stack cannot contain duplicate entry ids"
        }
    }
}

/**
 * The intent behind a completed stack mutation.
 *
 * Result runtimes use this distinction to synthesize cancellation only for an ordinary back and
 * to prune broken relationships after non-linear replacement.
 */
enum class WireBackStackChange {
    NAVIGATE,
    BACK,
    REPLACE,
}
