/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication.devices.remove

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.feature.SwitchAccountActions
import com.wire.android.ui.authentication.devices.DeviceItem
import com.wire.android.ui.authentication.devices.common.ClearSessionState
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModel
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.register.AuthenticationFailureDialog
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.dialogs.CancelLoginDialogContent
import com.wire.android.ui.common.dialogs.CancelLoginDialogState
import com.wire.android.ui.common.textfield.clearAutofillTree
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.kalium.logic.data.conversation.ClientId

/** Host-only Device rendering and shared dialog adapter for the generic feature screen. */
@Composable
internal fun RemoveDeviceRouteScreen(
    viewModel: RemoveDeviceViewModel<Device>,
    clearSessionViewModel: ClearSessionViewModel,
    switchAccountActions: SwitchAccountActions,
    onE2EIRequired: () -> Unit,
    onHomeRequired: () -> Unit,
    onInitialSyncRequired: () -> Unit,
) {
    clearAutofillTree()
    RemoveDeviceContent(
        viewModel = viewModel,
        placeholderDevice = { Device(clientId = ClientId("placeholder_$it")) },
        deviceItem = { device, placeholder, onClick ->
            DeviceItem(
                device = device,
                placeholder = placeholder,
                onClickAction = onClick,
                shouldShowVerifyLabel = false,
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_remove),
                        stringResource(R.string.content_description_remove_devices_screen_remove_icon),
                    )
                },
            )
        },
        removeDialog = { dialog, password, invalidPassword, dismiss, confirm ->
            RemoveDeviceDialog(
                errorState = if (invalidPassword) RemoveDeviceError.InvalidCredentialsError else RemoveDeviceError.None,
                state = RemoveDeviceDialogState.Visible(dialog.device, dialog.loading, dialog.removeEnabled),
                passwordTextState = password,
                onDialogDismiss = dismiss,
                onRemoveConfirm = confirm,
            )
        },
        initialLoadErrorDialog = { cancel, retry -> InitialLoadErrorDialog(cancel, retry) },
        genericFailureDialog = { failure, dismiss -> AuthenticationFailureDialog(failure, dismiss) },
        cancelDialog = {
            CancelRemoveDeviceDialog(
                state = clearSessionViewModel.state,
                onCancel = { clearSessionViewModel.onCancelLoginClicked(switchAccountActions) },
                onProceed = clearSessionViewModel::onProceedLoginClicked,
            )
        },
        onBack = clearSessionViewModel::onBackButtonClicked,
        onComplete = { action ->
            when {
                action.isE2EIRequired -> onE2EIRequired()
                action.initialSyncCompleted -> onHomeRequired()
                else -> onInitialSyncRequired()
            }
        },
    )
}

@Composable
private fun InitialLoadErrorDialog(onCancel: () -> Unit, onRetry: () -> Unit) {
    WireDialog(
        properties = wireDialogPropertiesBuilder(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = stringResource(R.string.label_general_error),
        text = stringResource(com.wire.android.feature.authentication.R.string.devices_loading_error),
        onDismiss = onRetry,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onCancel,
            text = stringResource(R.string.label_cancel),
            type = WireDialogButtonType.Secondary,
        ),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onRetry,
            text = stringResource(R.string.label_retry),
            type = WireDialogButtonType.Primary,
        ),
    )
}

@Composable
private fun CancelRemoveDeviceDialog(state: ClearSessionState, onCancel: () -> Unit, onProceed: () -> Unit) {
    val dialogState = rememberVisibilityState<CancelLoginDialogState>()
    CancelLoginDialogContent(dialogState, onCancel, onProceed)
    if (state.showCancelLoginDialog) {
        dialogState.show(dialogState.savedState ?: CancelLoginDialogState)
    } else {
        dialogState.dismiss()
    }
}
