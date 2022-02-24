package com.wire.android.ui.authentication


import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.ui.authentication.create.personalaccount.CreatePersonalAccountScreen
import com.wire.android.ui.authentication.devices.RemoveDeviceScreen
import com.wire.android.ui.authentication.login.LoginScreen
import com.wire.android.ui.authentication.welcome.WelcomeScreen
import com.wire.android.ui.common.UnderConstructionScreen
import com.wire.kalium.logic.configuration.ServerConfig

@Composable
fun AuthScreen() {
    val navController = rememberNavController()
    val authNavigationManager = AuthNavigationManager(navController)
    NavHost(navController = navController, startDestination = AuthDestination.Welcome.route) {
        AuthDestination.values().forEach { destination ->
            composable(route = destination.route) {
                destination.content(authNavigationManager)
            }
        }
    }
}

class AuthNavigationManager(private val navController: NavController) {
    fun navigate(authDestination: AuthDestination) = navController.navigate(authDestination.route)
    fun navigateBack() = navController.popBackStack()
}

enum class AuthDestination(val route: String) {
    Welcome(route = "welcome_screen"),
    Login(route = "login_screen"),
    RemoveDevice(route = "remove_device_screen"),
    CreatePersonalAccount(route = "create_personal_account_screen"),
    CreateTeam(route = "create_team_screen")
}

@Composable
private fun AuthDestination.content(manager: AuthNavigationManager) = when(this) {
    AuthDestination.Welcome -> WelcomeScreen(manager)
    AuthDestination.Login ->  LoginScreen(manager, ServerConfig.STAGING)
    AuthDestination.RemoveDevice -> RemoveDeviceScreen(manager)
    AuthDestination.CreatePersonalAccount -> CreatePersonalAccountScreen(manager, ServerConfig.STAGING)
    AuthDestination.CreateTeam -> UnderConstructionScreen("create_team_screen")
}
