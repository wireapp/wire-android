package com.wire.android.ui.authentication.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.getMinTouchMargins
import com.wire.android.ui.common.shimmerPlaceholder
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.extension.formatAsString
import com.wire.android.util.formatMediumDateTime

@Composable
fun DeviceItem(
    device: Device,
    placeholder: Boolean,
    background: Color? = null,
    onRemoveDeviceClick: ((Device) -> Unit)? = null
) {
    DeviceItemContent(
        device = device,
        placeholder = placeholder,
        background = background,
        onRemoveDeviceClick = onRemoveDeviceClick
    )
}

@Composable
private fun DeviceItemContent(
    device: Device,
    placeholder: Boolean,
    background: Color? = null,
    onRemoveDeviceClick: ((Device) -> Unit)?
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = if (background != null) Modifier.background(color = background) else Modifier
    ) {
        Row(
            modifier = Modifier
                .padding(MaterialTheme.wireDimensions.removeDeviceItemPadding)
                .weight(1f)
        ) {
            Icon(
                modifier = Modifier.shimmerPlaceholder(visible = placeholder),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_devices),
                contentDescription = stringResource(R.string.content_description_remove_devices_screen_device_item_icon)
            )
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .padding(start = MaterialTheme.wireDimensions.removeDeviceItemPadding)
                    .weight(1f),
            ) { DeviceItemTexts(device, placeholder) }
        }
        val (buttonTopPadding, buttonEndPadding) = getMinTouchMargins(minSize = MaterialTheme.wireDimensions.buttonSmallMinSize)
            .let {
                // default button touch area [48x48] is higher than button size [40x32] so it will have margins, we have to subtract
                // these margins from the default item padding so that all elements are the same distance from the edge
                Pair(
                    MaterialTheme.wireDimensions.removeDeviceItemPadding - it.calculateTopPadding(),
                    MaterialTheme.wireDimensions.removeDeviceItemPadding - it.calculateEndPadding(LocalLayoutDirection.current)
                )
            }
        if (!placeholder && onRemoveDeviceClick != null)
            WireSecondaryButton(
                modifier = Modifier
                    .padding(top = buttonTopPadding, end = buttonEndPadding)
                    .testTag("remove device button"),
                onClick = { onRemoveDeviceClick(device) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_remove),
                        contentDescription = stringResource(R.string.content_description_remove_devices_screen_remove_icon),
                    )
                },
                fillMaxWidth = false,
                minHeight = MaterialTheme.wireDimensions.buttonSmallMinSize.height,
                minWidth = MaterialTheme.wireDimensions.buttonSmallMinSize.width,
                shape = RoundedCornerShape(size = MaterialTheme.wireDimensions.buttonSmallCornerSize),
                contentPadding = PaddingValues(0.dp),
            )
    }
}

@Composable
private fun DeviceItemTexts(device: Device, placeholder: Boolean, isDebug: Boolean = BuildConfig.DEBUG) {
    val displayZombieIndicator = remember {
        if (isDebug) {
            !device.isValid
        } else {
            false
        }
    }

    Text(
        style = MaterialTheme.wireTypography.body02,
        color = MaterialTheme.wireColorScheme.onBackground,
        text = device.name,
        modifier = Modifier
            .fillMaxWidth()
            .shimmerPlaceholder(visible = placeholder)
    )

    if (displayZombieIndicator) {
        Text(
            style = MaterialTheme.wireTypography.body02,
            color = MaterialTheme.wireColorScheme.onBackground,
            text = "this client is invalid",
            modifier = Modifier
                .fillMaxWidth()
                .shimmerPlaceholder(visible = placeholder)
        )
    }

    Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.removeDeviceItemTitleVerticalPadding))
    val details: String = if (device.registrationTime.isNotBlank()) {
        stringResource(
            R.string.remove_device_id_and_time_label,
            device.clientId.formatAsString(),
            device.registrationTime.formatMediumDateTime() ?: ""
        )
    } else {
        stringResource(
            R.string.remove_device_id_label,
            device.clientId.formatAsString()
        )
    }
    Text(
        style = MaterialTheme.wireTypography.subline01,
        color = MaterialTheme.wireColorScheme.labelText,
        text = details,
        modifier = Modifier
            .fillMaxWidth()
            .shimmerPlaceholder(visible = placeholder)
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewDeviceItem() {
    Box(modifier = Modifier.fillMaxWidth()) {
        DeviceItem(
            device = Device(),
            placeholder = false,
            background = null,
            {}
        )
    }
}
