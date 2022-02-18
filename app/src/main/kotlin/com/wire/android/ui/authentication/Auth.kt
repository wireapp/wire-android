package com.wire.android.ui.authentication

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.ui.authentication.devices.RemoveDeviceScreen
import com.wire.android.ui.authentication.login.LoginScreen
import com.wire.android.ui.authentication.welcome.WelcomeScreen
import com.wire.android.ui.common.UnderConstructionScreen
import com.wire.kalium.logic.configuration.ServerConfig

@ExperimentalMaterialApi
@Composable
fun AuthScreen() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AuthDestination.start) {
        composable(AuthDestination.welcomeScreen) { WelcomeScreen(navController) }
        composable(AuthDestination.loginScreen) { LoginScreen(navController, ServerConfig.STAGING) }
        composable(AuthDestination.createEnterpriseAccountScreen) { UnderConstructionScreen(AuthDestination.createEnterpriseAccountScreen) }
        composable(AuthDestination.createPrivateAccountScreen) { UnderConstructionScreen(AuthDestination.createPrivateAccountScreen) }
        composable(AuthDestination.removeDeviceScreen) { RemoveDeviceScreen(navController = navController) }
    }
}

object AuthDestination {
    const val welcomeScreen: String = "welcome_screen"
    const val loginScreen: String = "login_screen"
    const val createEnterpriseAccountScreen: String = "create_enterprise_account_screen"
    const val createPrivateAccountScreen: String = "create_private_account_screen"
    const val removeDeviceScreen: String = "remove_device"
    const val start = welcomeScreen
}
