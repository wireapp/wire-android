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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.feature.NavigationSwitchAccountActions
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.authentication.devices.DeviceItem
import com.wire.android.ui.authentication.devices.common.ClearSessionState
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModel
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.dialogs.CancelLoginDialogContent
import com.wire.android.ui.common.dialogs.CancelLoginDialogState
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.textfield.clearAutofillTree
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.destinations.InitialSyncScreenDestination
import com.wire.android.util.dialogErrorStrings

@RootNavGraph
@Destination(
    style = PopUpNavigationAnimation::class,
)
@Composable
fun RemoveDeviceScreen(navigator: Navigator) {
    val viewModel: RemoveDeviceViewModel = hiltViewModel()
    val clearSessionViewModel: ClearSessionViewModel = hiltViewModel()
    val state: RemoveDeviceState = viewModel.state
    val clearSessionState: ClearSessionState = clearSessionViewModel.state

    fun navigateAfterSuccess(initialSyncCompleted: Boolean) = navigator.navigate(
        NavigationCommand(
            destination = if (initialSyncCompleted) HomeScreenDestination else InitialSyncScreenDestination,
            backStackMode = BackStackMode.CLEAR_WHOLE
        )
    )

    clearAutofillTree()
    RemoveDeviceContent(
        state = state,
        clearSessionState = clearSessionState,
        onItemClicked = { viewModel.onItemClicked(it) { navigateAfterSuccess(it) } },
        onPasswordChange = viewModel::onPasswordChange,
        onRemoveConfirm = { viewModel.onRemoveConfirmed { navigateAfterSuccess(it) } },
        onDialogDismiss = viewModel::onDialogDismissed,
        onErrorDialogDismiss = viewModel::clearDeleteClientError,
        onBackButtonClicked = clearSessionViewModel::onBackButtonClicked,
        onCancelLoginClicked = { clearSessionViewModel.onCancelLoginClicked(NavigationSwitchAccountActions(navigator::navigate)) },
        onProceedLoginClicked = clearSessionViewModel::onProceedLoginClicked
    )
}

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
    onItemClicked: (Device) -> Unit
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
                    onRemoveDeviceClick = onItemClicked,
                    shouldShowVerifyLabel = false,
                    leadingIcon = {
                        Icon(
                            painterResource(id = R.drawable.ic_remove),
                            stringResource(R.string.content_description_remove_devices_screen_remove_icon)
                        )
                    }
                )
                if (index < items.lastIndex) WireDivider()
            }
        }
    }
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
