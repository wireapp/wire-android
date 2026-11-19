/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.home.settings.mfa.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.lightspark.composeqr.DotShape
import com.lightspark.composeqr.QrCodeView
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.settings.mfa.components.MFACodeInput
import com.wire.android.ui.home.settings.mfa.components.MFARecoveryCodesContent
import com.wire.android.ui.home.settings.mfa.components.RecoveryCodesBackHandler
import com.wire.android.ui.home.settings.mfa.components.rememberAuthenticatorLinkActions
import com.wire.android.ui.home.settings.mfa.components.rememberRecoveryCodeActions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.mfa.MFAEnrollment

@Composable
internal fun MFASetupScreen(
    step: MFASetupStep,
    state: MFASetupState,
    viewModel: MFASetupViewModel,
    onBack: () -> Unit,
    onNext: (MFASetupStep) -> Unit,
) {
    val authenticatorName = stringResource(R.string.mfa_default_app_name)
    val recoveryCodeActions = rememberRecoveryCodeActions(state.recoveryCodes)
    val linkActions = rememberAuthenticatorLinkActions(state.enrollment) {
        onNext(MFASetupStep.VERIFY_CODE)
    }
    MFASetupContent(
        step = step,
        state = state,
        password = viewModel.password,
        code = viewModel.code,
        onBack = onBack,
        onContinue = {
            when (step) {
                MFASetupStep.CONFIRM_PASSWORD -> viewModel.confirmPassword()
                MFASetupStep.LINK_AUTHENTICATOR,
                MFASetupStep.MANUAL_KEY -> onNext(MFASetupStep.VERIFY_CODE)
                MFASetupStep.VERIFY_CODE -> viewModel.verifyAuthenticationCode()
                MFASetupStep.RECOVERY_CODES -> viewModel.finishSetup(authenticatorName)
            }
        },
        onOpenAuthenticator = linkActions.open,
        onManualKey = { onNext(MFASetupStep.MANUAL_KEY) },
        onCopyKey = linkActions.copyKey,
        onCopyCodes = recoveryCodeActions.copy,
        onDownloadCodes = recoveryCodeActions.download,
        onCodesSaved = viewModel::setRecoveryCodesSaved,
    )
}

@Composable
internal fun MFASetupContent(
    step: MFASetupStep,
    state: MFASetupState,
    password: TextFieldState,
    code: TextFieldState,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onOpenAuthenticator: () -> Unit,
    onManualKey: () -> Unit,
    onCopyKey: () -> Unit,
    onCopyCodes: () -> Unit,
    onDownloadCodes: () -> Unit,
    onCodesSaved: (Boolean) -> Unit,
) {
    val canGoBack = state.canNavigateBackFrom(step)
    RecoveryCodesBackHandler(canGoBack, onBack)
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.mfa_setup_title),
                onNavigationPressed = onBack,
                navigationIconType = if (canGoBack) NavigationIconType.Back() else null,
                elevation = dimensions().spacing0x,
            )
        },
        bottomBar = {
            SetupContinueButton(step, state, password, onContinue)
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(dimensions().spacing16x),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing16x),
        ) {
            SetupHeading(step)
            when (step) {
                MFASetupStep.CONFIRM_PASSWORD -> {
                    WirePasswordTextField(
                        textState = password,
                        state = if (state.isBusy) WireTextFieldState.Disabled else WireTextFieldState.Default,
                    )
                }
                MFASetupStep.LINK_AUTHENTICATOR -> {
                    WireSecondaryButton(
                        text = stringResource(R.string.mfa_open_authenticator),
                        onClick = onOpenAuthenticator,
                    )
                    state.enrollment?.let { enrollment ->
                        val qrDescription = stringResource(R.string.mfa_qr_description)
                        QrCodeView(
                            data = enrollment.authenticatorUri,
                            dotShape = DotShape.Square,
                            modifier = Modifier.align(Alignment.CenterHorizontally).size(180.dp).background(Color.White)
                                .semantics { contentDescription = qrDescription },
                        )
                    }
                    WireSecondaryButton(text = stringResource(R.string.mfa_manual_key), onClick = onManualKey)
                }
                MFASetupStep.MANUAL_KEY -> {
                    state.enrollment?.let { enrollment ->
                        Text(enrollment.account, style = MaterialTheme.wireTypography.body01)
                        Text(enrollment.setupKey, style = MaterialTheme.wireTypography.body01, fontFamily = FontFamily.Monospace)
                        SetupDescription(stringResource(R.string.mfa_key_type))
                        WireSecondaryButton(text = stringResource(R.string.mfa_copy_key), onClick = onCopyKey)
                    }
                }
                MFASetupStep.VERIFY_CODE -> MFACodeInput(
                    code = code,
                    isBusy = state.isBusy,
                    isInvalid = state.error == MFASetupError.INVALID_CODE,
                    onCodeComplete = onContinue,
                )
                MFASetupStep.RECOVERY_CODES -> MFARecoveryCodesContent(
                    codes = state.recoveryCodes,
                    saved = state.recoveryCodesSaved,
                    onSavedChange = onCodesSaved,
                    onCopy = onCopyCodes,
                    onDownload = onDownloadCodes,
                )
            }
            state.error?.let { SetupError(it) }
        }
    }
}

@Composable
private fun SetupContinueButton(
    step: MFASetupStep,
    state: MFASetupState,
    password: TextFieldState,
    onContinue: () -> Unit,
) {
    if (step == MFASetupStep.VERIFY_CODE) return
    val enabled = !state.isBusy && when (step) {
        MFASetupStep.CONFIRM_PASSWORD -> password.text.isNotBlank()
        MFASetupStep.RECOVERY_CODES -> state.recoveryCodesSaved && state.recoveryCodes.isNotEmpty()
        else -> state.enrollment != null
    }
    WirePrimaryButton(
        text = stringResource(
            when (step) {
                MFASetupStep.RECOVERY_CODES -> R.string.mfa_finish_setup
                else -> R.string.label_continue
            }
        ),
        onClick = onContinue,
        loading = state.isBusy,
        state = if (enabled) WireButtonState.Default else WireButtonState.Disabled,
        modifier = Modifier.padding(dimensions().spacing16x),
    )
}

@Composable
private fun ColumnScope.SetupHeading(step: MFASetupStep) {
    if (step != MFASetupStep.CONFIRM_PASSWORD) {
        SetupProgressLabel(step)
    }
    val (heading, description) = when (step) {
        MFASetupStep.CONFIRM_PASSWORD ->
            R.string.mfa_confirm_password_title to R.string.mfa_confirm_password_description
        MFASetupStep.LINK_AUTHENTICATOR ->
            R.string.mfa_link_title to R.string.mfa_link_description
        MFASetupStep.MANUAL_KEY ->
            R.string.mfa_key_title to R.string.mfa_key_description
        MFASetupStep.VERIFY_CODE ->
            R.string.mfa_code_title to R.string.mfa_code_description
        MFASetupStep.RECOVERY_CODES ->
            R.string.mfa_codes_title to R.string.mfa_codes_description
    }
    Text(stringResource(heading), style = MaterialTheme.wireTypography.title01)
    SetupDescription(stringResource(description))
}

@Composable
private fun SetupProgressLabel(step: MFASetupStep) {
    val stepNumber = when (step) {
        MFASetupStep.VERIFY_CODE -> 2
        MFASetupStep.RECOVERY_CODES -> 3
        else -> 1
    }
    SetupDescription(stringResource(R.string.mfa_setup_step, stepNumber, 3))
}

@Composable
private fun SetupError(error: MFASetupError) {
    Text(
        text = stringResource(
            when (error) {
                MFASetupError.PASSWORD_REQUIRED -> R.string.mfa_password_required
                MFASetupError.INVALID_CODE -> R.string.mfa_invalid_code
                MFASetupError.GENERIC -> R.string.mfa_setup_error
            }
        ),
        color = MaterialTheme.wireColorScheme.error,
        style = MaterialTheme.wireTypography.body01,
    )
}

@Composable
private fun SetupDescription(text: String) {
    Text(text, style = MaterialTheme.wireTypography.body01, color = MaterialTheme.wireColorScheme.secondaryText)
}

@PreviewMultipleThemes
@Composable
private fun PreviewMFASetup() = WireTheme {
    MFASetupContent(
        step = MFASetupStep.LINK_AUTHENTICATOR,
        state = MFASetupState(
            enrollment = MFAEnrollment(
                id = "demo",
                account = "demo@wire.example",
                setupKey = "JBSWY3DPEHPK3PXP",
                authenticatorUri = "otpauth://totp/Wire:demo@wire.example?secret=JBSWY3DPEHPK3PXP&issuer=Wire" +
                    "&algorithm=SHA1&digits=6&period=30",
            ),
        ),
        password = remember { TextFieldState() },
        code = remember { TextFieldState() },
        onBack = {}, onContinue = {}, onOpenAuthenticator = {}, onManualKey = {}, onCopyKey = {},
        onCopyCodes = {}, onDownloadCodes = {}, onCodesSaved = {},
    )
}
