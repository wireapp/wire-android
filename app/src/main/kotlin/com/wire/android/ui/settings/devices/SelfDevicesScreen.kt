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
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.settings.devices.model.SelfDevicesState
import com.wire.android.ui.theme.wireColorScheme

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
            ) {
                when (state.isLoadingClientsList) {
                    true -> items(count = 4, itemContent = { Device() })
                    false -> {
                        state.currentDevice?.let { currentDevice ->
                            folderWithElements(
                                context.getString(R.string.current_device_label),
                                listOf(currentDevice)
                            )
                        }
                        folderWithElements(
                            context.getString(R.string.other_devices_label),
                            state.deviceList
                        )
                    }
                }
            }
        },
    )
}

private fun LazyListScope.folderWithElements(
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
        DeviceItem(item,
            background = MaterialTheme.wireColorScheme.surface,
            placeholder = false)
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
