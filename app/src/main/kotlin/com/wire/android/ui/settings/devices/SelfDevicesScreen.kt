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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.authentication.devices.DeviceItem
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.settings.devices.model.SelfDevicesState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.extension.folderWithElements

@Composable
fun SelfDevicesScreen(viewModel: SelfDevicesViewModel = hiltViewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }

    SelfDevicesScreenContent(
        snackbarHostState = snackbarHostState,
        state = viewModel.state,
        onNavigateBack = viewModel::navigateBack
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
)
@Composable
fun SelfDevicesScreenContent(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateBack: () -> Unit = {},
    state: SelfDevicesState,
) {
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current

    Scaffold(
        snackbarHost = {
            SwipeDismissSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.fillMaxWidth()
            )
        },
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
                                context.getString(R.string.current_device_label),
                                listOf(currentDevice)
                            )
                        }
                        folderDeviceItems(
                            context.getString(R.string.other_devices_label),
                            state.deviceList
                        )
                    }
                }
            }
        },
    )
}

internal fun LazyListScope.folderDeviceItems(
    header: String,
    items: List<Device>,
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
            placeholder = false
        )
    }
}

@Composable
private fun TopBarHeader(
    onNavigateBack: () -> Unit,
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onNavigateBack,
        title = stringResource(id = R.string.devices_title),
        elevation = 0.dp
    )
}
