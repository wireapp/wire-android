package com.wire.android.navigation

import kotlinx.coroutines.flow.MutableSharedFlow

class NavigationManager {

    var navigateState = MutableSharedFlow<NavigationCommand?>()
    var navigateBack = MutableSharedFlow<Unit>()

    suspend fun navigate(command: NavigationCommand) {
        navigateState.emit(command)
    }

    suspend fun navigateBack() {
        navigateBack.emit(Unit)
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

    //TODO add in/out animations here
)

enum class BackStackMode {
    CLEAR_TILL_START, // clear the whole backstack excluding "start screen"
    CLEAR_WHOLE, // clear the whole backstack including "start screen" (use when you navigate to a new "start screen" )
    CLEAR_CURRENT, // navigate to next destination and clear only the current screen
    NONE; // screen will be added to the existing backstack.

    fun shouldClear(): Boolean = this == CLEAR_WHOLE || this == CLEAR_TILL_START
}
