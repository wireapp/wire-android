/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.welcome

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import com.wire.android.BuildConfig.ENABLE_NEW_REGISTRATION
import com.wire.android.R
import com.wire.android.config.LocalCustomUiConfigurationProvider
import com.wire.android.feature.authentication.R as AuthenticationR
import com.wire.android.ui.common.R as CommonR
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogContent
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogState
import com.wire.android.ui.common.dialogs.MaxAccountsReachedDialog
import com.wire.android.ui.common.dialogs.MaxAccountsReachedDialogState
import com.wire.android.ui.common.dialogs.NomadAccountBlocksLoginDialog
import com.wire.android.ui.common.dialogs.NomadAccountBlocksLoginDialogState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.kalium.logic.configuration.server.ServerConfig

@Composable
internal fun WelcomeContent(
    activeSession: Boolean,
    maxAccounts: Boolean,
    nomadBlocksLogin: Boolean,
    links: ServerConfig.Links,
    navigateBack: () -> Unit,
    onAction: (WelcomeScreenAction) -> Unit,
) {
    val maxDialog = rememberVisibilityState<MaxAccountsReachedDialogState>()
    val nomadDialog = rememberVisibilityState<NomadAccountBlocksLoginDialogState>()
    val enterpriseDialog = rememberVisibilityState<FeatureDisabledWithProxyDialogState>()
    val personalDialog = rememberVisibilityState<FeatureDisabledWithProxyDialogState>()
    MaxAccountsReachedDialog(maxDialog, navigateBack)
    NomadAccountBlocksLoginDialog(nomadDialog, navigateBack)
    FeatureDisabledWithProxyDialogContent(enterpriseDialog) { onAction(WelcomeScreenAction.OpenUrl(links.teams)) }
    FeatureDisabledWithProxyDialogContent(personalDialog)
    if (maxAccounts) maxDialog.show(maxDialog.savedState ?: MaxAccountsReachedDialogState)
    if (nomadBlocksLogin) nomadDialog.show(nomadDialog.savedState ?: NomadAccountBlocksLoginDialogState)
    val teamRegistrationUrl = links.teams + stringResource(
        AuthenticationR.string.create_account_email_backlink_to_team_suffix_url,
    )
    WelcomeScreenContent(
        state = welcomePresentation(activeSession),
        loginLabel = stringResource(R.string.label_login),
        createTeamLabel = stringResource(AuthenticationR.string.welcome_button_create_team),
        footerText = stringResource(AuthenticationR.string.welcome_footer_text),
        createPersonalLabel = stringResource(AuthenticationR.string.welcome_button_create_personal_account),
        openLinkDescription = stringResource(CommonR.string.content_description_open_link_label),
        closeContentDescription = AuthenticationR.string.content_description_welcome_screen_close_btn,
        onClose = navigateBack,
        onLogin = { onAction(WelcomeScreenAction.Login(links)) },
        onCreateTeam = { handleTeamDecision(links, teamRegistrationUrl, enterpriseDialog, onAction) },
        onCreatePersonal = { handlePersonalDecision(links, personalDialog, onAction) },
        logoContent = { WelcomeLogo() },
        serverTitleContent = { WelcomeServerTitle(links) },
        bodyOverride = links.welcomeBodyOverride(),
    )
}

@Composable
private fun welcomePresentation(activeSession: Boolean) = WelcomePresentationState(
    showCloseButton = activeSession,
    accountCreationAllowed = LocalCustomUiConfigurationProvider.current.isAccountCreationAllowed,
    showTeamCreation = true,
    showPersonalCreation = true,
    carouselDelayMillis = integerResource(AuthenticationR.integer.welcome_carousel_item_time_ms).toLong(),
    carouselPages = welcomeCarouselPages(),
)

private fun handleTeamDecision(
    links: ServerConfig.Links,
    registrationUrl: String,
    dialog: VisibilityState<FeatureDisabledWithProxyDialogState>,
    onAction: (WelcomeScreenAction) -> Unit,
) {
    val policy = WelcomePolicy(
        links,
        links.isProxyEnabled(),
        ENABLE_NEW_REGISTRATION,
        links.teams,
        registrationUrl,
    )
    when (val result = welcomeTeamDecision(policy)) {
        is WelcomeDecision.Action -> onAction(result.value.toRouteAction())
        is WelcomeDecision.Dialog -> dialog.show(dialog.savedState ?: FeatureDisabledWithProxyDialogState(
            AuthenticationR.string.create_team_not_supported_dialog_description, (result.value as WelcomeDialog.TeamBlockedByProxy).url,
        ))
    }
}

private fun handlePersonalDecision(
    links: ServerConfig.Links,
    dialog: VisibilityState<FeatureDisabledWithProxyDialogState>,
    onAction: (WelcomeScreenAction) -> Unit,
) {
    val policy = WelcomePolicy(
        links,
        links.isProxyEnabled(),
        ENABLE_NEW_REGISTRATION,
        links.teams,
        links.teams,
    )
    when (val result = welcomePersonalDecision(policy)) {
        is WelcomeDecision.Action -> onAction(result.value.toRouteAction())
        is WelcomeDecision.Dialog -> dialog.show(dialog.savedState ?: FeatureDisabledWithProxyDialogState(
            AuthenticationR.string.create_personal_account_not_supported_dialog_description,
        ))
    }
}
