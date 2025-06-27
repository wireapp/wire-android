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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.wire.android.ui.authentication.create.common.CreatePersonalAccountNavGraph
import com.wire.android.ui.authentication.create.common.CreateTeamAccountNavGraph
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.CreateAccountEmailScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.kalium.logic.configuration.server.ServerConfig

@CreatePersonalAccountNavGraph(start = true)
@WireDestination(navArgsDelegate = CreateAccountOverviewNavArgs::class)
@Composable
fun CreatePersonalAccountOverviewScreen(
    navigator: Navigator,
    viewModel: CreateAccountOverviewViewModel = hiltViewModel()
) {
    CreateAccountOverviewScreen(navigator, CreateAccountFlowType.CreatePersonalAccount, viewModel)
}

@CreateTeamAccountNavGraph(start = true)
@WireDestination(navArgsDelegate = CreateAccountOverviewNavArgs::class)
@Composable
fun CreateTeamAccountOverviewScreen(
    navigator: Navigator,
    viewModel: CreateAccountOverviewViewModel = hiltViewModel()
) {
    CreateAccountOverviewScreen(navigator, CreateAccountFlowType.CreateTeam, viewModel)
}

@Composable
fun CreateAccountOverviewScreen(
    navigator: Navigator,
    flowType: CreateAccountFlowType,
    viewModel: CreateAccountOverviewViewModel,
) {
    with(flowType) {
        fun navigateToEmailScreen() {
            val createAccountNavArgs = CreateAccountNavArgs(
                flowType = this,
                customServerConfig = viewModel.navArgs.customServerConfig
            )
            navigator.navigate(NavigationCommand(CreateAccountEmailScreenDestination(createAccountNavArgs)))
        }

        OverviewContent(
            onBackPressed = navigator::navigateBack,
            onContinuePressed = ::navigateToEmailScreen,
            serverConfig = viewModel.serverConfig,
            overviewParams = CreateAccountOverviewParams(
                title = stringResource(id = titleResId),
                contentTitle = overviewResources.overviewContentTitleResId?.let { stringResource(id = it) } ?: "",
                contentText = stringResource(id = overviewResources.overviewContentTextResId),
                contentIconResId = overviewResources.overviewContentIconResId,
                learnMoreText = stringResource(id = overviewResources.overviewLearnMoreTextResId),
                learnMoreUrl = viewModel.learnMoreUrl(),
            )
        )
    }
}

@Composable
private fun OverviewContent(
    onBackPressed: () -> Unit,
    onContinuePressed: () -> Unit,
    overviewParams: CreateAccountOverviewParams,
    serverConfig: ServerConfig.Links
) {
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = dimensions().spacing0x,
                title = overviewParams.title,
                onNavigationPressed = onBackPressed,
                navigationIconType = NavigationIconType.Back(R.string.content_description_login_back_btn),
                subtitleContent = {
                    if (serverConfig.isOnPremises) {
                        ServerTitle(
                            serverLinks = serverConfig,
                            style = MaterialTheme.wireTypography.body01
                        )
                    }
                }
            )
        },
    ) { internalPadding ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(internalPadding)) {
            val context = LocalContext.current
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(id = overviewParams.contentIconResId),
                contentDescription = "",
                contentScale = ContentScale.Inside,
                modifier = Modifier
                    .padding(
                        horizontal = MaterialTheme.wireDimensions.spacing64x,
                        vertical = MaterialTheme.wireDimensions.spacing32x
                    )
                    .clearAndSetSemantics {}
            )
            OverviewTexts(
                modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing24x),
                onLearnMoreClick = { CustomTabsHelper.launchUrl(context, overviewParams.learnMoreUrl) },
                overviewParams = overviewParams,
            )
            Spacer(modifier = Modifier.weight(1f))
            WirePrimaryButton(
                text = stringResource(R.string.label_continue),
                onClick = onContinuePressed,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.wireDimensions.spacing16x),
            )
        }
    }
}

@Composable
private fun OverviewTexts(
    overviewParams: CreateAccountOverviewParams,
    modifier: Modifier = Modifier,
    onLearnMoreClick: () -> Unit
) {
    Column(modifier = modifier) {
        if (overviewParams.contentTitle.isNotEmpty()) {
            Text(
                text = overviewParams.contentTitle,
                style = MaterialTheme.wireTypography.title01,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.wireDimensions.spacing8x)
                    .clearAndSetSemantics {}
            )
        }
        Text(
            text = overviewParams.contentText,
            style = MaterialTheme.wireTypography.body02,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {}
        )
        Text(
            text = overviewParams.learnMoreText,
            style = MaterialTheme.wireTypography.body02.copy(
                textDecoration = TextDecoration.Underline
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onLearnMoreClick,
                    onClickLabel = stringResource(R.string.content_description_open_link_label)
                )
        )
    }
}

@Composable
@Preview
fun PreviewCreateAccountOverviewScreen() = WireTheme {
    OverviewContent(
        onBackPressed = { },
        onContinuePressed = { },
        overviewParams = CreateAccountOverviewParams(
            title = "title",
            contentTitle = "contentTitle",
            contentText = "contentText",
            contentIconResId = R.drawable.ic_create_personal_account,
            learnMoreText = "learn more",
            learnMoreUrl = ""
        ),
        ServerConfig.DEFAULT
    )
}
