package com.wire.android.ui.authentication.create.personalaccount

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.wire.kalium.logic.configuration.ServerConfig

enum class CreatePersonalAccountNavigationItem(
    val route: String,
    val content: @Composable (ContentParams) -> Unit
) {
    Overview(
        route = CreatePersonalAccountDestinationsRoutes.OVERVIEW,
        content = { OverviewScreen(it.viewModel, it.serverConfig) }
    ),
    Email(
        route = CreatePersonalAccountDestinationsRoutes.EMAIL,
        content = { EmailScreen(it.viewModel, it.serverConfig) }
    ),
    Details(
        route = CreatePersonalAccountDestinationsRoutes.DETAILS,
        content = { DetailsScreen(it.viewModel) }
    )
}

object CreatePersonalAccountDestinationsRoutes {
    const val OVERVIEW = "create_personal_account_overview_screen"
    const val EMAIL = "create_personal_account_email_screen"
    const val DETAILS = "create_personal_accounts_details_screen"
}

data class ContentParams(
    val viewModel: CreatePersonalAccountViewModel,
    val serverConfig: ServerConfig,
)

internal fun navigateToItemInCreatePersonalAccount(
    navController: NavController,
    item: CreatePersonalAccountNavigationItem
) {
    navController.navigate(item.route)
}
