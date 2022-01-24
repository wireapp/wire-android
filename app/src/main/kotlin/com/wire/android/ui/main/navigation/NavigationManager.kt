package com.wire.android.ui.main.navigation

import kotlinx.coroutines.flow.MutableStateFlow

class NavigationManager {

    var commands = MutableStateFlow(MainNavigationScreenItem.Conversations)

    fun navigate(directions: MainNavigationScreenItem) {
        commands.value = directions
    }

}
