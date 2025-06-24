/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

package com.wire.android.ui.registration.selector

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.config.orDefault
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.navigation.style.AuthPopUpNavigationAnimation
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.create.common.CreateAccountNavGraph
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.create.common.UserRegistrationInfo
import com.wire.android.ui.authentication.login.PreFilledUserIdentifierType
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.EdgeToEdgePreview
import com.wire.android.ui.destinations.CreateAccountDataDetailScreenDestination
import com.wire.android.ui.destinations.NewLoginPasswordScreenDestination
import com.wire.android.ui.newauthentication.login.NewAuthContainer
import com.wire.android.ui.newauthentication.login.NewAuthHeader
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.configuration.server.ServerConfig

@CreateAccountNavGraph(start = true)
@WireDestination(
    navArgsDelegate = CreateAccountSelectorNavArgs::class,
    style = AuthPopUpNavigationAnimation::class
)
@Composable
fun CreateAccountSelectorScreen(
    navigator: Navigator,
    viewModel: CreateAccountSelectorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    fun navigateToEmailScreen() {
        val createAccountNavArgs = CreateAccountDataNavArgs(
            customServerConfig = viewModel.serverConfig.orDefault(),
            userRegistrationInfo = UserRegistrationInfo(viewModel.email)
        )
        navigator.navigate(NavigationCommand(CreateAccountDataDetailScreenDestination(createAccountNavArgs)))
    }

    val startForResult = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        navigator.navigate(
            NavigationCommand(
                NewLoginPasswordScreenDestination(PreFilledUserIdentifierType.PreFilled(viewModel.email)),
                BackStackMode.REMOVE_CURRENT_NESTED_GRAPH
            )
        )
    }

    val teamAccountCreationUrl =
        viewModel.teamAccountCreationUrl + stringResource(R.string.create_account_email_backlink_to_team_suffix_url)

    fun navigateToTeamScreen() {
        val customTabsIntent = CustomTabsHelper.buildCustomTabIntent(context)
        customTabsIntent.intent.setData(teamAccountCreationUrl.toUri())
        startForResult.launch(customTabsIntent.intent)
    }

    CreateAccountSelectorContent(
        customServerLinks = viewModel.serverConfig,
        onPersonalAccountCreationClicked = ::navigateToEmailScreen,
        onTeamAccountCreationClicked = ::navigateToTeamScreen,
        onNavigateBack = navigator::navigateBack,
    )
}

@SuppressLint("ComposeModifierMissing")
@Composable
fun CreateAccountSelectorContent(
    customServerLinks: ServerConfig.Links?,
    onNavigateBack: () -> Unit,
    onPersonalAccountCreationClicked: () -> Unit,
    onTeamAccountCreationClicked: () -> Unit,
) {
    NewAuthContainer(
        header = {
            NewAuthHeader(
                title = {
                    Text(
                        text = stringResource(id = R.string.create_account_selector_title),
                        style = MaterialTheme.wireTypography.title01
                    )
                    if (customServerLinks?.isOnPremises == true) {
                        ServerTitle(
                            serverLinks = customServerLinks,
                            style = MaterialTheme.wireTypography.body01
                        )
                    }
                },
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        contentPadding = dimensions().spacing16x,
        content = {
            AccountType(
                title = stringResource(id = R.string.create_account_selector_team_title),
                subtitle = stringResource(id = R.string.create_account_selector_team_subtitle),
                highlights = listOf(
                    stringResource(id = R.string.create_account_selector_team_highlight_one),
                    stringResource(id = R.string.create_account_selector_team_highlight_two)
                ),
                accountTypeStyling = AccountTypeStyling(
                    containerBorderColor = MaterialTheme.colorScheme.primary,
                    shouldUsePrimaryButton = true
                ),
                onContinueButtonText = stringResource(R.string.create_team_title),
                onContinuePressed = onTeamAccountCreationClicked
            )

            AccountType(
                title = stringResource(id = R.string.create_account_selector_personal_title),
                subtitle = stringResource(id = R.string.create_account_selector_personal_subtitle),
                highlights = listOf(
                    stringResource(id = R.string.create_account_selector_personal_highlight_one),
                    stringResource(id = R.string.create_account_selector_personal_highlight_two)
                ),
                accountTypeStyling = AccountTypeStyling(
                    containerBorderColor = MaterialTheme.colorScheme.outline,
                    shouldUsePrimaryButton = false
                ),
                onContinueButtonText = stringResource(R.string.create_personal_account_title),
                onContinuePressed = onPersonalAccountCreationClicked
            )
        }
    )
}

/**
 * Metadata for the account type container styles.
 */
data class AccountTypeStyling(
    val containerBorderColor: Color,
    val shouldUsePrimaryButton: Boolean,
)

@Composable
private fun AccountType(
    title: String,
    subtitle: String,
    highlights: List<String>,
    accountTypeStyling: AccountTypeStyling,
    onContinueButtonText: String,
    onContinuePressed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(bottom = dimensions().spacing24x, start = dimensions().spacing16x, end = dimensions().spacing16x)
            .border(
                border = BorderStroke(dimensions().spacing1x, accountTypeStyling.containerBorderColor),
                shape = RoundedCornerShape(dimensions().spacing24x)
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.wireDimensions.spacing12x),
            modifier = Modifier
                .padding(vertical = dimensions().spacing16x, horizontal = dimensions().spacing16x)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.wireTypography.title03,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = dimensions().spacing8x)
                    .fillMaxWidth()
            )
            Text(
                text = subtitle,
                style = MaterialTheme.wireTypography.body01,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = dimensions().spacing8x)
                    .fillMaxWidth()
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = dimensions().spacing16x))
            highlights.forEach { highlight ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .padding(horizontal = dimensions().spacing16x)
                        .fillMaxWidth()
                ) {
                    Icon(
                        modifier = Modifier
                            .size(dimensions().spacing16x),
                        imageVector = Icons.Filled.CheckCircle,
                        tint = colorsScheme().positive,
                        contentDescription = null,
                    )
                    Text(
                        text = highlight,
                        style = MaterialTheme.wireTypography.body01,
                        modifier = Modifier.padding(start = dimensions().spacing8x)
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = dimensions().spacing12x))
            }

            when {
                accountTypeStyling.shouldUsePrimaryButton -> {
                    WirePrimaryButton(
                        text = onContinueButtonText,
                        onClick = onContinuePressed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = dimensions().spacing8x)
                    )
                }

                else -> {
                    WireSecondaryButton(
                        text = onContinueButtonText,
                        onClick = onContinuePressed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = dimensions().spacing8x)
                    )
                }
            }
        }
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewCreateAccountSelectorScreen() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            CreateAccountSelectorContent(
                customServerLinks = null,
                onNavigateBack = {},
                onPersonalAccountCreationClicked = {},
                onTeamAccountCreationClicked = {}
            )
        }
    }
}
