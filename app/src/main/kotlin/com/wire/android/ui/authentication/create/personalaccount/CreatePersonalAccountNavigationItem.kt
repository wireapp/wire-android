package com.wire.android.ui.authentication.create.personalaccount

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewParams
import com.wire.android.R

enum class CreatePersonalAccountNavigationItem(
    val route: String,
    val content: @Composable (ContentParams) -> Unit
) {
    Overview(
        route = CreatePersonalAccountDestinationsRoutes.OVERVIEW,
        content = { CreateAccountOverviewScreen(
            it.viewModel,
            CreateAccountOverviewParams(
                title = stringResource(R.string.create_personal_account_title),
                contentText = stringResource(R.string.create_personal_account_text),
                contentIconResId = R.drawable.ic_create_personal_account,
                learnMoreText = stringResource(R.string.label_learn_more),
                learnMoreUrl = "https://${it.serverConfig.websiteUrl}/pricing"
            ), ) }
    ),
    Email(
        route = CreatePersonalAccountDestinationsRoutes.EMAIL,
        content = { CreateAccountEmailScreen(
                it.viewModel,
                it.serverConfig,
                stringResource(R.string.create_personal_account_title),
                stringResource(R.string.create_personal_account_email_text)
            )
        }
    ),
    Details(
        route = CreatePersonalAccountDestinationsRoutes.DETAILS,
        content = { CreateAccountDetailsScreen(it.viewModel, stringResource(R.string.create_personal_account_title)) }
    ),
    Code(
        route = CreatePersonalAccountDestinationsRoutes.CODE,
        content = { CreateAccountCodeScreen(it.viewModel, stringResource(R.string.create_personal_account_title)) }
    )
}

object CreatePersonalAccountDestinationsRoutes {
    const val OVERVIEW = "create_personal_account_overview_screen"
    const val EMAIL = "create_personal_account_email_screen"
    const val DETAILS = "create_personal_accounts_details_screen"
    const val CODE = "create_personal_accounts_code_screen"
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
