/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.configuration.server.ServerConfig

@Composable
internal fun WelcomeContent(
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
    val teamCreationUrl = state.teams + stringResource(AuthenticationR.string.create_account_email_backlink_to_team_suffix_url)
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
        createTeamLabel = stringResource(AuthenticationR.string.welcome_button_create_team),
        footerText = stringResource(R.string.welcome_footer_text),
        createPersonalLabel = stringResource(R.string.welcome_button_create_personal_account),
        openLinkDescription = stringResource(commonR.string.content_description_open_link_label),
        closeContentDescription = R.string.content_description_welcome_screen_close_btn,
        onClose = navigateBack,
        onLogin = { onAction(WelcomeScreenAction.Login(state)) },
        onCreateTeam = { state.createTeam(teamCreationUrl, enterpriseProxyDialog, onAction) },
        onCreatePersonal = { state.createPersonal(personalProxyDialog, onAction) },
        logoContent = {
            Icon(
                ImageVector.vectorResource(R.drawable.ic_wire_logo),
                null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        },
        serverTitleContent = { state.ServerTitleContent() },
        bodyOverride = if (!state.isConfigured()) ({
            MissingBackendConfigContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.wireDimensions.welcomeButtonHorizontalPadding)
                    .weight(1f, true),
                showTitle = true,
                centerText = true,
                verticalArrangement = Arrangement.Center,
            )
        }) else null,
    )
}

@Composable
private fun ServerConfig.Links.ServerTitleContent() {
    if (isOnPremises) {
        ServerTitle(
            serverLinks = this,
            modifier = Modifier.padding(
                top = dimensions().spacing16x,
                start = dimensions().spacing32x,
                end = dimensions().spacing32x,
            ),
        )
    }
}
