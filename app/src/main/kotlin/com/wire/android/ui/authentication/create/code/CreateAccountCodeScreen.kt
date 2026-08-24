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
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.feature.authentication.R as AuthenticationR
import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
internal fun CreateAccountCodeRouteScreen(
    viewModel: AppCreateAccountCodeViewModel,
    onNavigateBack: () -> Unit,
    onSuccess: (CreateAccountRouteFlowType, com.wire.kalium.logic.data.user.UserId) -> Unit,
    onTooManyDevices: (com.wire.kalium.logic.data.user.UserId) -> Unit,
) {
    with(viewModel) {
        CreateAccountCodeContent(
            state = codeState,
            textState = codeTextState,
            onResendCodePressed = ::resendCode,
            onBackPressed = onNavigateBack,
            presentation = CreateAccountCodePresentation(
                title = stringResource(id = codeState.type.titleResId()),
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
            val (title, message) = it.dialogResources(type = codeState.type)
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
                onSuccess(flowType, it.userId)
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

private fun CreateAccountRouteFlowType.titleResId(): Int = when (this) {
    CreateAccountRouteFlowType.PERSONAL -> com.wire.android.feature.authentication.R.string.create_personal_account_title
    CreateAccountRouteFlowType.TEAM -> R.string.create_team_title
}
