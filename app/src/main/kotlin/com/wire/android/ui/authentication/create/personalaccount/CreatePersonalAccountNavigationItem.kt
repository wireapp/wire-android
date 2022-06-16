package com.wire.android.ui.authentication.create.personalaccount

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.wire.android.ui.authentication.create.code.CreateAccountCodeScreen
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsScreen
import com.wire.android.ui.authentication.create.email.CreateAccountEmailScreen
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewScreen
import com.wire.kalium.logic.configuration.server.ServerConfig

enum class CreatePersonalAccountNavigationItem(val route: String, val content: @Composable (ContentParams) -> Unit) {
    Overview("create_personal_account_overview_screen", { CreateAccountOverviewScreen(it.viewModel) }),
    Email("create_personal_account_email_screen", { CreateAccountEmailScreen(it.viewModel) }),
    Details("create_personal_account_details_screen", { CreateAccountDetailsScreen(it.viewModel) }),
    Code("create_personal_account_code_screen", { CreateAccountCodeScreen(it.viewModel) })
}

data class ContentParams(val viewModel: CreatePersonalAccountViewModel)

internal fun navigateToItemInCreatePersonalAccount(navController: NavController, item: CreatePersonalAccountNavigationItem) {
    navController.navigate(item.route)
}
