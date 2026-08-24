/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.registration.code

import androidx.activity.compose.BackHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.feature.authentication.R as AuthenticationR
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegistrationCodeContent
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegistrationCodeState
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegistrationCodeText
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.user.UserId

/** Navigation/resource adapter for the feature-owned legacy registration code surface. */
@Composable
internal fun CreateAccountVerificationCodeRouteScreen(
    viewModel: CreateAccountVerificationCodeViewModel,
    onNavigateBack: () -> Unit,
    onSuccess: (UserId) -> Unit,
    onTooManyDevices: (UserId) -> Unit,
) {
    val state = viewModel.state
    LegacyRegistrationCodeContent(
        state = state,
        textState = viewModel.codeTextState,
        text = LegacyRegistrationCodeText(
            title = stringResource(AuthenticationR.string.create_personal_account_title),
            instruction = stringResource(AuthenticationR.string.create_account_code_text, state.email),
            invalidCode = stringResource(AuthenticationR.string.create_account_code_error),
        ),
        onResendCodePressed = viewModel::resendCode,
        onBackPressed = onNavigateBack,
        serverTitle = {
            if (viewModel.serverConfig.isOnPremises) {
                ServerTitle(
                    serverLinks = viewModel.serverConfig,
                    style = MaterialTheme.wireTypography.body01,
                )
            }
        },
    )
    LegacyRegistrationCodeErrorDialog(
        result = state.result,
        onDismiss = viewModel::clearError,
    )
    HandleLegacyRegistrationCodeResult(
        result = state.result,
        onSuccess = onSuccess,
        onTooManyDevices = onTooManyDevices,
        onHandled = {
            viewModel.clearError()
            viewModel.clearCodeField()
        },
    )
    BackHandler(enabled = !state.loading, onBack = onNavigateBack)
}

@Composable
private fun HandleLegacyRegistrationCodeResult(
    result: LegacyRegistrationCodeState.Result<UserId, *>,
    onSuccess: (UserId) -> Unit,
    onTooManyDevices: (UserId) -> Unit,
    onHandled: () -> Unit,
) {
    LaunchedEffect(result) {
        when (result) {
            is LegacyRegistrationCodeState.Result.Success -> onSuccess(result.userId)
            is LegacyRegistrationCodeState.Result.TooManyDevices -> {
                onHandled()
                onTooManyDevices(result.userId)
            }

            else -> Unit
        }
    }
}
