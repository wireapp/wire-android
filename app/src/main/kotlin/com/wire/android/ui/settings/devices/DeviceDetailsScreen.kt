package com.wire.android.ui.settings.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.hiltSavedStateViewModel
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.wirePrimaryButtonColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.conversation.ClientId
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

@Composable
fun DeviceDetailsScreen(
    backNavArgs: ImmutableMap<String, Any> = persistentMapOf(),
    viewModel: DeviceDetailsViewModel = hiltSavedStateViewModel(backNavArgs = backNavArgs)
) {
    with(viewModel.state) {
        device?.let {
            DeviceDetailsContent(
                device = it,
                onDeleteDevice = { },
                onNavigateBack = viewModel::navigateBack
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailsContent(
    device: Device,
    onDeleteDevice: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = onNavigateBack,
                elevation = 0.dp,
                title = device.name
            )
        },
        bottomBar = {
            if (true) { // check for current device
                WirePrimaryButton(
                    text = stringResource(R.string.content_description_remove_devices_screen_remove_icon),
                    onClick = onDeleteDevice,
                    modifier = Modifier.padding(dimensions().spacing16x),
                    colors = wirePrimaryButtonColors().copy(enabled = colorsScheme().error)
                )
            }
        }
    ) { internalPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
                .background(MaterialTheme.wireColorScheme.surface)
        ) {
            item {
                DeviceDetailSectionContent("ADDED", device.registrationTime)
            }
            item {
                DeviceDetailSectionContent("Device Id", device.clientId.value)
            }
        }
    }
}

@Composable
private fun DeviceDetailSectionContent(
    sectionTitle: String,
    sectionText: String = "",
    titleTrailingItem: (@Composable () -> Unit)? = null,
    clickable: Clickable = Clickable(enabled = false, onClick = { /* not handled */ }, onLongClick = { /* not handled */ })
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(
                top = MaterialTheme.wireDimensions.spacing12x,
                bottom = MaterialTheme.wireDimensions.spacing12x,
                start = MaterialTheme.wireDimensions.spacing16x,
                end = MaterialTheme.wireDimensions.spacing12x
            )
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                text = sectionTitle,
                style = MaterialTheme.wireTypography.label01,
                color = MaterialTheme.wireColorScheme.secondaryText,
                modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.spacing4x)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sectionText,
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.onBackground,
                    modifier = Modifier.weight(weight = 1f, fill = true)
                )
                if (titleTrailingItem != null) {
                    Box(modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing8x)) { titleTrailingItem() }
                }
            }
        }
    }
    Divider(color = MaterialTheme.wireColorScheme.background)
}

@Preview
@Composable
fun PreviewDeviceDetailsScreen() {
    DeviceDetailsContent(
        Device(
            clientId = ClientId("1234567890"),
            registrationTime = "2021-09-01T12:00:00.000Z",
            name = "My Pixel 3a",
            isValid = true
        )
    )
}
