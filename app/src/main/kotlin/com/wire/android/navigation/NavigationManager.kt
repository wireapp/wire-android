package com.wire.android.navigation

import kotlinx.coroutines.flow.MutableSharedFlow

class NavigationManager {

    var navigateState = MutableSharedFlow<NavigationCommand?>()
    var navigateBack = MutableSharedFlow<Map<String, Any>>()

    suspend fun navigate(command: NavigationCommand) {
        navigateState.emit(command)
    }

    suspend fun navigateBack(previousBackStackPassedArgs: Map<String, Any> = mapOf()) {
        navigateBack.emit(previousBackStackPassedArgs)
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
