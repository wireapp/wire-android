/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */
package com.wire.android.ui.authentication.devices.remove

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.unit.Dp
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
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.dialogErrorStrings
import com.wire.android.util.extension.folderWithElements
import com.wire.android.util.formatMediumDateTime
import kotlinx.collections.immutable.toImmutableList

@Composable
fun RemoveDeviceScreen() {
    val viewModel: RemoveDeviceViewModel = hiltViewModel()
    val clearSessionViewModel: ClearSessionViewModel = hiltViewModel()
    val state: RemoveDeviceState = viewModel.state
    val clearSessionState: ClearSessionState = clearSessionViewModel.state

    clearAutofillTree()
    RemoveDeviceContent(
        state = state,
        title = stringResource(id = R.string.remove_device_title),
        description = stringResource(id = R.string.remove_device_message),
        clearSessionState = clearSessionState,
        onItemClicked = viewModel::onItemClicked,
        onPasswordChange = viewModel::onPasswordChange,
        onRemoveConfirm = viewModel::onRemoveConfirmed,
        onDialogDismiss = viewModel::onDialogDismissed,
        onErrorDialogDismiss = viewModel::clearDeleteClientError,
        onBackButtonClicked = clearSessionViewModel::onBackButtonClicked,
        onCancelLoginClicked = clearSessionViewModel::onCancelLoginClicked,
        onProceedLoginClicked = clearSessionViewModel::onProceedLoginClicked,
        navigationIconType = NavigationIconType.Close
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoveDeviceContent(
    title: String,
    description: String?,
    state: RemoveDeviceState,
    clearSessionState: ClearSessionState,
    onItemClicked: (Device) -> Unit,
    onPasswordChange: (TextFieldValue) -> Unit,
    onRemoveConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
    onErrorDialogDismiss: () -> Unit,
    onCancelLoginClicked: () -> Unit,
    onProceedLoginClicked: () -> Unit,
    onBackButtonClicked: () -> Unit,
    navigationIconType: NavigationIconType
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
            title = title,
            description = description,
            elevation = lazyListState.rememberTopBarElevationState().value,
            onBackButtonClicked = onBackButtonClicked,
            navigationIconType = navigationIconType
        )
    }) { internalPadding ->
        Box(modifier = Modifier.padding(internalPadding)) {
            when (state.isLoadingClientsList) {
                true -> RemoveDeviceItemsList(lazyListState, List(5) { Device() }, true, onItemClicked, state.currentDevice)
                false -> RemoveDeviceItemsList(lazyListState, state.deviceList, false, onItemClicked, state.currentDevice)
            }
        }
        // TODO handle list loading errors
        if (!state.isLoadingClientsList && state.removeDeviceDialogState is RemoveDeviceDialogState.Visible) {
            RemoveDeviceDialog(
                errorState = state.error,
                state = state.removeDeviceDialogState,
                onPasswordChange = onPasswordChange,
                onDialogDismiss = onDialogDismiss,
                onRemoveConfirm = onRemoveConfirm
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
                        type = WireDialogButtonType.Primary
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
    currentDevice: Device? = null,
    context: Context = LocalContext.current
) {
    SurfaceBackgroundWrapper {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxWidth()
        ) {
            currentDevice?.let { currentDevice ->
                folderDeviceItems(
                    context.getString(R.string.current_device_label),
                    listOf(currentDevice),
                    placeholders,
                    null
                )
            }
            folderDeviceItems(
                context.getString(R.string.other_devices_label),
                items,
                placeholders,
                onItemClicked
            )
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
    onRemoveConfirm: () -> Unit
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

internal fun LazyListScope.folderDeviceItems(
    header: String,
    items: List<Device>,
    placeholders: Boolean,
    onItemClicked: ((Device) -> Unit)? = null
) {
    folderWithElements(
        header = header.uppercase(),
        items = items.associateBy { it.clientId.value },
        divider = {
            Divider(
                color = MaterialTheme.wireColorScheme.background,
                thickness = Dp.Hairline
            )
        }
    ) { item ->
        DeviceItem(
            item,
            background = MaterialTheme.wireColorScheme.surface,
            placeholder = placeholders,
            onRemoveDeviceClick = onItemClicked
        )
    }
}

@Preview
@Composable
fun PreviewRemoveDeviceScreen() {
    RemoveDeviceContent(
        state = RemoveDeviceState(
            List(10) { Device() }.toImmutableList(),
            RemoveDeviceDialogState.Hidden,
            isLoadingClientsList = false,
            error = RemoveDeviceError.None,
            null
        ),
        title = "Remove device",
        description = "Remove a device from your account.",
        clearSessionState = ClearSessionState(),
        onItemClicked = {},
        onPasswordChange = {},
        onRemoveConfirm = {},
        onDialogDismiss = {},
        onErrorDialogDismiss = {},
        onBackButtonClicked = {},
        onCancelLoginClicked = {},
        onProceedLoginClicked = {},
        navigationIconType = NavigationIconType.Close
    )
}
