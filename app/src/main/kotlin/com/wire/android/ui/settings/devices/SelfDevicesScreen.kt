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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.authentication.devices.DeviceItem
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.DeviceDetailsScreenDestination
import com.wire.android.ui.settings.devices.model.SelfDevicesState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.extension.folderWithElements

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
}

@Composable
fun SelfDevicesScreenContent(
    onNavigateBack: () -> Unit = {},
    onDeviceClick: (Device) -> Unit = {},
    state: SelfDevicesState
) {
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current

    WireScaffold(
        topBar = {
            TopBarHeader(
                onNavigateBack = onNavigateBack
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
                    true -> items(count = 4, itemContent = { Device() })
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
    header: String,
    items: List<Device>,
    shouldShowVerifyLabel: Boolean,
    isCurrentClient: Boolean,
    isE2EIEnabled: Boolean,
    onDeviceClick: (Device) -> Unit = {}
) {
    folderWithElements(
        header = header.uppercase(),
        items = items.associateBy { it.clientId.value },
        divider = {
            HorizontalDivider(
                thickness = Dp.Hairline,
                color = MaterialTheme.wireColorScheme.background
            )
        }
    ) { item: Device ->
        DeviceItem(
            item,
            background = MaterialTheme.wireColorScheme.surface,
            placeholder = false,
            onClickAction = onDeviceClick,
            icon = Icons.Filled.ChevronRight.Icon(),
            isWholeItemClickable = true,
            shouldShowVerifyLabel = shouldShowVerifyLabel,
            isCurrentClient = isCurrentClient,
            shouldShowE2EIInfo = isE2EIEnabled
        )
    }
}

@Composable
private fun TopBarHeader(
    onNavigateBack: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onNavigateBack,
        title = stringResource(id = R.string.devices_title),
        elevation = 0.dp
    )
}
