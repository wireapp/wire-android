/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.ui.authentication.create.overview

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.create.common.createAccountFlowPolicy
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.kalium.logic.configuration.server.ServerConfig

@Composable
internal fun CreateAccountOverviewRouteScreen(
    flowType: CreateAccountRouteFlowType,
    viewModel: CreateAccountOverviewViewModel<ServerConfig.Links>,
    onNavigateBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val policy = flowType.createAccountFlowPolicy()
    with(flowType) {
        CreateAccountOverviewContent(
            overviewParams = CreateAccountOverviewParams(
                title = stringResource(id = titleResId()),
                contentTitle = policy.overview.contentTitleResId?.let(::stringResource) ?: "",
                contentText = stringResource(policy.overview.contentTextResId),
                contentIconResId = policy.overview.contentIconResId,
                learnMoreText = policy.overview.learnMoreTextResId?.let(::stringResource)
                    ?: stringResource(R.string.label_learn_more),
                learnMoreUrl = viewModel.learnMoreUrl(),
            ),
            continueText = stringResource(R.string.label_continue),
            backContentDescription = R.string.content_description_login_back_btn,
            onBackPressed = onNavigateBack,
            onContinuePressed = {
                onContinue()
            },
            onLearnMorePressed = { url -> CustomTabsHelper.launchUrl(context, url) },
            subtitleContent = {
                if (viewModel.serverConfig.isOnPremises) {
                    ServerTitle(
                        serverLinks = viewModel.serverConfig,
                        style = MaterialTheme.wireTypography.body01,
                    )
                }
            },
        )
    }
}

private fun CreateAccountRouteFlowType.titleResId(): Int = when (this) {
    CreateAccountRouteFlowType.PERSONAL -> com.wire.android.feature.authentication.R.string.create_personal_account_title
    CreateAccountRouteFlowType.TEAM -> R.string.create_team_title
}
