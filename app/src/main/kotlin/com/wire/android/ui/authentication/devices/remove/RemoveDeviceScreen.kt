package com.wire.android.ui.authentication.devices.remove

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.authentication.devices.DeviceItem
import com.wire.android.ui.authentication.devices.common.ClearSessionState
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModel
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.dialogs.CancelLoginDialogContent
import com.wire.android.ui.common.dialogs.CancelLoginDialogState
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.textfield.clearAutofillTree
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.dialogErrorStrings
import com.wire.android.util.formatMediumDateTime

@Composable
fun RemoveDeviceScreen() {
    val viewModel: RemoveDeviceViewModel = hiltViewModel()
    val clearSessionViewModel: ClearSessionViewModel = hiltViewModel()
    val state: RemoveDeviceState = viewModel.state
    val clearSessionState: ClearSessionState = clearSessionViewModel.state

    clearAutofillTree()
    RemoveDeviceContent(
        state = state,
        clearSessionState = clearSessionState,
        onItemClicked = viewModel::onItemClicked,
        onPasswordChange = viewModel::onPasswordChange,
        onRemoveConfirm = viewModel::onRemoveConfirmed,
        onDialogDismiss = viewModel::onDialogDismissed,
        onErrorDialogDismiss = viewModel::clearDeleteClientError,
        onBackButtonClicked = clearSessionViewModel::onBackButtonClicked,
        onCancelLoginClicked = clearSessionViewModel::onCancelLoginClicked,
        onProceedLoginClicked = clearSessionViewModel::onProceedLoginClicked
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoveDeviceContent(
    state: RemoveDeviceState,
    clearSessionState: ClearSessionState,
    onItemClicked: (Device) -> Unit,
    onPasswordChange: (TextFieldValue) -> Unit,
    onRemoveConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
    onErrorDialogDismiss: () -> Unit,
    onBackButtonClicked: () -> Unit,
    onCancelLoginClicked: () -> Unit,
    onProceedLoginClicked: () -> Unit
) {
    BackHandler {
        onBackButtonClicked()
    }
    val cancelLoginDialogState = rememberVisibilityState<CancelLoginDialogState>()
    CancelLoginDialogContent(
        dialogState = cancelLoginDialogState,
        onActionButtonClicked = {
            onCancelLoginClicked()
        },
        onProceedButtonClicked = {
            onProceedLoginClicked()
        }
    )
    if (clearSessionState.showCancelLoginDialog) {
        cancelLoginDialogState.show(
            cancelLoginDialogState.savedState ?: CancelLoginDialogState
        )
    } else {
        cancelLoginDialogState.dismiss()
    }

    val lazyListState = rememberLazyListState()
    Scaffold(topBar = {
        RemoveDeviceTopBar(
            elevation = lazyListState.rememberTopBarElevationState().value,
            onBackButtonClicked = onBackButtonClicked
        )
    }) { internalPadding ->
        Box(modifier = Modifier.padding(internalPadding)) {
            when (state.isLoadingClientsList) {
                true -> RemoveDeviceItemsList(lazyListState, List(10) { Device() }, true, onItemClicked)
                false -> RemoveDeviceItemsList(lazyListState, state.deviceList, false, onItemClicked)

            }
        }
        // TODO handle list loading errors
        if (!state.isLoadingClientsList && state.removeDeviceDialogState is RemoveDeviceDialogState.Visible) {
            RemoveDeviceDialog(
                errorState = state.error,
                state = state.removeDeviceDialogState,
                onPasswordChange = onPasswordChange,
                onDialogDismiss = onDialogDismiss,
                onRemoveConfirm = onRemoveConfirm,
            )
            if (state.error is RemoveDeviceError.GenericError) {
                val (title, message) = state.error.coreFailure.dialogErrorStrings(LocalContext.current.resources)

                WireDialog(
                    title = title,
                    text = message,
                    onDismiss = onErrorDialogDismiss,
                    optionButton1Properties = WireDialogButtonProperties(
                        onClick = onErrorDialogDismiss,
                        text = stringResource(id = R.string.label_ok),
                        type = WireDialogButtonType.Primary,
                    )
                )
            }
        }
    }
}

@Composable
private fun RemoveDeviceItemsList(
    lazyListState: LazyListState,
    items: List<Device>,
    placeholders: Boolean,
    onItemClicked: (Device) -> Unit,
) {
    SurfaceBackgroundWrapper {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(items) { index, device ->
                DeviceItem(
                    device = device,
                    placeholder = placeholders,
                    onRemoveDeviceClick = onItemClicked
                )
                if (index < items.lastIndex) Divider()
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RemoveDeviceDialog(
    errorState: RemoveDeviceError,
    state: RemoveDeviceDialogState.Visible,
    onPasswordChange: (TextFieldValue) -> Unit,
    onDialogDismiss: () -> Unit,
    onRemoveConfirm: () -> Unit,
) {
    var keyboardController: SoftwareKeyboardController? = null
    val onDialogDismissHideKeyboard: () -> Unit = {
        keyboardController?.hide()
        onDialogDismiss()
    }
    WireDialog(
        title = stringResource(R.string.remove_device_dialog_title),
        text = state.device.name + "\n" +
                stringResource(
                    R.string.remove_device_id_and_time_label,
                    state.device.clientId.value,
                    state.device.registrationTime.formatMediumDateTime() ?: ""
                ),
        onDismiss = onDialogDismissHideKeyboard,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onDialogDismissHideKeyboard,
            text = stringResource(id = R.string.label_cancel),
            state = WireButtonState.Default
        ),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = {
                keyboardController?.hide()
                onRemoveConfirm()
            },
            text = stringResource(id = if (state.loading) R.string.label_removing else R.string.label_remove),
            type = WireDialogButtonType.Primary,
            loading = state.loading,
            state = if (state.removeEnabled) WireButtonState.Error else WireButtonState.Disabled
        ),
        content = {
            // keyboard controller from outside the Dialog doesn't work inside its content so we have to pass the state
            // to the dialog's content and use keyboard controller from there
            keyboardController = LocalSoftwareKeyboardController.current
            val focusRequester = remember { FocusRequester() }
            WirePasswordTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                state = when {
                    errorState is RemoveDeviceError.InvalidCredentialsError ->
                        WireTextFieldState.Error(stringResource(id = R.string.remove_device_invalid_password))

                    state.loading -> WireTextFieldState.Disabled
                    else -> WireTextFieldState.Default
                },
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .padding(bottom = MaterialTheme.wireDimensions.spacing8x)
                    .testTag("remove device password field")
            )
            LaunchedEffect(Unit) { // executed only once when showing the dialog
                focusRequester.requestFocus()
            }
        }
    )
}

@Preview
@Composable
fun PreviewRemoveDeviceScreen() {
    RemoveDeviceContent(
        state = RemoveDeviceState(
            List(10) { Device() },
            RemoveDeviceDialogState.Hidden,
            isLoadingClientsList = false
        ),
        clearSessionState = ClearSessionState(),
        onItemClicked = {},
        onPasswordChange = {},
        onRemoveConfirm = {},
        onDialogDismiss = {},
        onErrorDialogDismiss = {},
        onBackButtonClicked = {},
        onCancelLoginClicked = {},
        onProceedLoginClicked = {}
    )
}
