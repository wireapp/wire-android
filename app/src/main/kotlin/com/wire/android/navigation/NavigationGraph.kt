package com.wire.android.navigation

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@OptIn(ExperimentalMaterialApi::class)
@ExperimentalMaterial3Api
@Composable
fun NavigationGraph(navController: NavHostController) {
    NavigationItem.globalNavigationItems.also { navItems ->
        NavHost(navController, startDestination = NavigationItem.Home.route) {
            navItems.forEach { item ->
                composable(route = item.route, content = item.content, arguments = item.arguments)
            }
        }
    }
}
