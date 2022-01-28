package com.wire.android.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.flow.MutableSharedFlow

@ExperimentalMaterial3Api
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
    val skipBackStack: Boolean = false
    //TODO add in/out animations here
)
