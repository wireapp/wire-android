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

package com.wire.android.ui.authentication.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.model.lastActiveDescription
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.getMinTouchMargins
import com.wire.android.ui.common.button.wireSecondaryButtonColors
import com.wire.android.ui.common.shimmerPlaceholder
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.extension.formatAsFingerPrint
import com.wire.android.util.extension.formatAsString
import com.wire.android.util.formatMediumDateTime
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText

@Composable
fun DeviceItem(
    device: Device,
    placeholder: Boolean,
    shouldShowVerifyLabel: Boolean,
    background: Color? = null,
    leadingIcon: @Composable (() -> Unit),
    leadingIconBorder: Dp = 1.dp,
    isWholeItemClickable: Boolean = false,
    onRemoveDeviceClick: ((Device) -> Unit)? = null
) {
    DeviceItemContent(
        device = device,
        placeholder = placeholder,
        background = background,
        leadingIcon = leadingIcon,
        leadingIconBorder = leadingIconBorder,
        onRemoveDeviceClick = onRemoveDeviceClick,
        isWholeItemClickable = isWholeItemClickable,
        shouldShowVerifyLabel = shouldShowVerifyLabel
    )
}

@Composable
private fun DeviceItemContent(
    device: Device,
    placeholder: Boolean,
    background: Color? = null,
    leadingIcon: @Composable (() -> Unit),
    leadingIconBorder: Dp,
    onRemoveDeviceClick: ((Device) -> Unit)?,
    isWholeItemClickable: Boolean,
    shouldShowVerifyLabel: Boolean
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = (if (background != null) Modifier.background(color = background) else Modifier)
            .clickable(enabled = isWholeItemClickable) {
                if (isWholeItemClickable) {
                    onRemoveDeviceClick?.invoke(device)
                }
            }
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
                    .weight(1f)
            ) { DeviceItemTexts(device, placeholder, shouldShowVerifyLabel) }
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
        if (!placeholder && onRemoveDeviceClick != null) {
            WireSecondaryButton(
                modifier = Modifier.testTag("remove device button"),
                onClick = { onRemoveDeviceClick(device) },
                leadingIcon = leadingIcon,
                fillMaxWidth = false,
                minSize = MaterialTheme.wireDimensions.buttonSmallMinSize,
                minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
                shape = RoundedCornerShape(size = MaterialTheme.wireDimensions.buttonSmallCornerSize),
                contentPadding = PaddingValues(0.dp),
                borderWidth = leadingIconBorder,
                colors = wireSecondaryButtonColors().copy(
                    enabled = background ?: MaterialTheme.wireColorScheme.secondaryButtonEnabled
                )
            )
        }
    }
}

@Composable
private fun DeviceItemTexts(
    device: Device,
    placeholder: Boolean,
    shouldShowVerifyLabel: Boolean,
    isDebug: Boolean = BuildConfig.DEBUG
) {
    val displayZombieIndicator = remember {
        if (isDebug) {
            !device.isValid
        } else {
            false
        }
    }

    Row(Modifier.fillMaxWidth()) {
        Text(
            style = MaterialTheme.wireTypography.body02,
            color = MaterialTheme.wireColorScheme.onBackground,
            text = device.name.asString(),
            modifier = Modifier
                .wrapContentWidth()
                .shimmerPlaceholder(visible = placeholder)
        )
        if (shouldShowVerifyLabel) {
            Spacer(modifier = Modifier.width(MaterialTheme.wireDimensions.spacing8x))
            VerifyLabel(device.isVerified, Modifier.wrapContentWidth())
        }
    }

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

    device.mlsPublicKeys?.values?.firstOrNull()?.let { mlsThumbprint ->
        Text(
            style = MaterialTheme.wireTypography.subline01,
            color = MaterialTheme.wireColorScheme.labelText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            text = stringResource(
                R.string.remove_device_mls_thumbprint_label,
                mlsThumbprint.formatAsFingerPrint()
            ),
            modifier = Modifier
                .fillMaxWidth()
                .shimmerPlaceholder(visible = placeholder)
        )
    }

    val proteusDetails: String = if (!device.registrationTime.isNullOrBlank()) {
        if (device.lastActiveInWholeWeeks != null) {
            stringResource(
                R.string.remove_device_id_and_time_label_active_label,
                device.clientId.formatAsString(),
                device.registrationTime.formatMediumDateTime() ?: "",
                device.lastActiveDescription() ?: ""
            )
        } else {
            stringResource(
                R.string.remove_device_id_and_time_label,
                device.clientId.formatAsString(),
                device.registrationTime.formatMediumDateTime() ?: ""
            )
        }
    } else {
        stringResource(
            R.string.remove_device_id_label,
            device.clientId.formatAsString()
        )
    }
    Text(
        style = MaterialTheme.wireTypography.subline01,
        color = MaterialTheme.wireColorScheme.labelText,
        text = proteusDetails,
        modifier = Modifier
            .fillMaxWidth()
            .shimmerPlaceholder(visible = placeholder)
    )
}

@Composable
fun VerifyLabel(isVerified: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.border(
            width = MaterialTheme.wireDimensions.spacing1x,
            shape = RoundedCornerShape(MaterialTheme.wireDimensions.spacing4x),
            color = if (isVerified) MaterialTheme.wireColorScheme.primary else MaterialTheme.wireColorScheme.secondaryText,
        )
    ) {
        Text(
            text = stringResource(id = if (isVerified) R.string.label_client_verified else R.string.label_client_unverified),
            color = if (isVerified) MaterialTheme.wireColorScheme.primary else MaterialTheme.wireColorScheme.secondaryText,
            style = MaterialTheme.wireTypography.label03,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .wrapContentWidth()
                .padding(horizontal = MaterialTheme.wireDimensions.spacing4x, vertical = MaterialTheme.wireDimensions.spacing2x)
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewDeviceItem() {
    WireTheme {
        DeviceItem(
            device = Device(name = UIText.DynamicString("name")),
            placeholder = false,
            shouldShowVerifyLabel = true,
            background = null,
            { Icon(painter = painterResource(id = R.drawable.ic_remove), contentDescription = "") }
        ) {}
    }
}
