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

package com.wire.android.ui.settings.devices

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.authentication.devices.common.ClearSessionState
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceContent
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceDialogState
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceError
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceState
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceViewModel
import com.wire.android.ui.common.topappbar.NavigationIconType

@Composable
fun SelfDevicesScreen() {
    val removeDeviceViewModel: RemoveDeviceViewModel = hiltViewModel()
    val selfDeviceViewModel: SelfDevicesViewModel = hiltViewModel()
    val state = removeDeviceViewModel.state
    val clearSessionState = remember { ClearSessionState(showCancelLoginDialog = false) }
    RemoveDeviceContent(
        state = state,
        title = stringResource(id = R.string.devices_title),
        description = null,
        clearSessionState = clearSessionState,
        onItemClicked = removeDeviceViewModel::onItemClicked,
        onPasswordChange = removeDeviceViewModel::onPasswordChange,
        onRemoveConfirm = removeDeviceViewModel::onRemoveConfirmed,
        onDialogDismiss = removeDeviceViewModel::onDialogDismissed,
        onErrorDialogDismiss = removeDeviceViewModel::clearDeleteClientError,
        onBackButtonClicked = selfDeviceViewModel::navigateBack,
        onCancelLoginClicked = {},
        onProceedLoginClicked = {},
        navigationIconType = NavigationIconType.Back
    )
}

@Preview
@Composable
fun PreviewSelfDevicesScreen() {
    RemoveDeviceContent(
        state = RemoveDeviceState(
            List(10) { Device() },
            RemoveDeviceDialogState.Hidden,
            isLoadingClientsList = false,
            error = RemoveDeviceError.None,
            null
        ),
        title = "Your devices",
        description = null,
        clearSessionState = ClearSessionState(),
        onItemClicked = {},
        onPasswordChange = {},
        onRemoveConfirm = {},
        onDialogDismiss = {},
        onErrorDialogDismiss = {},
        onBackButtonClicked = {},
        onCancelLoginClicked = {},
        onProceedLoginClicked = {},
        navigationIconType = NavigationIconType.Back
    )
}
