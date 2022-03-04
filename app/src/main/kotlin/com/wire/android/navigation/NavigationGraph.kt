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
fun NavigationGraph(navController: NavHostController, startDestination: String, arguments: List<Any> = emptyList()) {
    NavHost(navController, startDestination) {
        NavigationItem.values().onEach { item ->
            composable(
                route = item.getCanonicalRoute(),
                content = { navBackStackEntry -> item.content(ContentParams(navBackStackEntry, arguments)) },
                deepLinks = item.deepLinks
            )
        }
    }
}
