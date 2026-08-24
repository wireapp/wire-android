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
import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.create.common.createAccountFlowPolicy
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.android.feature.authentication.R as AuthenticationR

@Composable
internal fun CreateAccountOverviewRouteScreen(
    flowType: CreateAccountRouteFlowType,
    viewModel: CreateAccountOverviewViewModel<ServerConfig.Links>,
    onNavigateBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val policy = flowType.createAccountFlowPolicy()
    CreateAccountOverviewContent(
        overviewParams = CreateAccountOverviewParams(
            title = stringResource(policy.titleResId),
            contentTitle = policy.overview.contentTitleResId?.let { stringResource(it) } ?: "",
            contentText = stringResource(policy.overview.contentTextResId),
            contentIconResId = policy.overview.contentIconResId,
            learnMoreText = policy.overview.learnMoreTextResId?.let { stringResource(it) }
                ?: stringResource(com.wire.android.R.string.label_learn_more),
            learnMoreUrl = viewModel.learnMoreUrl(),
        ),
        continueText = stringResource(com.wire.android.R.string.label_continue),
        backContentDescription = AuthenticationR.string.content_description_login_back_btn,
        onBackPressed = onNavigateBack,
        onContinuePressed = onContinue,
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
