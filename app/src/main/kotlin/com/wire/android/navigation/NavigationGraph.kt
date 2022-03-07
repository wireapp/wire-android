package com.wire.android.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun NavigationGraph(navController: NavHostController, startDestination: String, arguments: List<Any> = emptyList()) {
    AnimatedNavHost(navController, startDestination) {
        NavigationItem.values().onEach { item ->
            composable(
                route = item.getCanonicalRoute(),
                content = { navBackStackEntry -> item.content(ContentParams(navBackStackEntry, arguments)) },
                enterTransition = {
                    if (item.enterTransition != EnterTransition.None) item.enterTransition
                    else slideIntoContainer(
                        AnimatedContentScope.SlideDirection.Right, animationSpec = tween(200)
                    )
                },
                exitTransition = {
                    if (item.exitTransition != EnterTransition.None) item.exitTransition
                    else slideOutOfContainer(
                        AnimatedContentScope.SlideDirection.Left, animationSpec = tween(200)
                    )
                }
            )
        }
    }
}
