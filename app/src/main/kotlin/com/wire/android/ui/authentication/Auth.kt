package com.wire.android.ui.authentication

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.ui.authentication.login.LoginScreen
import com.wire.android.ui.authentication.welcome.WelcomeScreen
import com.wire.android.ui.common.UnderConstructionScreen

@Composable
fun AuthScreen() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AuthScreenDestinations.start) {
        composable(AuthScreenDestinations.welcomeScreen) { WelcomeScreen(navController) }
        composable(AuthScreenDestinations.loginScreen) { LoginScreen(navController) }
        composable(AuthScreenDestinations.createEnterpriseAccount) { UnderConstructionScreen(AuthScreenDestinations.createEnterpriseAccount) }
        composable(AuthScreenDestinations.createPrivateAccountScreen) { UnderConstructionScreen(AuthScreenDestinations.createPrivateAccountScreen) }

    }
}

object AuthScreenDestinations {
    const val welcomeScreen: String = "welcome_screen"
    const val loginScreen: String = "login_screen"
    const val createEnterpriseAccount: String = "create_enterprise_account_screen"
    const val createPrivateAccountScreen: String = "create_private_acount_screen"
    const val start = welcomeScreen
}
