package com.wire.android.ui.authentication.devices.remove

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
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.appBarElevation
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.dialogErrorStrings
import com.wire.android.util.formatMediumDateTime

@Composable
fun RemoveDeviceScreen() {
    val viewModel: RemoveDeviceViewModel = hiltViewModel()
    val state: RemoveDeviceState = viewModel.state
    RemoveDeviceContent(
        state = state,
        onItemClicked = viewModel::onItemClicked,
        onPasswordChange = viewModel::onPasswordChange,
        onRemoveConfirm = viewModel::onRemoveConfirmed,
        onDialogDismiss = viewModel::onDialogDismissed,
        onErrorDialogDismiss = viewModel::clearDeleteClientError,
    )
}

typealias HideKeyboard = () -> Unit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun RemoveDeviceContent(
    state: RemoveDeviceState,
    onItemClicked: (Device) -> Unit,
    onPasswordChange: (TextFieldValue) -> Unit,
    onRemoveConfirm: (HideKeyboard) -> Unit,
    onDialogDismiss: () -> Unit,
    onErrorDialogDismiss: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    Scaffold(topBar = { RemoveDeviceTopBar(elevation = lazyListState.appBarElevation()) }) {
        when (state) {
            is RemoveDeviceState.Success ->
                RemoveDeviceItemsList(lazyListState, state.deviceList, false, onItemClicked)
            RemoveDeviceState.Loading ->
                RemoveDeviceItemsList(lazyListState, List(10) { Device() }, true, onItemClicked)
        }
        // TODO handle list loading errors
        if (state is RemoveDeviceState.Success && state.removeDeviceDialogState is RemoveDeviceDialogState.Visible) {
            RemoveDeviceDialog(
                state = state.removeDeviceDialogState,
                onPasswordChange = onPasswordChange,
                onDialogDismiss = onDialogDismiss,
                onRemoveConfirm = onRemoveConfirm,
            )
            if (state.removeDeviceDialogState.error is RemoveDeviceError.GenericError) {
                val (title, message) = state.removeDeviceDialogState.error.coreFailure.dialogErrorStrings(LocalContext.current.resources)
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
                RemoveDeviceItem(device, placeholders, onItemClicked)
                if (index < items.lastIndex) Divider()
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RemoveDeviceDialog(
    state: RemoveDeviceDialogState.Visible,
    onPasswordChange: (TextFieldValue) -> Unit,
    onDialogDismiss: () -> Unit,
    onRemoveConfirm: (HideKeyboard) -> Unit,
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
                onRemoveConfirm {
                    keyboardController?.hide()
                }
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
                    state.error is RemoveDeviceError.InvalidCredentialsError ->
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
private fun RemoveDeviceScreenPreview() {
    RemoveDeviceContent(
        state = RemoveDeviceState.Success(List(10) { Device(name = "device") }, RemoveDeviceDialogState.Hidden),
        onItemClicked = {},
        onPasswordChange = {},
        onRemoveConfirm = {},
        onDialogDismiss = {},
        onErrorDialogDismiss = {}
    )
}
