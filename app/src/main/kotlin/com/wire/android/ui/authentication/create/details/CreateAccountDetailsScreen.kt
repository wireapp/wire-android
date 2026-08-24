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

package com.wire.android.ui.authentication.create.details

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.feature.authentication.R as AuthenticationR
import com.wire.android.navigation.routes.auth.CreateAccountCodeRoute
import com.wire.android.navigation.routes.auth.CreateAccountDetailsRoute
import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import com.wire.android.ui.authentication.create.common.createAccountFlowPolicy
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.configuration.server.ServerConfig

@Composable
internal fun CreateAccountDetailsRouteScreen(
    route: CreateAccountDetailsRoute,
    viewModel: CreateAccountDetailsViewModel<ServerConfig.Links, NetworkFailure>,
    onNavigateBack: () -> Unit,
    onCodeRequested: (CreateAccountCodeRoute) -> Unit,
) {
    with(viewModel) {
        LaunchedEffect(detailsState.success) {
            if (detailsState.success) {
                onCodeRequested(
                    CreateAccountCodeRoute(
                        type = route.type,
                        registrationInfo = route.registrationInfo.copy(
                            firstName = firstNameTextState.text.toString().trim(),
                            lastName = lastNameTextState.text.toString().trim(),
                            password = passwordTextState.text.toString(),
                            teamName = teamNameTextState.text.toString().trim(),
                        ),
                        customServerConfig = route.customServerConfig,
                        flowId = route.flowId,
                    )
                )
            }
        }

        CreateAccountDetailsContent(
            state = detailsState,
            title = stringResource(route.type.titleResId()),
            showTeamName = route.type.createAccountFlowPolicy().isTeam,
            sharedText = CreateAccountDetailsSharedText(
                passwordDescription = stringResource(R.string.create_account_details_password_description),
                confirmPasswordLabel = stringResource(AuthenticationR.string.create_account_details_confirm_password_label),
                invalidPasswordError = stringResource(AuthenticationR.string.create_account_details_password_error),
                passwordsNotMatchingError = stringResource(AuthenticationR.string.create_account_details_password_not_matching_error),
                continueLabel = stringResource(R.string.label_continue),
            ),
            firstNameTextState = firstNameTextState,
            lastNameTextState = lastNameTextState,
            passwordTextState = passwordTextState,
            confirmPasswordTextState = confirmPasswordTextState,
            teamNameTextState = teamNameTextState,
            onBackPressed = onNavigateBack,
            onContinuePressed = ::onDetailsContinue,
            onErrorDismiss = ::onDetailsErrorDismiss,
            subtitleContent = {
                if (serverConfig.isOnPremises) {
                    ServerTitle(
                        serverLinks = serverConfig,
                        style = MaterialTheme.wireTypography.body01,
                    )
                }
            },
            genericFailureContent = { failure, onDismiss -> CoreFailureErrorDialog(failure, onDismiss) },
        )
    }
}

private fun CreateAccountRouteFlowType.titleResId(): Int = when (this) {
    CreateAccountRouteFlowType.PERSONAL -> com.wire.android.feature.authentication.R.string.create_personal_account_title
    CreateAccountRouteFlowType.TEAM -> R.string.create_team_title
}
