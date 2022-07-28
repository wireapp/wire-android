package com.wire.android.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import cafe.adriel.voyager.navigator.Navigator
import com.wire.android.appLogger

@ExperimentalMaterial3Api
internal fun Navigator.navigateToItem(command: NavigationCommand) {
    when (command.backStackMode) {
        BackStackMode.CLEAR_WHOLE -> {
            replaceAll(command.destinations[0])
            if (command.destinations.size > 1) push(command.destinations.drop(1))
        }
        BackStackMode.REMOVE_CURRENT -> {
            replace(command.destinations[0])
            if (command.destinations.size > 1) push(command.destinations.drop(1))
        }
        BackStackMode.UPDATE_EXISTED ->
            if (items.any { it.key == command.destinations[0].key }) {
                popUntil { it.key == command.destinations[0].key }
                replace(command.destinations[0])
                if (command.destinations.size > 1) push(command.destinations.drop(1))
            } else {
                push(command.destinations)
            }
        BackStackMode.NONE -> push(command.destinations)
    }
}

internal fun Navigator.getCurrentNavigationItem(): VoyagerNavigationItem? =
    this.lastItemOrNull?.let { it as? VoyagerNavigationItem }
