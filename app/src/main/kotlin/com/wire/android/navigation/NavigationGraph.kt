package com.wire.android.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.wire.android.ui.authentication.AuthDestination
import com.wire.android.ui.authentication.devices.RemoveDeviceScreen
import com.wire.android.ui.authentication.login.LoginScreen
import com.wire.android.ui.authentication.welcome.WelcomeScreen
import com.wire.android.ui.common.UnderConstructionScreen
import com.wire.kalium.logic.configuration.ServerConfig

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun NavigationGraph(navController: NavHostController, startDestination: String) {
    AnimatedNavHost(navController, startDestination) {
        // Authentication screens
        composable(AuthDestination.welcomeScreen) { WelcomeScreen(navController) }
        composable(AuthDestination.loginScreen) { LoginScreen(navController, ServerConfig.STAGING) }
        composable(AuthDestination.createEnterpriseAccountScreen) { UnderConstructionScreen(AuthDestination.createEnterpriseAccountScreen) }
        composable(AuthDestination.createPrivateAccountScreen) { UnderConstructionScreen(AuthDestination.createPrivateAccountScreen) }
        composable(AuthDestination.removeDeviceScreen) { RemoveDeviceScreen(navController = navController) }

        // Main authenticated screens
        NavigationItem.values().onEach { item ->
            composable(
                route = item.getCanonicalRoute(), arguments = item.arguments, content = item.content,
                enterTransition = { item.enterTransition },
                exitTransition = { item.exitTransition },
            )
        }
    }
}
