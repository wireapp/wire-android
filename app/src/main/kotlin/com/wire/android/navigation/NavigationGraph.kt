package com.wire.android.navigation

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wire.android.ui.authentication.login.LoginScreen
import com.wire.kalium.logic.configuration.ServerConfig

@OptIn(ExperimentalMaterialApi::class)
@ExperimentalMaterial3Api
@Composable
fun NavigationGraph(navController: NavHostController, startDestination: String, serverConfig: ServerConfig) {
    NavHost(navController, startDestination) {
        NavigationItem.values().onEach { item ->
            if (item == NavigationItem.Login)
                composable(route = item.getCanonicalRoute(), content = { LoginScreen(serverConfig) })
            else
                composable(route = item.getCanonicalRoute(), content = item.content)
        }
    }
}
