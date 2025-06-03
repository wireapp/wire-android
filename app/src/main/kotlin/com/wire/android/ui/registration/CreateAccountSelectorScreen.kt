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

package com.wire.android.ui.registration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.navigation.style.AuthPopUpNavigationAnimation
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.preview.EdgeToEdgePreview
import com.wire.android.ui.destinations.CreateAccountEmailScreenDestination
import com.wire.android.ui.newauthentication.login.NewLoginContainer
import com.wire.android.ui.newauthentication.login.NewLoginHeader
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.configuration.server.ServerConfig

@RootNavGraph
@WireDestination(
    navArgsDelegate = CreateAccountSelectorNavArgs::class,
    style = AuthPopUpNavigationAnimation::class
)
@Composable
fun CreateAccountSelectorScreen(
    navigator: Navigator,
    viewModel: CreateAccountSelectorViewModel = hiltViewModel()
) {
    CreateAccountSelectorContent(navigator, CreateAccountFlowType.CreatePersonalAccount, viewModel)
}

//@CreateTeamAccountNavGraph(start = true)
//@WireDestination(navArgsDelegate = CreateAccountOverviewNavArgs::class)
//@Composable
//fun CreateTeamAccountOverviewScreen(
//    navigator: Navigator,
//    viewModel: CreateAccountOverviewViewModel = hiltViewModel()
//) {
//    CreateAccountOverviewScreen(navigator, CreateAccountFlowType.CreateTeam, viewModel)
//}

@Composable
fun CreateAccountSelectorContent(
    navigator: Navigator,
    flowType: CreateAccountFlowType,
    viewModel: CreateAccountSelectorViewModel,
) {
    with(flowType) {
        fun navigateToEmailScreen() {
            val createAccountNavArgs = CreateAccountNavArgs(
                flowType = this,
                customServerConfig = viewModel.navArgs.customServerConfig
            )
            navigator.navigate(NavigationCommand(CreateAccountEmailScreenDestination(createAccountNavArgs)))
        }

        AccountTypes(
            onBackPressed = navigator::navigateBack,
            onContinuePressed = ::navigateToEmailScreen,
            serverConfig = viewModel.serverConfig,
//            overviewParams = CreateAccountOverviewParams(
//                title = stringResource(id = titleResId),
//                contentTitle = overviewResources.overviewContentTitleResId?.let { stringResource(id = it) } ?: "",
//                contentText = stringResource(id = overviewResources.overviewContentTextResId),
//                contentIconResId = overviewResources.overviewContentIconResId,
//                learnMoreText = stringResource(id = overviewResources.overviewLearnMoreTextResId),
//                learnMoreUrl = viewModel.learnMoreUrl(),
//            )
        )
    }
}

@Composable
private fun AccountTypes(
    onBackPressed: () -> Unit,
    onContinuePressed: () -> Unit,
    serverConfig: ServerConfig.Links
) {
    NewLoginContainer(
        header = {
            NewLoginHeader(
                title = {
                    Text(text = stringResource(id = R.string.create_account_selector_title), style = MaterialTheme.wireTypography.title01)
                    if (serverConfig.isOnPremises) {
                        ServerTitle(
                            serverLinks = serverConfig,
                            style = MaterialTheme.wireTypography.body01
                        )
                    }
                },
                canNavigateBack = true,
                onNavigateBack = onBackPressed
            )
        },
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val context = LocalContext.current
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
    )
}


@Composable
@Preview
fun PreviewCreateAccountOverviewScreen() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            AccountTypes(
                onBackPressed = { },
                onContinuePressed = { },
                ServerConfig.DEFAULT
            )
        }
    }
}
