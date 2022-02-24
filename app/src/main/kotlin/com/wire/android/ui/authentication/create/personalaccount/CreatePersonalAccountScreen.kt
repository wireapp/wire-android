package com.wire.android.ui.authentication.create.personalaccount

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.ui.authentication.AuthNavigationManager
import com.wire.android.ui.common.UnderConstructionScreen
import com.wire.kalium.logic.configuration.ServerConfig

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CreatePersonalAccountScreen(
    authNavigationManager: AuthNavigationManager,
    serverConfig: ServerConfig
) {
    val viewModel: CreatePersonalAccountViewModel = hiltViewModel()
    val navController = rememberNavController()
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }
    val navigationManager = CreatePersonalAccountNavigationManager(authNavigationManager, navController)
    NavHost(navController = navController, startDestination = CreatePersonalAccountDestination.Overview.route) {
        CreatePersonalAccountDestination.values().forEach { destination ->
            composable(route = destination.route) {
                    destination.content(ContentParams(navigationManager, viewModel, serverConfig))
            }
        }
    }
}

class CreatePersonalAccountNavigationManager(
    private val authNavigationManager: AuthNavigationManager,
    private val navController: NavController
) {
    fun navigate(destination: CreatePersonalAccountDestination) {
        navController.navigate(destination.route)
    }

    fun navigateBack() {
        if (!navController.popBackStack()) authNavigationManager.navigateBack()
    }
}

enum class CreatePersonalAccountDestination(val route: String) {
    Overview(route = "create_personal_account_overview_screen"),
    Email(route = "create_personal_account_email_screen")
}

private data class ContentParams(
    val manager: CreatePersonalAccountNavigationManager,
    val viewModel: CreatePersonalAccountViewModel,
    val serverConfig: ServerConfig,
)

@Composable
private fun CreatePersonalAccountDestination.content(params: ContentParams) = when(this) {
    CreatePersonalAccountDestination.Overview ->
        OverviewScreen(navigationManager = params.manager, serverConfig = params.serverConfig)
    CreatePersonalAccountDestination.Email -> UnderConstructionScreen(CreatePersonalAccountDestination.Email.route)
}
