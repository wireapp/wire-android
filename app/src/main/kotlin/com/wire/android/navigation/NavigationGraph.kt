package com.wire.android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(navController, startDestination = NavigationItem.Conversations.route) {
        NavigationItem.values.forEach { item ->
            composable(route = item.route, content = item.content)
        }
    }
}
