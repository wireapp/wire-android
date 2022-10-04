package com.wire.android.ui.settings.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.authentication.devices.DeviceItem
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.settings.devices.model.DevicesState
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun DevicesScreen(viewModel: DevicesViewModel = hiltViewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }

    DevicesScreenContent(
        snackbarHostState = snackbarHostState,
        state = viewModel.state,
        onNavigateBack = viewModel::navigateBack
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
)
@Composable
fun DevicesScreenContent(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateBack: () -> Unit = {},
    state: DevicesState,
) {
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current

    val headerColor = MaterialTheme.colorScheme.background

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
                    .background(color = MaterialTheme.wireColorScheme.surface)
            ) {
                when (state.isLoadingClientsList) {
                    true -> items(count = 4, itemContent = { Device() })
                    false -> {
                        state.currentDevice?.let { currentDevice ->
                            item {
                                FolderHeader(
                                    name = context.getString(R.string.current_device_label).uppercase(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(color = headerColor)
                                )
                            }
                            item { DeviceItem(currentDevice, placeholder = false) }
                            item { Divider() }
                        }
                        item {
                            FolderHeader(
                                name = context.getString(R.string.other_devices_label).uppercase(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(color = headerColor)
                            )
                        }
                        item { Divider() }
                        itemsIndexed(state.deviceList) { index, item ->
                            DeviceItem(item, placeholder = false)
                            if (index < state.deviceList.lastIndex) Divider()
                        }
                    }
                }
            }
        },
    )
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
