/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.newauthentication.login.code

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.authentication.R
import com.wire.android.ui.authentication.verificationcode.VerificationCode
import com.wire.android.ui.authentication.verificationcode.VerificationCodeState
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.newauthentication.login.NewAuthContainer
import com.wire.android.ui.newauthentication.login.NewAuthHeader
import com.wire.android.ui.newauthentication.login.NewAuthSubtitle
import com.wire.android.ui.newauthentication.login.NewAuthTitle
import com.wire.android.ui.theme.wireDimensions

/** Feature-owned verification-code UI; the host owns state clearing and navigation. */
@Composable
fun NewLoginVerificationCodeContent(
    codeTextState: TextFieldState,
    codeState: VerificationCodeState,
    isLoading: Boolean,
    onResendCode: () -> Unit,
    onLoginButtonClick: () -> Unit,
    canNavigateBack: Boolean,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NewAuthContainer(
        modifier = modifier,
        header = {
            NewAuthHeader(
                title = {
                    NewAuthTitle(
                        title = stringResource(R.string.enterprise_login_verification_code_title),
                        modifier = Modifier.padding(bottom = dimensions().spacing24x),
                    )
                    NewAuthSubtitle(
                        title = stringResource(R.string.second_factor_authentication_instructions_label, codeState.emailUsed),
                    )
                },
                canNavigateBack = canNavigateBack,
                onNavigateBack = navigateBack,
            )
        },
    ) {
        VerificationCode(
            codeLength = codeState.codeLength,
            codeState = codeTextState,
            isLoading = isLoading,
            showLoadingProgress = false,
            isCurrentCodeInvalid = codeState.isCurrentCodeInvalid,
            onResendCode = onResendCode,
            modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.spacing24x),
        )
        VerificationNextButton(
            loading = isLoading,
            enabled = codeTextState.text.length == codeState.codeLength,
            onClick = onLoginButtonClick,
        )
    }
}

@Composable
private fun VerificationNextButton(
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensions().spacing8x),
    ) {
        WirePrimaryButton(
            text = stringResource(R.string.enterprise_login_next),
            onClick = onClick,
            state = if (enabled && !loading) WireButtonState.Default else WireButtonState.Disabled,
            loading = loading,
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth().testTag("loginButton"),
        )
    }
}
