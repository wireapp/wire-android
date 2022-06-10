package com.wire.android.ui.authentication.create.team

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.wire.android.ui.authentication.create.code.CreateAccountCodeScreen
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsScreen
import com.wire.android.ui.authentication.create.email.CreateAccountEmailScreen
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewScreen
import com.wire.kalium.logic.configuration.server.ServerConfig

enum class CreateTeamNavigationItem(val route: String, val content: @Composable (ContentParams) -> Unit) {
    Overview("create_team_overview_screen", { CreateAccountOverviewScreen(it.viewModel) }),
    Email("create_team_email_screen", { CreateAccountEmailScreen(it.viewModel) }),
    Details("create_team_details_screen", { CreateAccountDetailsScreen(it.viewModel) }),
    Code("create_team_code_screen", { CreateAccountCodeScreen(it.viewModel) })
}

data class ContentParams(val viewModel: CreateTeamViewModel)

internal fun navigateToItemInCreateTeam(navController: NavController, item: CreateTeamNavigationItem) {
    navController.navigate(item.route)
}
