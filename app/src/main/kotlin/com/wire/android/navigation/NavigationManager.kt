package com.wire.android.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.flow.MutableSharedFlow

@OptIn(ExperimentalMaterial3Api::class)
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
    val backStackMode: BackStackMode = BackStackMode.NONE
    //TODO add in/out animations here
)

enum class BackStackMode {
    CLEAR_TILL_START, // clear the whole backstack excluding "start screen"
    CLEAR_WHOLE, // clear the whole backstack including "start screen" (use when you navigate to a new "start screen" )
    NONE; // screen will be added to the existing backstack.

    fun shouldClear(): Boolean = this == CLEAR_WHOLE || this == CLEAR_TILL_START
}
