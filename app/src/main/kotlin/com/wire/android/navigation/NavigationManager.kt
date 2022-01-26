package com.wire.android.navigation

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow

class NavigationManager {

    var navigateState = MutableSharedFlow<NavigationItem?>()
    var navigateBack = MutableSharedFlow<Unit>()
    var drawerState = MutableSharedFlow<Boolean>()

    suspend fun navigate(directions: NavigationItem) {
        println("cyka 1")
        navigateState.emit(directions)
    }

    suspend fun navigateBack() {
        navigateBack.emit(Unit)
    }

    suspend fun drawerState(isOpened: Boolean) {
        drawerState.emit(isOpened)
    }

}
