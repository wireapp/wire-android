/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.registration.selector

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.MaterialTheme
import com.wire.android.feature.authentication.R as AuthenticationR
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.create.common.UserRegistrationInfo
import com.wire.android.ui.authentication.legacyregistration.selector.LegacyRegistrationSelectorContent
import com.wire.android.ui.authentication.legacyregistration.selector.LegacyRegistrationSelectorText
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.common.R as CommonR

/** App route adapter: it resolves app resources/Kalium server metadata and delegates UI to auth. */
@Composable
internal fun CreateAccountSelectorRouteScreen(
    viewModel: CreateAccountSelectorViewModel,
    onPersonalAccountCreation: (CreateAccountDataNavArgs) -> Unit,
    onTeamAccountCreation: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val teamUrl = viewModel.teamAccountCreationUrl + stringResource(
        AuthenticationR.string.create_account_email_backlink_to_team_suffix_url,
    )
    LegacyRegistrationSelectorContent(
        text = LegacyRegistrationSelectorText(
            title = stringResource(AuthenticationR.string.create_account_selector_title),
            team = LegacyRegistrationSelectorText.Card(
                stringResource(AuthenticationR.string.create_account_selector_team_title),
                stringResource(AuthenticationR.string.create_account_selector_team_subtitle),
                highlights = listOf(
                    stringResource(AuthenticationR.string.create_account_selector_team_highlight_one),
                    stringResource(AuthenticationR.string.create_account_selector_team_highlight_two),
                ),
                continueLabel = stringResource(AuthenticationR.string.create_team_title),
            ),
            personal = LegacyRegistrationSelectorText.Card(
                stringResource(AuthenticationR.string.create_account_selector_personal_title),
                stringResource(AuthenticationR.string.create_account_selector_personal_subtitle),
                highlights = listOf(
                    stringResource(AuthenticationR.string.create_account_selector_personal_highlight_one),
                    stringResource(AuthenticationR.string.create_account_selector_personal_highlight_two),
                ),
                continueLabel = stringResource(AuthenticationR.string.create_personal_account_title),
            ),
        ),
        checkIcon = painterResource(CommonR.drawable.ic_check_circle),
        positiveColor = colorsScheme().positive,
        serverTitle = {
            if (viewModel.serverConfig.isOnPremises) {
                ServerTitle(
                    serverLinks = viewModel.serverConfig,
                    style = MaterialTheme.wireTypography.body01,
                )
            }
        },
        onNavigateBack = onNavigateBack,
        onPersonalAccountCreationClicked = {
            onPersonalAccountCreation(
                CreateAccountDataNavArgs(
                    customServerConfig = viewModel.serverConfig,
                    userRegistrationInfo = UserRegistrationInfo(viewModel.email),
                ),
            )
        },
        onTeamAccountCreationClicked = { onTeamAccountCreation(teamUrl) },
    )
    LaunchedEffect(Unit) { viewModel.onPageLoaded() }
}
