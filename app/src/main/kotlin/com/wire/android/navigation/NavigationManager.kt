package com.wire.android.navigation

import kotlinx.coroutines.flow.MutableSharedFlow

class NavigationManager {

    var navigateState = MutableSharedFlow<NavigationItem?>()
    var navigateBack = MutableSharedFlow<Unit>()
    var drawerState = MutableSharedFlow<Boolean>()

    suspend fun navigate(directions: NavigationItem) {
        navigateState.emit(directions)
    }

    suspend fun navigateBack() {
        navigateBack.emit(Unit)
    }

    suspend fun drawerState(isOpened: Boolean) {
        drawerState.emit(isOpened)
    }

}
