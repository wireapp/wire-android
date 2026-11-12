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

package com.wire.android.ui.home.settings.multifactorauthentication

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.lightspark.composeqr.DotShape
import com.lightspark.composeqr.QrCodeView
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlinx.coroutines.launch

@Composable
internal fun MultiFactorAuthenticationSetupScreen(
    step: MultiFactorAuthenticationSetupStep,
    state: MultiFactorAuthenticationSetupState,
    viewModel: MultiFactorAuthenticationSetupViewModel,
    onBack: () -> Unit,
    onNext: (MultiFactorAuthenticationSetupStep) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbar = LocalSnackbarHostState.current
    val copied = stringResource(R.string.multi_factor_authentication_copied)
    val noAuthenticator = stringResource(R.string.multi_factor_authentication_no_authenticator)
    val authenticatorName = stringResource(R.string.multi_factor_authentication_default_app_name)
    val recoveryCodeActions = rememberRecoveryCodeActions(state.recoveryCodes)
    val copy: (String) -> Unit = { text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("", text))
        coroutineScope.launch { snackbar.showSnackbar(copied) }
    }
    MultiFactorAuthenticationSetupContent(
        step = step,
        state = state,
        password = viewModel.password,
        code = viewModel.code,
        onBack = onBack,
        onContinue = {
            when (step) {
                MultiFactorAuthenticationSetupStep.CONFIRM_PASSWORD -> viewModel.confirmPassword()
                MultiFactorAuthenticationSetupStep.LINK_AUTHENTICATOR,
                MultiFactorAuthenticationSetupStep.MANUAL_KEY -> onNext(MultiFactorAuthenticationSetupStep.VERIFY_CODE)
                MultiFactorAuthenticationSetupStep.VERIFY_CODE -> viewModel.verifyAuthenticationCode()
                MultiFactorAuthenticationSetupStep.RECOVERY_CODES -> viewModel.finishSetup(authenticatorName)
            }
        },
        onOpenAuthenticator = {
            state.enrollment?.let { enrollment ->
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, enrollment.authenticatorUri.toUri()))
                    onNext(MultiFactorAuthenticationSetupStep.VERIFY_CODE)
                } catch (_: ActivityNotFoundException) {
                    coroutineScope.launch { snackbar.showSnackbar(noAuthenticator) }
                }
            }
        },
        onManualKey = { onNext(MultiFactorAuthenticationSetupStep.MANUAL_KEY) },
        onCopyKey = { state.enrollment?.setupKey?.let(copy) },
        onCopyCodes = recoveryCodeActions.copy,
        onDownloadCodes = recoveryCodeActions.download,
        onCodesSaved = viewModel::setRecoveryCodesSaved,
    )
}

@Composable
internal fun MultiFactorAuthenticationSetupContent(
    step: MultiFactorAuthenticationSetupStep,
    state: MultiFactorAuthenticationSetupState,
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
                title = stringResource(R.string.multi_factor_authentication_setup_title),
                onNavigationPressed = onBack,
                navigationIconType = if (canGoBack) NavigationIconType.Back() else null,
                elevation = dimensions().spacing0x,
            )
        },
        bottomBar = {
            SetupContinueButton(step, state, password, code, onContinue)
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(dimensions().spacing16x),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing16x),
        ) {
            SetupHeading(step)
            when (step) {
                MultiFactorAuthenticationSetupStep.CONFIRM_PASSWORD -> {
                    WirePasswordTextField(
                        textState = password,
                        state = if (state.isBusy) WireTextFieldState.Disabled else WireTextFieldState.Default,
                    )
                    SetupDescription(stringResource(R.string.multi_factor_authentication_demo_password))
                }
                MultiFactorAuthenticationSetupStep.LINK_AUTHENTICATOR -> {
                    WireSecondaryButton(
                        text = stringResource(R.string.multi_factor_authentication_open_authenticator),
                        onClick = onOpenAuthenticator,
                    )
                    state.enrollment?.let { enrollment ->
                        val qrDescription = stringResource(R.string.multi_factor_authentication_qr_description)
                        QrCodeView(
                            data = enrollment.authenticatorUri,
                            dotShape = DotShape.Square,
                            modifier = Modifier.align(Alignment.CenterHorizontally).size(180.dp).background(Color.White)
                                .semantics { contentDescription = qrDescription },
                        )
                    }
                    WireSecondaryButton(text = stringResource(R.string.multi_factor_authentication_manual_key), onClick = onManualKey)
                }
                MultiFactorAuthenticationSetupStep.MANUAL_KEY -> {
                    state.enrollment?.let { enrollment ->
                        Text(enrollment.account, style = MaterialTheme.wireTypography.body01)
                        Text(enrollment.setupKey, style = MaterialTheme.wireTypography.body01, fontFamily = FontFamily.Monospace)
                        SetupDescription(stringResource(R.string.multi_factor_authentication_key_type))
                        WireSecondaryButton(text = stringResource(R.string.multi_factor_authentication_copy_key), onClick = onCopyKey)
                    }
                }
                MultiFactorAuthenticationSetupStep.VERIFY_CODE -> VerificationCodeInput(code, state.isBusy)
                MultiFactorAuthenticationSetupStep.RECOVERY_CODES -> MultiFactorAuthenticationRecoveryCodesContent(
                    codes = state.recoveryCodes,
                    saved = state.recoveryCodesSaved,
                    onSavedChange = onCodesSaved,
                    onCopy = onCopyCodes,
                    onDownload = onDownloadCodes,
                )
            }
            state.error?.let { error ->
                Text(
                    text = stringResource(
                        when (error) {
                            MultiFactorAuthenticationSetupError.PASSWORD_REQUIRED -> R.string.multi_factor_authentication_password_required
                            MultiFactorAuthenticationSetupError.INVALID_CODE -> R.string.multi_factor_authentication_invalid_code
                            MultiFactorAuthenticationSetupError.GENERIC -> R.string.multi_factor_authentication_setup_error
                        }
                    ),
                    color = MaterialTheme.wireColorScheme.error,
                    style = MaterialTheme.wireTypography.body01,
                )
            }
        }
    }
}

@Composable
private fun SetupContinueButton(
    step: MultiFactorAuthenticationSetupStep,
    state: MultiFactorAuthenticationSetupState,
    password: TextFieldState,
    code: TextFieldState,
    onContinue: () -> Unit,
) {
    val enabled = !state.isBusy && when (step) {
        MultiFactorAuthenticationSetupStep.CONFIRM_PASSWORD -> password.text.isNotBlank()
        MultiFactorAuthenticationSetupStep.VERIFY_CODE -> code.text.length == AUTHENTICATION_CODE_LENGTH
        MultiFactorAuthenticationSetupStep.RECOVERY_CODES -> state.recoveryCodesSaved && state.recoveryCodes.isNotEmpty()
        else -> state.enrollment != null
    }
    WirePrimaryButton(
        text = stringResource(
            when (step) {
                MultiFactorAuthenticationSetupStep.VERIFY_CODE -> R.string.multi_factor_authentication_verify
                MultiFactorAuthenticationSetupStep.RECOVERY_CODES -> R.string.multi_factor_authentication_finish_setup
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
private fun SetupHeading(step: MultiFactorAuthenticationSetupStep) {
    if (step != MultiFactorAuthenticationSetupStep.CONFIRM_PASSWORD) {
        val stepNumber = when (step) {
            MultiFactorAuthenticationSetupStep.VERIFY_CODE -> 2
            MultiFactorAuthenticationSetupStep.RECOVERY_CODES -> 3
            else -> 1
        }
        SetupDescription(stringResource(R.string.multi_factor_authentication_setup_step, stepNumber, 3))
    }
    val (heading, description) = when (step) {
        MultiFactorAuthenticationSetupStep.CONFIRM_PASSWORD ->
            R.string.multi_factor_authentication_confirm_password_title to R.string.multi_factor_authentication_confirm_password_description
        MultiFactorAuthenticationSetupStep.LINK_AUTHENTICATOR ->
            R.string.multi_factor_authentication_link_title to R.string.multi_factor_authentication_link_description
        MultiFactorAuthenticationSetupStep.MANUAL_KEY ->
            R.string.multi_factor_authentication_key_title to R.string.multi_factor_authentication_key_description
        MultiFactorAuthenticationSetupStep.VERIFY_CODE ->
            R.string.multi_factor_authentication_code_title to R.string.multi_factor_authentication_code_description
        MultiFactorAuthenticationSetupStep.RECOVERY_CODES ->
            R.string.multi_factor_authentication_codes_title to R.string.multi_factor_authentication_codes_description
    }
    Text(stringResource(heading), style = MaterialTheme.wireTypography.title01)
    SetupDescription(stringResource(description))
}

@Composable
private fun VerificationCodeInput(code: TextFieldState, isBusy: Boolean) {
    WireTextField(
        textState = code,
        labelText = stringResource(R.string.multi_factor_authentication_code_label),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        inputTransformation = InputTransformation {
            if (length > AUTHENTICATION_CODE_LENGTH || asCharSequence().any { it !in '0'..'9' }) revertAllChanges()
        },
        state = if (isBusy) WireTextFieldState.Disabled else WireTextFieldState.Default,
    )
    SetupDescription(stringResource(R.string.multi_factor_authentication_demo_code))
}

@Composable
private fun SetupDescription(text: String) {
    Text(text, style = MaterialTheme.wireTypography.body01, color = MaterialTheme.wireColorScheme.secondaryText)
}

@PreviewMultipleThemes
@Composable
private fun PreviewMultiFactorAuthenticationSetup() = WireTheme {
    MultiFactorAuthenticationSetupContent(
        step = MultiFactorAuthenticationSetupStep.LINK_AUTHENTICATOR,
        state = MultiFactorAuthenticationSetupState(
            enrollment = MultiFactorAuthenticationEnrollment("demo", "demo@wire.example", "JBSWY3DPEHPK3PXP"),
        ),
        password = remember { TextFieldState() },
        code = remember { TextFieldState() },
        onBack = {}, onContinue = {}, onOpenAuthenticator = {}, onManualKey = {}, onCopyKey = {},
        onCopyCodes = {}, onDownloadCodes = {}, onCodesSaved = {},
    )
}
