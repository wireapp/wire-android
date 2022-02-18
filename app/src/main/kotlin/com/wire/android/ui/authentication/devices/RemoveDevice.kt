package com.wire.android.ui.authentication.devices

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.authentication.devices.mock.mockDevices
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.appBarElevation
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.util.dialogErrorStrings
import com.wire.android.util.formatMediumDateTime

@Composable
fun RemoveDeviceScreen(navController: NavController) {
    val viewModel: RemoveDeviceViewModel = hiltViewModel()
    val state: RemoveDeviceState = viewModel.state
    RemoveDeviceContent(
        navController = navController,
        state = state,
        onItemClicked = viewModel::onItemClicked,
        onPasswordChange = viewModel::onPasswordChange,
        onRemoveConfirm = viewModel::onRemoveConfirmed,
        onDialogDismiss = viewModel::onDialogDismissed,
        onErrorDialogDismiss = viewModel::clearDeleteClientError
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoveDeviceContent(
    navController: NavController,
    state: RemoveDeviceState,
    onItemClicked: (Device) -> Unit,
    onPasswordChange: (TextFieldValue) -> Unit,
    onRemoveConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
    onErrorDialogDismiss: () -> Unit
) {
    val lazyListState = rememberLazyListState()
    Scaffold(
        topBar = {
            RemoveDeviceTopBar(
                elevation = lazyListState.appBarElevation(),
                onBackNavigationPressed = { navController.popBackStack() }) //TODO logout?
        }
    ) {
        when(state) {
            is RemoveDeviceState.Success ->
                RemoveDeviceItemsList(lazyListState, state.deviceList, false, onItemClicked)
            RemoveDeviceState.Loading ->
                RemoveDeviceItemsList(lazyListState, mockDevices, true, onItemClicked)
        }
        //TODO handle list loading errors
        if (state is RemoveDeviceState.Success && state.removeDeviceDialogState is RemoveDeviceDialogState.Visible) {
            RemoveDeviceDialog(
                state = state.removeDeviceDialogState,
                onPasswordChange = onPasswordChange,
                onDialogDismiss = onDialogDismiss,
                onRemoveConfirm = onRemoveConfirm,
            )
            if(state.removeDeviceDialogState.error is RemoveDeviceError.GenericError) {
                val (title, message) = state.removeDeviceDialogState.error.coreFailure.dialogErrorStrings(LocalContext.current.resources)
                WireDialog(
                    title = title,
                    text = message,
                    onDismiss = onErrorDialogDismiss,
                    confirmButtonProperties = WireDialogButtonProperties(
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

@Composable
private fun RemoveDeviceDialog(
    state: RemoveDeviceDialogState.Visible,
    onPasswordChange: (TextFieldValue) -> Unit,
    onDialogDismiss: () -> Unit,
    onRemoveConfirm: () -> Unit,
) {
    WireDialog(
        title = stringResource(R.string.remove_device_dialog_title),
        text = state.device.name + "\n" +
                stringResource(
                    R.string.remove_device_id_and_time_label,
                    state.device.clientId.value,
                    state.device.registrationTime.formatMediumDateTime() ?: ""
                ),
        onDismiss = onDialogDismiss,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onDialogDismiss,
            text = stringResource(id = R.string.label_cancel),
            state = if(state.removeEnabled) WireButtonState.Default else WireButtonState.Disabled
        ),
        confirmButtonProperties = WireDialogButtonProperties(
            onClick = onRemoveConfirm,
            text = stringResource(id = if(state.loading) R.string. label_removing else R.string.label_remove),
            type = WireDialogButtonType.Primary,
            loading = state.loading,
            state = if(state.removeEnabled) WireButtonState.Error else WireButtonState.Disabled
        ),
        content = {
            WirePasswordTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                state = when {
                    state.error is RemoveDeviceError.InvalidCredentialsError ->
                        WireTextFieldState.Error(stringResource(id = R.string.remove_device_invalid_password))
                    state.loading -> WireTextFieldState.Disabled
                    else -> WireTextFieldState.Default
                }
            )
        }
    )
}


@Preview
@Composable
private fun RemoveDeviceScreenPreview() {
    RemoveDeviceContent(
        navController = rememberNavController(),
        state = RemoveDeviceState.Success(mockDevices, RemoveDeviceDialogState.Hidden),
        onItemClicked = {},
        onPasswordChange = {},
        onRemoveConfirm = {},
        onDialogDismiss = {},
        onErrorDialogDismiss = {}
    )
}
