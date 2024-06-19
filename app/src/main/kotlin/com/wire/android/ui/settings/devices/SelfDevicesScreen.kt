/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.settings.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.authentication.devices.DeviceItem
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.DeviceDetailsScreenDestination
import com.wire.android.ui.settings.devices.model.SelfDevicesState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.extension.folderWithElements
import com.wire.kalium.logic.data.conversation.ClientId

@RootNavGraph
@Destination
@Composable
fun SelfDevicesScreen(
    navigator: Navigator,
    viewModel: SelfDevicesViewModel = hiltViewModel()
) {
    SelfDevicesScreenContent(
        state = viewModel.state,
        onNavigateBack = navigator::navigateBack,
        onDeviceClick = { navigator.navigate(NavigationCommand(DeviceDetailsScreenDestination(viewModel.currentAccountId, it.clientId))) }
    )

    if (viewModel.state.error is SelfDevicesState.Error.InitError) {
        WireDialog(
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false, usePlatformDefaultWidth = false),
            title = stringResource(id = R.string.label_general_error),
            text = stringResource(id = R.string.devices_loading_error),
            onDismiss = navigator::navigateBack,
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = navigator::navigateBack,
                text = stringResource(id = R.string.label_close),
                type = WireDialogButtonType.Secondary,
            ),
            optionButton1Properties = WireDialogButtonProperties(
                onClick = viewModel::retryFetch,
                text = stringResource(id = R.string.label_retry),
                type = WireDialogButtonType.Primary,
            ),
        )
    }
}

@Composable
fun SelfDevicesScreenContent(
    state: SelfDevicesState,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onDeviceClick: (Device) -> Unit = {},
) {
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current

    WireScaffold(
        modifier = modifier,
        topBar = {
            TopBarHeader(
                onNavigateBack = onNavigateBack,
                elevation = lazyListState.rememberTopBarElevationState().value,
            )
        },
        content = { paddingValues ->
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                when (state.isLoadingClientsList) {
                    true -> {
                        folderDeviceItems(
                            header = null,
                            items = List(4) { Device(clientId = ClientId("placeholder_$it")) },
                            shouldShowVerifyLabel = false,
                            isCurrentClient = false,
                            isE2EIEnabled = false,
                            placeholders = true,
                        )
                    }
                    false -> {
                        state.currentDevice?.let { currentDevice ->
                            folderDeviceItems(
                                header = context.getString(R.string.current_device_label),
                                items = listOf(currentDevice),
                                shouldShowVerifyLabel = true,
                                isCurrentClient = true,
                                isE2EIEnabled = state.isE2EIEnabled,
                                onDeviceClick = onDeviceClick,
                            )
                        }
                        folderDeviceItems(
                            header = context.getString(R.string.other_devices_label),
                            items = state.deviceList,
                            shouldShowVerifyLabel = true,
                            isCurrentClient = false,
                            isE2EIEnabled = state.isE2EIEnabled,
                            onDeviceClick = onDeviceClick
                        )
                    }
                }
            }
        }
    )
}

@Suppress("LongParameterList")
private fun LazyListScope.folderDeviceItems(
    header: String?,
    items: List<Device>,
    shouldShowVerifyLabel: Boolean,
    isCurrentClient: Boolean,
    isE2EIEnabled: Boolean,
    placeholders: Boolean = false,
    onDeviceClick: (Device) -> Unit = {}
) {
    folderWithElements(
        header = header?.uppercase(),
        items = items.associateBy { it.clientId.value },
        divider = {
            HorizontalDivider(
                thickness = Dp.Hairline,
                color = MaterialTheme.wireColorScheme.background
            )
        }
    ) { item: Device ->
        DeviceItem(
            device = item,
            modifier = Modifier
                .background(MaterialTheme.wireColorScheme.surface),
            placeholder = placeholders,
            onClickAction = onDeviceClick,
            icon = { ArrowRightIcon() },
            isWholeItemClickable = true,
            shouldShowVerifyLabel = shouldShowVerifyLabel,
            isCurrentClient = isCurrentClient,
            shouldShowE2EIInfo = isE2EIEnabled
        )
    }
}

@Composable
private fun TopBarHeader(
    elevation: Dp = 0.dp,
    onNavigateBack: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onNavigateBack,
        title = stringResource(id = R.string.devices_title),
        elevation = elevation,
    )
}
