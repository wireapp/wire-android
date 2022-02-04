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

data class NavigationCommand(
    val destination: String,
    val backStackMode: BackStackMode = BackStackMode.NONE
    //TODO add in/out animations here
)

enum class BackStackMode {
    CLEAR_TILL_START, // clear the whole backstack excluding "start screen"
    CLEAR_WHOLE, // clear the whole backstack including "start screen" (use when you navigate to a new "start screen" )
    NONE; // screen will be added to the existing backstack.

    fun shouldClear(): Boolean = this == CLEAR_WHOLE || this == CLEAR_TILL_START
}
