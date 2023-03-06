/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take

class NavigationManager {

    private val _navigateState = MutableSharedFlow<NavigationCommand?>()
    private val _navigateBack = MutableSharedFlow<Map<String, Any>>()
    var navigateState: SharedFlow<NavigationCommand?> = _navigateState
    var navigateBack: SharedFlow<Map<String, Any>> = _navigateBack

    suspend fun navigate(command: NavigationCommand) {
        // in case of DeepLink possible scenario when navigate() is called, but _navigateState Flow is not subscribed yet,
        // so the navigate command goes nowhere.
        // To avoid such lose we'll wait till _navigateState Flow is subscribed and emit command into it only after that.
        _navigateState.subscriptionCount
            .filter { it > 0 }
            .take(1)
            .collect { _navigateState.emit(command) }
    }

    suspend fun navigateBack(previousBackStackPassedArgs: Map<String, Any> = mapOf()) {
        _navigateBack.emit(previousBackStackPassedArgs)
    }
}

/**
 * Wrapper class used to specify to the Navigation Manager the new component we want to navigate to.
 */
data class NavigationCommand(
    /**
     * The destination route of the component we want to navigate to.
     */
    val destination: String,

    /**
     * Whether we want to clear the previously added screens on the backstack, only until the current one, or none of them.
     */
    val backStackMode: BackStackMode = BackStackMode.NONE,

    /**
     * A list of arguments to be stored in the savedStateHandle in case we want to pass information to the previous screen
     */
    val previousBackStackPassedArgs: List<Pair<String, Any>>? = null

    // TODO add in/out animations here
)

enum class BackStackMode {
    // clear the whole backstack excluding "start screen"
    CLEAR_TILL_START,

    // clear the whole backstack including "start screen" (use when you navigate to a new "start screen" )
    CLEAR_WHOLE,

    // remove the current item from backstack before adding the new one.
    // use it instead of:
    //  navigationManager.navigateBack()
    //  navigationManager.navigate(SomeWhere)
    REMOVE_CURRENT,

    // if there is an instance of that screen in backStack,
    // then app pops stack till that screen and replace it by the new screen.
    // if no instance in backStack, then just add screen in top of stack.
    UPDATE_EXISTED,

    // screen will be added to the existing backstack.
    NONE;
}
