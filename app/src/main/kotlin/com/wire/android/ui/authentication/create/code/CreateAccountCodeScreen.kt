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

package com.wire.android.ui.authentication.create.code

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.feature.authentication.R as AuthenticationR
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.create.summary.CreateAccountSummaryNavArgs
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.DialogErrorStrings
import com.wire.android.util.dialogErrorStrings

@Composable
internal fun CreateAccountCodeRouteScreen(
    viewModel: AppCreateAccountCodeViewModel,
    onNavigateBack: () -> Unit,
    onSuccess: (CreateAccountSummaryNavArgs, com.wire.kalium.logic.data.user.UserId) -> Unit,
    onTooManyDevices: (com.wire.kalium.logic.data.user.UserId) -> Unit,
) {
    with(viewModel) {
        CreateAccountCodeContent(
            state = codeState,
            textState = codeTextState,
            onResendCodePressed = ::resendCode,
            onBackPressed = onNavigateBack,
            presentation = CreateAccountCodePresentation(
                title = stringResource(id = codeState.type.titleResId),
                codeInstruction = stringResource(AuthenticationR.string.create_account_code_text, codeState.email),
                invalidActivationCodeError = stringResource(id = AuthenticationR.string.create_account_code_error),
                backContentDescription = R.string.content_description_login_back_btn,
            ),
            subtitleContent = {
                if (serverConfig.isOnPremises) {
                    ServerTitle(
                        serverLinks = serverConfig,
                        style = MaterialTheme.wireTypography.body01,
                    )
                }
            },
        )

        (codeState.result as? CreateAccountCodeResult.Error.DialogError)?.let {
            val (title, message) = it.getResources(type = codeState.type)
            WireDialog(
                title = title,
                text = message,
                onDismiss = ::clearCodeError,
                optionButton1Properties = WireDialogButtonProperties(
                    onClick = ::clearCodeError,
                    text = stringResource(id = R.string.label_ok),
                    type = WireDialogButtonType.Primary,
                )
            )
        }
        LaunchedEffect(codeState.result) {
            (codeState.result as? CreateAccountCodeResult.Success)?.let {
                onSuccess(CreateAccountSummaryNavArgs(flowType), it.userId)
            }
            val tooManyDevicesError = codeState.result as? CreateAccountCodeResult.Error.TooManyDevicesError
            if (tooManyDevicesError != null) {
                clearCodeError()
                clearCodeField()
                onTooManyDevices(tooManyDevicesError.userId)
            }
        }
    }
}

@Composable
private fun CreateAccountCodeResult.Error.DialogError<com.wire.kalium.common.error.CoreFailure>.getResources(
    type: CreateAccountFlowType,
) = when (this) {
    CreateAccountCodeResult.Error.DialogError.AccountAlreadyExistsError -> DialogErrorStrings(
        stringResource(id = AuthenticationR.string.create_account_code_error_title),
        stringResource(id = AuthenticationR.string.create_account_email_already_in_use_error)
    )

    CreateAccountCodeResult.Error.DialogError.BlackListedError -> DialogErrorStrings(
        stringResource(id = AuthenticationR.string.create_account_code_error_title),
        stringResource(id = AuthenticationR.string.create_account_email_blacklisted_error)
    )

    CreateAccountCodeResult.Error.DialogError.EmailDomainBlockedError -> DialogErrorStrings(
        stringResource(id = AuthenticationR.string.create_account_code_error_title),
        stringResource(id = AuthenticationR.string.create_account_email_domain_blocked_error)
    )

    CreateAccountCodeResult.Error.DialogError.InvalidEmailError -> DialogErrorStrings(
        stringResource(id = AuthenticationR.string.create_account_code_error_title),
        stringResource(id = AuthenticationR.string.create_account_email_invalid_error)
    )

    CreateAccountCodeResult.Error.DialogError.TeamMembersLimitError -> DialogErrorStrings(
        stringResource(id = AuthenticationR.string.create_account_code_error_title),
        stringResource(id = AuthenticationR.string.create_account_code_error_team_members_limit_reached)
    )

    CreateAccountCodeResult.Error.DialogError.CreationRestrictedError -> DialogErrorStrings(
        stringResource(id = AuthenticationR.string.create_account_code_error_title),
        stringResource(
            id = when (type) {
                CreateAccountFlowType.CreatePersonalAccount -> AuthenticationR.string.create_account_code_error_personal_account_creation_restricted
                CreateAccountFlowType.CreateTeam -> AuthenticationR.string.create_account_code_error_team_creation_restricted
            }
        )
    )
    // TODO: sync with design about the error message
    CreateAccountCodeResult.Error.DialogError.UserAlreadyExistsError ->
        DialogErrorStrings("User Already LoggedIn", "UserAlreadyLoggedIn")

    is CreateAccountCodeResult.Error.DialogError.GenericError ->
        this.failure.dialogErrorStrings(LocalContext.current.resources)
}
