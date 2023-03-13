package com.wire.android.ui.settings.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.hiltSavedStateViewModel
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceDialog
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceDialogState
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceError
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.wirePrimaryButtonColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.settings.devices.model.DeviceDetailsState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.dialogErrorStrings
import com.wire.android.util.extension.formatAsString
import com.wire.android.util.formatMediumDateTime
import com.wire.kalium.logic.data.conversation.ClientId
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

@Composable
fun DeviceDetailsScreen(
    backNavArgs: ImmutableMap<String, Any> = persistentMapOf(),
    viewModel: DeviceDetailsViewModel = hiltSavedStateViewModel(backNavArgs = backNavArgs)
) {
    viewModel.state?.let { state ->
        DeviceDetailsContent(
            state = state,
            onDeleteDevice = viewModel::removeDevice,
            onPasswordChange = viewModel::onPasswordChange,
            onRemoveConfirm = viewModel::onRemoveConfirmed,
            onDialogDismiss = viewModel::onDialogDismissed,
            onErrorDialogDismiss = viewModel::clearDeleteClientError,
            onNavigateBack = viewModel::navigateBack
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailsContent(
    state: DeviceDetailsState,
    onDeleteDevice: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onPasswordChange: (TextFieldValue) -> Unit = {},
    onRemoveConfirm: () -> Unit = {},
    onDialogDismiss: () -> Unit = {},
    onErrorDialogDismiss: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = onNavigateBack,
                elevation = 0.dp,
                title = state.device.name
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .background(MaterialTheme.wireColorScheme.surface)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            ) {
                if (!state.isCurrentDevice) {
                    Text(
                        text = stringResource(
                            id = if (BuildConfig.WIPE_ON_DEVICE_REMOVAL) {
                                R.string.remove_device_details_description_with_wipe
                            } else {
                                R.string.remove_device_details_description
                            }
                        ),
                        style = MaterialTheme.wireTypography.body01,
                        color = MaterialTheme.wireColorScheme.onBackground,
                        modifier = Modifier.padding(dimensions().spacing16x)
                    )
                    WirePrimaryButton(
                        text = stringResource(R.string.content_description_remove_devices_screen_remove_icon),
                        onClick = onDeleteDevice,
                        colors = wirePrimaryButtonColors().copy(enabled = colorsScheme().error),
                        modifier = Modifier.padding(
                            start = dimensions().spacing16x,
                            end = dimensions().spacing16x,
                            bottom = dimensions().spacing16x
                        )
                    )
                }
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
                with(state.device.registrationTime) {
                    DeviceDetailSectionContent(
                        stringResource(id = R.string.label_client_added_time),
                        this.formatMediumDateTime() ?: this
                    )
                }
            }
            item {
                DeviceDetailSectionContent(stringResource(id = R.string.label_client_device_id), state.device.clientId.formatAsString())
            }
        }
        if (state.removeDeviceDialogState is RemoveDeviceDialogState.Visible) {
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
        state = DeviceDetailsState(
            device = Device(
                clientId = ClientId(""),
                name = "My Device",
                registrationTime = "2022-03-24T18:02:30.360Z"
            ),
            isCurrentDevice = false
        ),
        onPasswordChange = { },
        onRemoveConfirm = { },
        onDialogDismiss = { },
        onErrorDialogDismiss = { }
    )
}
