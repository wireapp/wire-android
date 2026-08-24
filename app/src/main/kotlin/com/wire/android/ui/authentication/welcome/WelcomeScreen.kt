/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.welcome

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.theme.WireTheme
import com.wire.kalium.logic.configuration.server.ServerConfig

/**
 * Navigation-neutral screen adapter. It keeps rendering and dialog behavior in the screen while
 * the owning runtime translates semantic actions into concrete routes.
 */
@Composable
internal fun WelcomeRouteScreen(
    viewModel: WelcomeViewModel<ServerConfig.Links>,
    onNavigateBack: () -> Unit,
    onAction: (WelcomeAction<ServerConfig.Links>) -> Unit,
) {
    WelcomeContent(
        isThereActiveSession = viewModel.state.isThereActiveSession,
        maxAccountsReached = viewModel.state.maxAccountsReached,
        nomadAccountBlocksLogin = viewModel.state.nomadAccountBlocksLogin,
        state = viewModel.state.links,
        navigateBack = onNavigateBack,
        onAction = onAction,
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
    onAction: (WelcomeAction<ServerConfig.Links>) -> Unit,
) {
    val maxDialog = rememberVisibilityState<MaxAccountsReachedDialogState>()
    val nomadDialog = rememberVisibilityState<NomadAccountBlocksLoginDialogState>()
    val enterpriseProxyDialog = rememberVisibilityState<FeatureDisabledWithProxyDialogState>()
    val personalProxyDialog = rememberVisibilityState<FeatureDisabledWithProxyDialogState>()
    MaxAccountsReachedDialog(maxDialog) { navigateBack() }
    NomadAccountBlocksLoginDialog(nomadDialog) { navigateBack() }
    FeatureDisabledWithProxyDialogContent(enterpriseProxyDialog) { onAction(WelcomeAction.OpenUrl(state.teams)) }
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
        onLogin = { onAction(WelcomeAction.Login(state)) },
        onCreateTeam = {
            when (val decision = welcomeTeamDecision(WelcomePolicy(state, state.isProxyEnabled(), ENABLE_NEW_REGISTRATION, teamCreationUrl))) {
                is WelcomeDecision.Action -> onAction(decision.value)
                is WelcomeDecision.Dialog -> enterpriseProxyDialog.show(enterpriseProxyDialog.savedState ?: FeatureDisabledWithProxyDialogState(
                    R.string.create_team_not_supported_dialog_description, (decision.value as WelcomeDialog.TeamBlockedByProxy).url,
                ))
            }
        },
        onCreatePersonal = {
            when (val decision = welcomePersonalDecision(WelcomePolicy(state, state.isProxyEnabled(), ENABLE_NEW_REGISTRATION, teamCreationUrl))) {
                is WelcomeDecision.Action -> onAction(decision.value)
                is WelcomeDecision.Dialog -> personalProxyDialog.show(personalProxyDialog.savedState ?: FeatureDisabledWithProxyDialogState(
                    R.string.create_personal_account_not_supported_dialog_description,
                ))
            }
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
            onAction = {},
        )
    }
}
