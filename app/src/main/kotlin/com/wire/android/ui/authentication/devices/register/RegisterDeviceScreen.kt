/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication.devices.register

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.feature.SwitchAccountActions
import com.wire.android.ui.authentication.devices.common.ClearSessionState
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModel
import com.wire.android.ui.common.dialogs.CancelLoginDialogContent
import com.wire.android.ui.common.dialogs.CancelLoginDialogState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.navigation.WireSessionId

@Composable
internal fun RegisterDeviceRouteScreen(
    viewModel: RegisterDeviceViewModel,
    clearSessionViewModel: ClearSessionViewModel,
    switchAccountActions: SwitchAccountActions,
    onE2EIRequired: (WireSessionId?) -> Unit,
    onHomeRequired: () -> Unit,
    onInitialSyncRequired: () -> Unit,
    onRemoveDeviceRequired: () -> Unit,
) = RegisterDeviceScreen(
    viewModel = viewModel,
    text = RegisterDeviceText(
        title = stringResource(com.wire.android.feature.authentication.R.string.register_device_title),
        message = stringResource(com.wire.android.feature.authentication.R.string.register_device_text),
        continueLabel = stringResource(com.wire.android.feature.authentication.R.string.label_add_device),
        invalidPasswordMessage = stringResource(R.string.remove_device_invalid_password),
    ),
    cancelDialog = {
        CancelRegisterDeviceDialog(
            state = clearSessionViewModel.state,
            onCancel = { clearSessionViewModel.onCancelLoginClicked(switchAccountActions) },
            onProceed = clearSessionViewModel::onProceedLoginClicked,
        )
    },
    failureDialog = { failure, dismiss -> AuthenticationFailureDialog(failure, dismiss) },
    onBack = clearSessionViewModel::onBackButtonClicked,
    onSuccess = { success ->
        when {
            success.isE2EIRequired -> onE2EIRequired(success.e2eiSessionId)
            success.initialSyncCompleted -> onHomeRequired()
            else -> onInitialSyncRequired()
        }
    },
    onTooManyDevices = onRemoveDeviceRequired,
)

@Composable
private fun CancelRegisterDeviceDialog(
    state: ClearSessionState,
    onCancel: () -> Unit,
    onProceed: () -> Unit,
) {
    val dialogState = rememberVisibilityState<CancelLoginDialogState>()
    CancelLoginDialogContent(dialogState, onCancel, onProceed)
    if (state.showCancelLoginDialog) dialogState.show(dialogState.savedState ?: CancelLoginDialogState)
    else dialogState.dismiss()
}
