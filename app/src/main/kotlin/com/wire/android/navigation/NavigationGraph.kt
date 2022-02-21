package com.wire.android.navigation

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wire.android.ui.authentication.AuthDestination
import com.wire.android.ui.authentication.devices.RemoveDeviceScreen
import com.wire.android.ui.authentication.login.LoginScreen
import com.wire.android.ui.authentication.welcome.WelcomeScreen
import com.wire.android.ui.common.UnderConstructionScreen
import com.wire.kalium.logic.configuration.ServerConfig

@OptIn(ExperimentalMaterialApi::class)
@ExperimentalMaterial3Api
@Composable
fun NavigationGraph(navController: NavHostController, startDestination: String) {
    NavHost(navController, startDestination) {
        // Authentication screens
        composable(AuthDestination.welcomeScreen) { WelcomeScreen(navController) }
        composable(AuthDestination.loginScreen) { LoginScreen(navController, ServerConfig.STAGING) }
        composable(AuthDestination.createEnterpriseAccountScreen) { UnderConstructionScreen(AuthDestination.createEnterpriseAccountScreen) }
        composable(AuthDestination.createPrivateAccountScreen) { UnderConstructionScreen(AuthDestination.createPrivateAccountScreen) }
        composable(AuthDestination.removeDeviceScreen) { RemoveDeviceScreen(navController = navController) }

        // Main authenticated screens
        NavigationItem.values().onEach { item ->
            composable(route = item.getCanonicalRoute(), content = item.content, arguments = item.arguments)
        }
    }
}
