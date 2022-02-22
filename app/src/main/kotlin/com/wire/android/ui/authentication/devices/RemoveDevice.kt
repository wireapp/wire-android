package com.wire.android.ui.authentication.devices

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.common.appBarElevation

@Composable
fun RemoveDeviceScreen() {
    val viewModel: RemoveDeviceViewModel = hiltViewModel()
    val state: RemoveDeviceState = viewModel.state
    RemoveDeviceContent(
        viewModel = viewModel,
        state = state,
        onItemClicked = viewModel::onItemClicked,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoveDeviceContent(
    viewModel: RemoveDeviceViewModel,
    state: RemoveDeviceState,
    onItemClicked: (Device) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    Scaffold(
        topBar = {
            RemoveDeviceTopBar(
                elevation = lazyListState.appBarElevation(),
                onBackNavigationPressed = { viewModel.navigateBack() })
        }
    ) {
        SurfaceBackgroundWrapper {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(state.deviceList) { index, device ->
                    RemoveDeviceItem(device, onItemClicked)
                    if (index < state.deviceList.lastIndex) Divider()
                }
            }
        }
    }
}

@Preview
@Composable
private fun RemoveDeviceScreenPreview() {
    RemoveDeviceContent(
        viewModel = hiltViewModel(),
        state = RemoveDeviceState(listOf()),
        onItemClicked = {},
    )
}
