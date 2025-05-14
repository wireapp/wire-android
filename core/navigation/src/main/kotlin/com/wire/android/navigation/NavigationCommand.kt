/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.navigation

import com.ramcosta.composedestinations.spec.Direction

/**
 * Wrapper class used to specify to the Navigator the new component we want to navigate to.
 */
data class NavigationCommand(
    /**
     * The destination route of the component we want to navigate to.
     */
    val destination: Direction,

    /**
     * Whether we want to clear the previously added screens on the backstack, only until the current one, or none of them.
     */
    val backStackMode: BackStackMode = BackStackMode.NONE,

    /**
     * Whether we want to clear the backstack of the current graph.
     */
    val launchSingleTop: Boolean = true
)

enum class BackStackMode {
    // clear the whole backstack excluding "start screen"
    CLEAR_TILL_START,

    // clear the whole backstack including "start screen" (use when you navigate to a new "start screen" )
    CLEAR_WHOLE,

    // remove the current item from backstack before adding the new one.
    // use it instead of:
    //  navigator.navigateBack()
    //  navigator.navigate(SomeWhere)
    REMOVE_CURRENT,

    // Remove all items from currently entered nested graph before adding the new one.
    // For instance with screens A, B, C and D which is a nested graph containing screens D1, D2, D3,
    // if the current stack is A -> B -> D1 -> D2 -> D3 and we want to complete D flow and navigate to C,
    // then we add this flag and after navigation the stack will be A -> B -> C.
    REMOVE_CURRENT_NESTED_GRAPH,

    // if there is an instance of that screen in backStack,
    // then app pops stack till that screen and replace it by the new screen.
    // if no instance in backStack, then just add screen in top of stack.
    UPDATE_EXISTED,

    // remove the current item from backstack and then pops stack till that screen and replace it by the new screen.
    // it's REMOVE_CURRENT and UPDATE_EXISTED applied one after the other respectively
    REMOVE_CURRENT_AND_REPLACE,

    // screen will be added to the existing backstack.
    NONE;
}
