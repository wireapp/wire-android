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

package com.wire.android.ui.home.settings.mfa.recoverycodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.kalium.logic.data.mfa.RecoveryCodesStatus
import com.wire.android.ui.home.settings.mfa.components.MFARecoveryCodesContent
import com.wire.android.ui.home.settings.mfa.components.RecoveryCodesBackHandler
import com.wire.android.ui.home.settings.mfa.components.rememberRecoveryCodeActions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
internal fun MFARecoveryCodesScreen(
    route: MFARecoveryCodesRoute,
    state: MFARecoveryCodesState,
    viewModel: MFARecoveryCodesViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val actions = rememberRecoveryCodeActions(state.codes)
    MFARecoveryCodesScreenContent(
        step = route.step,
        status = RecoveryCodesStatus(route.remaining, route.total),
        state = state,
        password = viewModel.password,
        onBack = onBack,
        onContinue = {
            when (route.step) {
                MFARecoveryCodesStep.CONFIRM_PASSWORD -> viewModel.confirmPassword()
                MFARecoveryCodesStep.SUMMARY -> viewModel.requestGeneration()
                MFARecoveryCodesStep.NEW_CODES -> onDone()
            }
        },
        onCopy = actions.copy,
        onDownload = actions.download,
        onSavedChange = viewModel::setCodesSaved,
    )
    if (state.showGenerationConfirmation) {
        GenerateRecoveryCodesDialog(state, viewModel::generateNewCodes, viewModel::dismissGenerationConfirmation)
    }
}

@Composable
internal fun MFARecoveryCodesScreenContent(
    step: MFARecoveryCodesStep,
    status: RecoveryCodesStatus,
    state: MFARecoveryCodesState,
    password: TextFieldState,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onCopy: () -> Unit,
    onDownload: () -> Unit,
    onSavedChange: (Boolean) -> Unit,
) {
    RecoveryCodesBackHandler(state.canNavigateBack, onBack)
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.mfa_recovery_codes),
                onNavigationPressed = { if (state.canNavigateBack) onBack() },
                navigationIconType = if (state.canNavigateBack) NavigationIconType.Back() else null,
                elevation = dimensions().spacing0x,
            )
        },
        bottomBar = { RecoveryCodesContinueButton(step, state, password, onContinue) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(dimensions().spacing16x),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing16x),
        ) {
            when (step) {
                MFARecoveryCodesStep.CONFIRM_PASSWORD -> {
                    Text(
                        stringResource(R.string.mfa_confirm_password_title),
                        style = MaterialTheme.wireTypography.title01,
                    )
                    Text(
                        stringResource(R.string.mfa_recovery_password_description),
                        style = MaterialTheme.wireTypography.body01,
                    )
                    WirePasswordTextField(
                        textState = password,
                        state = if (state.isBusy) WireTextFieldState.Disabled else WireTextFieldState.Default,
                    )
                }
                MFARecoveryCodesStep.SUMMARY -> {
                    Text(
                        stringResource(R.string.mfa_recovery_codes_remaining, status.remaining, status.total),
                        style = MaterialTheme.wireTypography.title01,
                    )
                    Text(
                        stringResource(R.string.mfa_recovery_codes_shown_once),
                        style = MaterialTheme.wireTypography.body01,
                    )
                }
                MFARecoveryCodesStep.NEW_CODES -> {
                    Text(
                        stringResource(R.string.mfa_codes_title),
                        style = MaterialTheme.wireTypography.title01,
                    )
                    Text(
                        stringResource(R.string.mfa_new_codes_generated),
                        style = MaterialTheme.wireTypography.body01,
                    )
                    MFARecoveryCodesContent(state.codes, state.codesSaved, onSavedChange, onCopy, onDownload)
                }
            }
            if (state.hasError) {
                Text(
                    stringResource(R.string.mfa_setup_error),
                    color = MaterialTheme.wireColorScheme.error,
                    style = MaterialTheme.wireTypography.body01,
                )
            }
        }
    }
}

@Composable
private fun RecoveryCodesContinueButton(
    step: MFARecoveryCodesStep,
    state: MFARecoveryCodesState,
    password: TextFieldState,
    onContinue: () -> Unit,
) {
    val enabled = !state.isBusy && when (step) {
        MFARecoveryCodesStep.CONFIRM_PASSWORD -> password.text.isNotBlank()
        MFARecoveryCodesStep.SUMMARY -> state.passwordConfirmed
        MFARecoveryCodesStep.NEW_CODES -> state.codesSaved && state.codes.isNotEmpty()
    }
    val label = when (step) {
        MFARecoveryCodesStep.CONFIRM_PASSWORD -> R.string.label_continue
        MFARecoveryCodesStep.SUMMARY -> R.string.mfa_generate_codes
        MFARecoveryCodesStep.NEW_CODES -> R.string.label_done
    }
    WirePrimaryButton(
        text = stringResource(label),
        onClick = onContinue,
        state = if (enabled) WireButtonState.Default else WireButtonState.Disabled,
        loading = state.isBusy,
        modifier = Modifier.padding(dimensions().spacing16x),
    )
}

@Composable
private fun GenerateRecoveryCodesDialog(
    state: MFARecoveryCodesState,
    onGenerate: () -> Unit,
    onCancel: () -> Unit,
) {
    val buttonState = if (state.isGenerating) WireButtonState.Disabled else WireButtonState.Default
    WireDialog(
        title = stringResource(R.string.mfa_generate_codes_confirmation_title),
        text = stringResource(R.string.mfa_generate_codes_confirmation_description),
        onDismiss = onCancel,
        optionButton1Properties = WireDialogButtonProperties(
            text = stringResource(R.string.mfa_generate),
            onClick = onGenerate,
            state = buttonState,
            type = WireDialogButtonType.Primary,
            loading = state.isGenerating,
        ),
        dismissButtonProperties = WireDialogButtonProperties(
            text = stringResource(R.string.label_cancel),
            onClick = onCancel,
            state = buttonState,
        ),
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewMFARecoveryCodes() = WireTheme {
    MFARecoveryCodesScreenContent(
        step = MFARecoveryCodesStep.NEW_CODES,
        status = RecoveryCodesStatus(9, 10),
        state = MFARecoveryCodesState(codes = listOf("k7qm2-xpd9f-hwtr4", "b3np8-vz6x2-kem5q")),
        password = remember { TextFieldState() },
        onBack = {}, onContinue = {}, onCopy = {}, onDownload = {}, onSavedChange = {},
    )
}
