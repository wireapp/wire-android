package com.wire.android.ui.authentication.devices

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun RemoveDeviceScreen(navController: NavController) {
    val viewModel: RemoveDeviceViewModel = hiltViewModel()
    val state: RemoveDeviceState = viewModel.state
    RemoveDeviceContent(navController = navController, state = state)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoveDeviceContent(
    navController: NavController,
    state: RemoveDeviceState
) {
    Scaffold(
        topBar = { RemoveDeviceTopBar(onBackNavigationPressed = { navController.popBackStack() }) }
    ) {
            SurfaceBackgroundWrapper {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(state.deviceList) { device ->
                        RemoveDeviceItem(device)
                    }
                }
            }
    }
}

@Composable
private fun RemoveDeviceItem(device: Device) {  //TODO
    Text(
        text = device.name,
        style = MaterialTheme.wireTypography.body02,
        color = MaterialTheme.wireColorScheme.onBackground,
        modifier = Modifier.padding(
            horizontal = MaterialTheme.wireDimensions.removeDeviceHorizontalPadding
        )
    )
}

@Preview
@Composable
private fun RemoveDeviceScreenPreview() {
    RemoveDeviceContent(
        navController = rememberNavController(),
        state = RemoveDeviceState(listOf())
    )
}
