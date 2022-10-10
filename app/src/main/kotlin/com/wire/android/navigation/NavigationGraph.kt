package com.wire.android.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavigationGraph(navController: NavHostController, startDestination: String, appInitialArgs: List<Any> = emptyList()) {
    AnimatedNavHost(navController, startDestination) {
        NavigationItem.values().onEach { item ->
            composable(
                route = item.getCanonicalRoute(),
                content = { navBackStackEntry -> item.content(ContentParams(navBackStackEntry, appInitialArgs)) },
                deepLinks = item.deepLinks,
                enterTransition = { item.animationConfig.enterTransition },
                exitTransition = { item.animationConfig.exitTransition }
            )
        }
    }
}
