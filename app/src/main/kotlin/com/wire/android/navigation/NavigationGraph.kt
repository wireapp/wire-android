package com.wire.android.navigation

import androidx.compose.animation.ExperimentalAnimationApi
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
fun NavigationGraph(navController: NavHostController, startDestination: String) {
    NavigationItem.values().also { navItems ->
        AnimatedNavHost(navController, startDestination) {
            navItems.forEach { item ->
                composable(
                    route = item.getCanonicalRoute(), arguments = item.arguments, content = item.content,
                    enterTransition = { item.enterTransition },
                    exitTransition = { item.exitTransition },
                )
            }
        }
    }
}
