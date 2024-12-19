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

@file:Suppress("TooManyFunctions")

package com.wire.android.ui.authentication.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.config.LocalCustomUiConfigurationProvider
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.MainBackgroundComponent
import com.wire.android.ui.authentication.ServerTitle
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogContent
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogState
import com.wire.android.ui.common.dialogs.MaxAccountsReachedDialog
import com.wire.android.ui.common.dialogs.MaxAccountsReachedDialogState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.CreatePersonalAccountOverviewScreenDestination
import com.wire.android.ui.destinations.CreateTeamAccountOverviewScreenDestination
import com.wire.android.ui.destinations.LoginScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.kalium.logic.configuration.server.ServerConfig

@RootNavGraph(start = true)
@WireDestination(
    style = PopUpNavigationAnimation::class,
)
@Composable
fun WelcomeScreen(
    navigator: Navigator,
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    WelcomeContent(
        viewModel.state.isThereActiveSession,
        viewModel.state.maxAccountsReached,
        viewModel.state.links,
        navigator::navigateBack,
        navigator::navigate
    )
}

@Composable
private fun WelcomeContent(
    isThereActiveSession: Boolean,
    maxAccountsReached: Boolean,
    state: ServerConfig.Links,
    navigateBack: () -> Unit,
    navigate: (NavigationCommand) -> Unit
) {
    WireScaffold(topBar = {
        if (isThereActiveSession) {
            WireCenterAlignedTopAppBar(
                elevation = dimensions().spacing0x,
                title = "",
                navigationIconType = NavigationIconType.Close(R.string.content_description_welcome_screen_close_btn),
                onNavigationPressed = navigateBack
            )
        } else {
//            Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.welcomeVerticalPadding))
        }
    }) { internalPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MainBackgroundComponent()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topEnd = dimensions().spacing8x, topStart = dimensions().spacing8x))
                    .align(Alignment.BottomCenter)
                    .background(colorsScheme().surface)
                    .padding(16.dp)
            ) {
                VerticalSpace.x16()
                NewWelcomeExperienceContent(
                    internalPadding = internalPadding,
                    navigateBack = navigateBack,
                    maxAccountsReached = maxAccountsReached,
                    state = state,
                    navigate = navigate
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NewWelcomeExperienceContent(
    internalPadding: PaddingValues,
    navigateBack: () -> Unit,
    maxAccountsReached: Boolean,
    state: ServerConfig.Links,
    navigate: (NavigationCommand) -> Unit,
) {
    val enterpriseDisabledWithProxyDialogState = rememberVisibilityState<FeatureDisabledWithProxyDialogState>()
    val createPersonalAccountDisabledWithProxyDialogState = rememberVisibilityState<FeatureDisabledWithProxyDialogState>()
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .padding(internalPadding)
    ) {
        val maxAccountsReachedDialogState = rememberVisibilityState<MaxAccountsReachedDialogState>()
        MaxAccountsReachedDialog(dialogState = maxAccountsReachedDialogState) { navigateBack() }
        if (maxAccountsReached) {
            maxAccountsReachedDialogState.show(maxAccountsReachedDialogState.savedState ?: MaxAccountsReachedDialogState)
        }

        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_wire_logo),
            tint = MaterialTheme.colorScheme.onBackground,
            contentDescription = stringResource(id = R.string.content_description_welcome_wire_logo),
            modifier = Modifier.size(dimensions().spacing120x)
        )

        if (state.isOnPremises) {
            ServerTitle(serverLinks = state, modifier = Modifier.padding(top = dimensions().spacing16x))
        }

        Column(
            modifier = Modifier
                .padding(
                    vertical = MaterialTheme.wireDimensions.welcomeVerticalSpacing,
                    horizontal = MaterialTheme.wireDimensions.welcomeButtonHorizontalPadding
                )
                .semantics {
                    testTagsAsResourceId = true
                }
        ) {
            LoginButton(onClick = { navigate(NavigationCommand(LoginScreenDestination())) })
            FeatureDisabledWithProxyDialogContent(
                dialogState = enterpriseDisabledWithProxyDialogState,
                onActionButtonClicked = {
                    CustomTabsHelper.launchUrl(context, state.teams)
                }
            )
            FeatureDisabledWithProxyDialogContent(dialogState = createPersonalAccountDisabledWithProxyDialogState)

            if (LocalCustomUiConfigurationProvider.current.isAccountCreationAllowed) {
                CreateEnterpriseAccountButton {
                    if (state.isProxyEnabled()) {
                        enterpriseDisabledWithProxyDialogState.show(
                            enterpriseDisabledWithProxyDialogState.savedState ?: FeatureDisabledWithProxyDialogState(
                                R.string.create_team_not_supported_dialog_description,
                                state.teams
                            )
                        )
                    } else {
                        navigate(NavigationCommand(CreateTeamAccountOverviewScreenDestination))
                    }
                }
            }
        }

        if (LocalCustomUiConfigurationProvider.current.isAccountCreationAllowed) {
            WelcomeFooter(
                modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.welcomeTextHorizontalPadding),
                onPrivateAccountClick = {
                    if (state.isProxyEnabled()) {
                        createPersonalAccountDisabledWithProxyDialogState.show(
                            createPersonalAccountDisabledWithProxyDialogState.savedState ?: FeatureDisabledWithProxyDialogState(
                                R.string.create_personal_account_not_supported_dialog_description
                            )
                        )
                    } else {
                        navigate(NavigationCommand(CreatePersonalAccountOverviewScreenDestination))
                    }
                }
            )
        }
    }
}

@Composable
private fun LoginButton(onClick: () -> Unit) {
    WirePrimaryButton(
        onClick = onClick,
        text = stringResource(R.string.label_login),
        modifier = Modifier
            .padding(bottom = MaterialTheme.wireDimensions.welcomeButtonVerticalPadding)
            .testTag("loginButton")
    )
}

@Composable
private fun CreateEnterpriseAccountButton(onClick: () -> Unit) {
    WireSecondaryButton(
        onClick = onClick,
        text = stringResource(R.string.welcome_button_create_team),
        modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.welcomeButtonVerticalPadding)
    )
}

@Composable
private fun WelcomeFooter(onPrivateAccountClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.welcome_footer_text),
            style = MaterialTheme.wireTypography.body02,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.welcome_button_create_personal_account),
            style = MaterialTheme.wireTypography.body02.copy(
                textDecoration = TextDecoration.Underline,
                color = MaterialTheme.colorScheme.primary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPrivateAccountClick,
                    onClickLabel = stringResource(R.string.content_description_open_link_label)
                )
        )

        Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.welcomeVerticalPadding))
    }
}

@Preview
@Composable
fun PreviewWelcomeScreen() {
    WireTheme {
        WelcomeContent(
            isThereActiveSession = false,
            maxAccountsReached = false,
            state = ServerConfig.DEFAULT,
            navigateBack = {},
            navigate = {})
    }
}
