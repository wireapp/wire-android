package com.wire.android.ui.settings.devices

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.navigation.hiltSavedStateViewModel
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceDialog
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceDialogState
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceError
import com.wire.android.ui.common.CopyButton
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.WireSwitch
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.wirePrimaryButtonColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.settings.devices.model.DeviceDetailsState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.dialogErrorStrings
import com.wire.android.util.extension.formatAsFingerPrint
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
    DeviceDetailsContent(
        state = viewModel.state,
        onDeleteDevice = viewModel::removeDevice,
        onPasswordChange = viewModel::onPasswordChange,
        onRemoveConfirm = viewModel::onRemoveConfirmed,
        onDialogDismiss = viewModel::onDialogDismissed,
        onErrorDialogDismiss = viewModel::clearDeleteClientError,
        onNavigateBack = viewModel::navigateBack,
        onUpdateClientVerification = viewModel::onUpdateVerificationStatus
    )
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
    onErrorDialogDismiss: () -> Unit = {},
    onUpdateClientVerification: (Boolean) -> Unit = {}
) {
    val screenState = rememberConversationScreenState()
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = onNavigateBack,
                elevation = 0.dp,
                title = state.device.name
            )
        },
        snackbarHost = {
            SwipeDismissSnackbarHost(
                hostState = screenState.snackBarHostState,
                modifier = Modifier.fillMaxWidth()
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .background(MaterialTheme.wireColorScheme.surface)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            ) {
                if (!state.isCurrentDevice && state.isSelfClient) {
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

            state.device.registrationTime?.formatMediumDateTime()?.let {
                item {
                    DeviceDetailSectionContent(
                        stringResource(id = R.string.label_client_added_time),
                        AnnotatedString(it)
                    )
                    Divider(color = MaterialTheme.wireColorScheme.background)
                }
            }

            item {
                DeviceIdItem(state, screenState::copyMessage)
                Divider(color = MaterialTheme.wireColorScheme.background)
            }

            item {
                DeviceKeyFingerprintItem(state.fingerPrint, screenState::copyMessage)
                Divider(color = MaterialTheme.wireColorScheme.background)
            }

            if (!state.isCurrentDevice) {
                item {
                    DeviceVerificationItem(
                        state.device.isVerified,
                        state.fingerPrint != null,
                        state.isSelfClient,
                        state.userName,
                        onUpdateClientVerification
                    )
                    Divider(color = MaterialTheme.wireColorScheme.background)
                }
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
private fun DeviceIdItem(state: DeviceDetailsState, onCopy: (String) -> Unit) {
    DeviceDetailSectionContent(
        sectionTitle = stringResource(id = R.string.label_client_device_id),
        sectionText = AnnotatedString(state.device.clientId.formatAsString()),
        titleTrailingItem = {
            CopyButton(
                onCopyClicked = {
                    state.device.clientId.formatAsString().let { id -> onCopy(id) }
                }
            )
        }
    )
}

@Composable
fun DeviceKeyFingerprintItem(
    clientFingerPrint: String?,
    onCopy: (String) -> Unit
) {
    DeviceDetailSectionContent(
        stringResource(id = R.string.title_device_key_fingerprint),
        sectionText = clientFingerPrint?.formatAsFingerPrint()
            ?: AnnotatedString(stringResource(id = R.string.label_client_key_fingerprint_not_available)),
        enabled = clientFingerPrint != null,
        titleTrailingItem = {
            CopyButton(
                onCopyClicked = {
                    clientFingerPrint?.let { fingerprint -> onCopy(fingerprint) }
                },
                state = if (clientFingerPrint != null) WireButtonState.Default else WireButtonState.Disabled
            )
        }
    )
}

@Composable
fun DeviceVerificationItem(
    state: Boolean,
    enabled: Boolean,
    isSelfClient: Boolean,
    userName: String?,
    onStatusChange: (Boolean) -> Unit,
) {
    @StringRes
    val subTitle = if (state) {
        R.string.label_client_verified
    } else {
        R.string.label_client_unverified
    }
    DeviceDetailSectionContent(
        stringResource(id = R.string.title_device_key_fingerprint),
        AnnotatedString(stringResource(id = subTitle)),
        titleTrailingItem = {
            WireSwitch(
                checked = state,
                onCheckedChange = onStatusChange,
                enabled = enabled,
                modifier = Modifier.padding(end = dimensions().spacing16x)
            )
        }
    )
    VerificationDescription(isSelfClient, userName)
}

@Composable
private fun VerificationDescription(
    isSelfClient: Boolean,
    userName: String?
) {
    // TODO: add learn more according to design (need to figure out the link first)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensions().spacing16x,
                end = dimensions().spacing16x,
                bottom = dimensions().spacing16x
            )
    ) {

        if (isSelfClient) {
            stringResource(id = R.string.label_self_client_verification_description)
        } else {
            stringResource(
                id = R.string.label_client_verification_description,
                userName ?: stringResource(id = R.string.unknown_user_name)
            )
        }.let {
            Text(
                text = it,
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
            )
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        )

        if (isSelfClient) {
            stringResource(id = R.string.label_self_fingerprint_description)
        } else {
            stringResource(id = R.string.label_fingerprint_description)
        }.let {
            Text(
                text = it,
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
            )
        }
    }
}

@Composable
private fun DeviceDetailSectionContent(
    sectionTitle: String,
    sectionText: AnnotatedString,
    enabled: Boolean = true,
    titleTrailingItem: (@Composable () -> Unit)? = null
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
                    color = if (enabled) MaterialTheme.wireColorScheme.onBackground
                    else MaterialTheme.wireColorScheme.secondaryText,
                    modifier = Modifier.weight(weight = 1f, fill = true)
                )

                if (titleTrailingItem != null) {
                    Box(modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing8x)) { titleTrailingItem() }
                }
            }
        }
    }
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
