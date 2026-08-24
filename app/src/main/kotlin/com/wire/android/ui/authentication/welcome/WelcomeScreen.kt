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

@file:Suppress("TooManyFunctions", "MatchingDeclarationName")

package com.wire.android.ui.authentication.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.BuildConfig.ENABLE_NEW_REGISTRATION
import com.wire.android.R
import com.wire.android.config.LocalCustomUiConfigurationProvider
import com.wire.android.feature.authentication.R as AuthenticationR
import com.wire.android.ui.authentication.MissingBackendConfigContent
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.isConfigured
import com.wire.android.ui.common.R as commonR
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogContent
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogState
import com.wire.android.ui.common.dialogs.MaxAccountsReachedDialog
import com.wire.android.ui.common.dialogs.MaxAccountsReachedDialogState
import com.wire.android.ui.common.dialogs.NomadAccountBlocksLoginDialog
import com.wire.android.ui.common.dialogs.NomadAccountBlocksLoginDialogState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.configuration.server.ServerConfig

internal sealed interface WelcomeScreenAction {
    data class Login(val serverConfig: ServerConfig.Links) : WelcomeScreenAction
    data class OpenUrl(val url: String) : WelcomeScreenAction
    data class CreateTeam(val serverConfig: ServerConfig.Links) : WelcomeScreenAction
    data class CreatePersonal(val serverConfig: ServerConfig.Links) : WelcomeScreenAction
    data class CreateAccountData(val serverConfig: ServerConfig.Links) : WelcomeScreenAction
}

/**
 * Navigation-neutral screen adapter. It keeps rendering and dialog behavior in the screen while
 * the owning runtime translates semantic actions into concrete routes.
 */
@Composable
internal fun WelcomeRouteScreen(
    viewModel: WelcomeViewModel<ServerConfig.Links>,
    onNavigateBack: () -> Unit,
    onAction: (WelcomeScreenAction) -> Unit,
) {
    WelcomeContent(
        viewModel.state.isThereActiveSession,
        viewModel.state.maxAccountsReached,
        viewModel.state.nomadAccountBlocksLogin,
        viewModel.state.links,
        onNavigateBack,
        onAction,
    )
}

internal fun ServerConfig.Links.isProxyEnabled() = apiProxy != null

@Composable
private fun WelcomeContent(
    isThereActiveSession: Boolean,
    maxAccountsReached: Boolean,
    nomadAccountBlocksLogin: Boolean,
    state: ServerConfig.Links,
    navigateBack: () -> Unit,
    onAction: (WelcomeScreenAction) -> Unit,
) {
    val maxDialog = rememberVisibilityState<MaxAccountsReachedDialogState>()
    val nomadDialog = rememberVisibilityState<NomadAccountBlocksLoginDialogState>()
    val enterpriseProxyDialog = rememberVisibilityState<FeatureDisabledWithProxyDialogState>()
    val personalProxyDialog = rememberVisibilityState<FeatureDisabledWithProxyDialogState>()
    MaxAccountsReachedDialog(maxDialog) { navigateBack() }
    NomadAccountBlocksLoginDialog(nomadDialog) { navigateBack() }
    FeatureDisabledWithProxyDialogContent(enterpriseProxyDialog) { onAction(WelcomeScreenAction.OpenUrl(state.teams)) }
    FeatureDisabledWithProxyDialogContent(personalProxyDialog)
    if (maxAccountsReached) maxDialog.show(maxDialog.savedState ?: MaxAccountsReachedDialogState)
    if (nomadAccountBlocksLogin) nomadDialog.show(nomadDialog.savedState ?: NomadAccountBlocksLoginDialogState)
    val accountCreationAllowed = LocalCustomUiConfigurationProvider.current.isAccountCreationAllowed
    val teamCreationUrl = state.teams + stringResource(R.string.create_account_email_backlink_to_team_suffix_url)
    com.wire.android.ui.authentication.welcome.WelcomeScreenContent(
        state = com.wire.android.ui.authentication.welcome.WelcomePresentationState(
            showCloseButton = isThereActiveSession,
            accountCreationAllowed = accountCreationAllowed,
            showTeamCreation = true,
            showPersonalCreation = true,
            carouselDelayMillis = integerResource(AuthenticationR.integer.welcome_carousel_item_time_ms).toLong(),
            carouselPages = welcomeCarouselPages(),
        ),
        loginLabel = stringResource(R.string.label_login),
        createTeamLabel = stringResource(R.string.welcome_button_create_team),
        footerText = stringResource(R.string.welcome_footer_text),
        createPersonalLabel = stringResource(R.string.welcome_button_create_personal_account),
        openLinkDescription = stringResource(commonR.string.content_description_open_link_label),
        closeContentDescription = R.string.content_description_welcome_screen_close_btn,
        onClose = navigateBack,
        onLogin = { onAction(WelcomeScreenAction.Login(state)) },
        onCreateTeam = {
            if (state.isProxyEnabled()) {
                enterpriseProxyDialog.show(
                    enterpriseProxyDialog.savedState ?: FeatureDisabledWithProxyDialogState(
                        R.string.create_team_not_supported_dialog_description,
                        state.teams,
                    )
                )
            }
            else if (ENABLE_NEW_REGISTRATION) onAction(WelcomeScreenAction.OpenUrl(teamCreationUrl))
            else onAction(WelcomeScreenAction.CreateTeam(state))
        },
        onCreatePersonal = {
            if (state.isProxyEnabled()) {
                personalProxyDialog.show(
                    personalProxyDialog.savedState ?: FeatureDisabledWithProxyDialogState(
                        R.string.create_personal_account_not_supported_dialog_description,
                    )
                )
            }
            else if (ENABLE_NEW_REGISTRATION) onAction(WelcomeScreenAction.CreateAccountData(state))
            else onAction(WelcomeScreenAction.CreatePersonal(state))
        },
        logoContent = {
            Icon(
                ImageVector.vectorResource(R.drawable.ic_wire_logo),
                null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        },
        serverTitleContent = {
            if (state.isOnPremises) {
                ServerTitle(
                    state,
                    Modifier.padding(
                        top = dimensions().spacing16x,
                        start = dimensions().spacing32x,
                        end = dimensions().spacing32x,
                    ),
                )
            }
        },
        bodyOverride = if (!state.isConfigured()) ({
            MissingBackendConfigContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.wireDimensions.welcomeButtonHorizontalPadding)
                    .weight(1f, true),
                showTitle = true, centerText = true, verticalArrangement = Arrangement.Center,
            )
        }) else null,
    )
}

@Composable
private fun welcomeCarouselPages() = listOf(
    com.wire.android.ui.authentication.welcome.WelcomeCarouselPage(
        AuthenticationR.drawable.ic_welcome_1,
        stringResource(AuthenticationR.string.welcome_screen_carousel_item_message_1),
    ),
    com.wire.android.ui.authentication.welcome.WelcomeCarouselPage(
        AuthenticationR.drawable.ic_welcome_2,
        stringResource(AuthenticationR.string.welcome_screen_carousel_item_message_2),
    ),
    com.wire.android.ui.authentication.welcome.WelcomeCarouselPage(
        AuthenticationR.drawable.ic_welcome_3,
        stringResource(AuthenticationR.string.welcome_screen_carousel_item_message_3),
    ),
    com.wire.android.ui.authentication.welcome.WelcomeCarouselPage(
        AuthenticationR.drawable.ic_welcome_4,
        stringResource(AuthenticationR.string.welcome_screen_carousel_item_message_4),
    ),
    com.wire.android.ui.authentication.welcome.WelcomeCarouselPage(
        AuthenticationR.drawable.ic_welcome_5,
        stringResource(AuthenticationR.string.welcome_screen_carousel_item_message_5),
    ),
)

@Preview
@Composable
fun PreviewWelcomeScreen() {
    WireTheme {
        WelcomeContent(
            isThereActiveSession = false,
            maxAccountsReached = false,
            nomadAccountBlocksLogin = false,
            state = ServerConfig.DEFAULT,
            navigateBack = {},
            onAction = {}
        )
    }
}
