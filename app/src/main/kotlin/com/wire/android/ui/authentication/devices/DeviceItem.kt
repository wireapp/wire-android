/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.ui.authentication.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.model.lastActiveDescription
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.MLSVerificationIcon
import com.wire.android.ui.common.ProteusVerifiedIcon
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.wireSecondaryButtonColors
import com.wire.android.ui.common.shimmerPlaceholder
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.deviceDateTimeFormat
import com.wire.android.util.extension.formatAsFingerPrint
import com.wire.android.util.extension.formatAsString
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText

const val DEVICE_ITEM_TEST_TAG = "device_item"

@Composable
fun DeviceItem(
    device: Device,
    placeholder: Boolean,
    shouldShowVerifyLabel: Boolean,
    icon: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    isCurrentClient: Boolean = false,
    shouldShowE2EIInfo: Boolean = false,
    isWholeItemClickable: Boolean = false,
    onClickAction: ((Device) -> Unit)? = null
) {
    DeviceItemContent(
        device = device,
        placeholder = placeholder,
        icon = icon,
        onClickAction = onClickAction,
        isWholeItemClickable = isWholeItemClickable,
        shouldShowVerifyLabel = shouldShowVerifyLabel,
        isCurrentClient = isCurrentClient,
        shouldShowE2EIInfo = shouldShowE2EIInfo,
        modifier = modifier,
    )
}

@Composable
private fun DeviceItemContent(
    device: Device,
    placeholder: Boolean,
    icon: @Composable (() -> Unit),
    onClickAction: ((Device) -> Unit)?,
    isWholeItemClickable: Boolean,
    shouldShowVerifyLabel: Boolean,
    isCurrentClient: Boolean,
    shouldShowE2EIInfo: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .testTag(DEVICE_ITEM_TEST_TAG)
            .clickable(
                enabled = isWholeItemClickable,
                onClickLabel = stringResource(id = R.string.content_description_user_profile_open_device_btn)
            ) {
                if (isWholeItemClickable) {
                    onClickAction?.invoke(device)
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
                contentDescription = null
            )
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .padding(start = MaterialTheme.wireDimensions.removeDeviceItemPadding)
                    .weight(1f)
            ) {
                DeviceItemTexts(device, placeholder, shouldShowVerifyLabel, isCurrentClient, shouldShowE2EIInfo)
            }
        }
        if (!placeholder) {
            if (onClickAction != null && !isWholeItemClickable) {
                WireSecondaryButton(
                    modifier = Modifier
                        .padding(end = MaterialTheme.wireDimensions.removeDeviceItemPadding)
                        .testTag("remove device button"),
                    onClick = { onClickAction(device) },
                    leadingIcon = icon,
                    fillMaxWidth = false,
                    minSize = MaterialTheme.wireDimensions.buttonSmallMinSize,
                    minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
                    shape = RoundedCornerShape(size = MaterialTheme.wireDimensions.buttonSmallCornerSize),
                    contentPadding = PaddingValues(0.dp),
                    colors = wireSecondaryButtonColors().copy(
                        enabled = MaterialTheme.wireColorScheme.secondaryButtonEnabled
                    )
                )
            } else {
                Box(
                    modifier = Modifier
                        .padding(
                            top = MaterialTheme.wireDimensions.removeDeviceItemPadding,
                            end = MaterialTheme.wireDimensions.removeDeviceItemPadding,
                        )
                ) {
                    icon()
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.DeviceItemTexts(
    device: Device,
    placeholder: Boolean,
    shouldShowVerifyLabel: Boolean,
    isCurrentClient: Boolean = false,
    shouldShowE2EIInfo: Boolean = false,
    isDebug: Boolean = BuildConfig.DEBUG,
) {
    val displayZombieIndicator = remember {
        if (isDebug) {
            !device.isValid
        } else {
            false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shimmerPlaceholder(visible = placeholder)
    ) {
        val deviceName = device.name.asString()
        val shouldAddNotVerifiedLabel = shouldShowVerifyLabel && !shouldShowE2EIInfo && !(device.isVerifiedProteus && !isCurrentClient)
        val semantic = if (shouldAddNotVerifiedLabel) {
            val notVerifiedLabel = stringResource(R.string.label_client_unverified)
            Modifier.clearAndSetSemantics { contentDescription = "$deviceName, $notVerifiedLabel" }
        } else {
            Modifier
        }
        Text(
            style = MaterialTheme.wireTypography.body02,
            color = MaterialTheme.wireColorScheme.onBackground,
            text = deviceName,
            modifier = Modifier
                .wrapContentWidth()
                .shimmerPlaceholder(visible = placeholder)
                .then(semantic)
        )
        if (shouldShowVerifyLabel) {
            if (shouldShowE2EIInfo && device.mlsClientIdentity != null) {
                MLSVerificationIcon(device.mlsClientIdentity.e2eiStatus)
            }
            if (device.isVerifiedProteus && !isCurrentClient) {
                ProteusVerifiedIcon(
                    Modifier
                        .wrapContentWidth()
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }

    if (displayZombieIndicator) {
        Text(
            style = MaterialTheme.wireTypography.body02,
            color = MaterialTheme.wireColorScheme.onBackground,
            text = "this client is invalid",
            modifier = Modifier
                .fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.removeDeviceItemTitleVerticalPadding))

    MLSDetails(device, placeholder)

    ProteusDetails(device, placeholder)
}

@Composable
private fun MLSDetails(
    device: Device,
    placeholder: Boolean
) {
    device.mlsClientIdentity?.let { identity ->
        Text(
            style = MaterialTheme.wireTypography.subline01,
            color = MaterialTheme.wireColorScheme.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            text = stringResource(
                R.string.remove_device_mls_thumbprint_label,
                identity.thumbprint.formatAsFingerPrint()
            ),
            modifier = Modifier
                .fillMaxWidth()
                .shimmerPlaceholder(visible = placeholder)
        )
    }
}

@Composable
private fun ProteusDetails(
    device: Device,
    placeholder: Boolean
) {
    val proteusDetails: String = if (!device.registrationTime.isNullOrBlank()) {
        if (device.lastActiveInWholeWeeks != null) {
            stringResource(
                R.string.remove_device_id_and_time_label_active_label,
                device.clientId.formatAsString(),
                device.registrationTime.deviceDateTimeFormat() ?: "",
                device.lastActiveDescription() ?: ""
            )
        } else {
            stringResource(
                R.string.remove_device_id_and_time_label,
                device.clientId.formatAsString(),
                device.registrationTime.deviceDateTimeFormat() ?: ""
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
        color = MaterialTheme.wireColorScheme.secondaryText,
        text = proteusDetails,
        minLines = 2,
        modifier = Modifier
            .fillMaxWidth()
            .shimmerPlaceholder(visible = placeholder)
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewDeviceItemWithActionIcon() {
    WireTheme {
        DeviceItem(
            device = Device(name = UIText.DynamicString("Name"), isVerifiedProteus = true, registrationTime = "2024-01-01T12:00:00.000Z"),
            placeholder = false,
            shouldShowVerifyLabel = true,
            isCurrentClient = true,
            shouldShowE2EIInfo = true,
            icon = { Icon(painter = painterResource(id = R.drawable.ic_remove), contentDescription = "") }
        ) {}
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewDeviceItem() {
    WireTheme {
        DeviceItem(
            device = Device(name = UIText.DynamicString("Name"), isVerifiedProteus = true, registrationTime = "2024-01-01T12:00:00.000Z"),
            placeholder = false,
            shouldShowVerifyLabel = true,
            isWholeItemClickable = true,
            icon = { ArrowRightIcon() }
        ) {}
    }
}
