/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.ui.authentication.create.team

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.wire.android.navigation.getPrimaryRoute
import com.wire.android.ui.authentication.create.code.CreateAccountCodeScreen
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsScreen
import com.wire.android.ui.authentication.create.email.CreateAccountEmailScreen
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewScreen

enum class CreateTeamNavigationItem(val route: String, val content: @Composable (ContentParams) -> Unit) {
    Overview("create_team_overview_screen", { CreateAccountOverviewScreen(it.viewModel, it.viewModel.serverConfig) }),
    Email("create_team_email_screen", { CreateAccountEmailScreen(it.viewModel, it.viewModel.serverConfig) }),
    Details("create_team_details_screen", { CreateAccountDetailsScreen(it.viewModel, it.viewModel.serverConfig) }),
    Code("create_team_code_screen", { CreateAccountCodeScreen(it.viewModel, it.viewModel.serverConfig) });

    val itemName: String get() = ITEM_NAME_PREFIX + this.name

    companion object {
        private const val ITEM_NAME_PREFIX = "CreateTeamNavigationItem."
        private val map = CreateTeamNavigationItem.values().associateBy { it.route }
        fun fromRoute(fullRoute: String): CreateTeamNavigationItem? = map[fullRoute.getPrimaryRoute()]
    }
}

data class ContentParams(val viewModel: CreateTeamViewModel)

internal fun navigateToItemInCreateTeam(navController: NavController, item: CreateTeamNavigationItem) {
    navController.navigate(item.route)
}
