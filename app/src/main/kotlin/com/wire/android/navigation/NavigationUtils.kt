package com.wire.android.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.wire.android.model.ImageAsset
import com.wire.kalium.logic.data.id.QualifiedID

@ExperimentalMaterial3Api
internal fun navigateToItem(
    navController: NavController,
    command: NavigationCommand
) {
    navController.navigate(command.destination) {
        if (command.backStackMode.shouldClear()) {
            navController.run {
                backQueue.firstOrNull { it.destination.route != null }?.let { entry ->
                    val inclusive = command.backStackMode == BackStackMode.CLEAR_WHOLE
                    val startId = entry.destination.id

                    popBackStack(startId, inclusive)
                }
            }
        }
        launchSingleTop = true
        restoreState = true
    }
}

@ExperimentalMaterial3Api
@Composable
internal fun NavController.getCurrentNavigationItem(): NavigationItem? {
    val navBackStackEntry by currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    return NavigationItem.fromRoute(currentRoute)
}

internal fun QualifiedID.mapIntoArgumentString(): String = "$domain@$value"
internal fun ImageAsset.PrivateAsset.mapIntoArgumentsString(): String = "${conversationId.mapIntoArgumentString()}:$messageId"

fun String.parseIntoQualifiedID(): QualifiedID {
    val components = split("@")
    return QualifiedID(components.last(), components.first())
}

fun String.parseIntoPrivateImageAsset(): ImageAsset.PrivateAsset {
    val (conversationIdString, messageId) = split(":")
    val conversationIdParam = conversationIdString.parseIntoQualifiedID()
    return ImageAsset.PrivateAsset(conversationIdParam, messageId)
}
