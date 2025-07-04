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

package com.wire.android.ui.userprofile.other

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.R
import com.wire.android.ui.authentication.devices.DeviceItem
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.capitalizeFirstLetter
import com.wire.android.util.ui.LinkText
import com.wire.android.util.ui.LinkTextData

@Composable
fun OtherUserDevicesScreen(
    state: OtherUserProfileState,
    lazyListState: LazyListState = rememberLazyListState(),
    onDeviceClick: (Device) -> Unit
) {
    AnimatedContent(state.otherUserDevices) { devices ->
        when {
            devices == null -> OtherUserDevicesProgress()
            devices.isEmpty() -> OtherUserEmptyDevicesContent()
            else -> OtherUserDevicesContent(
                fullName = state.fullName,
                devices = devices,
                shouldShowE2EIInfo = state.isE2EIEnabled,
                lazyListState = lazyListState,
                onDeviceClick = onDeviceClick
            )
        }
    }
}

@Composable
private fun OtherUserDevicesProgress() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = dimensions().spacing56x),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top
    ) {
        WireCircularProgressIndicator(progressColor = colorsScheme().primary)
    }
}

@Composable
private fun OtherUserEmptyDevicesContent() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = dimensions().spacing56x),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = stringResource(id = R.string.label_client_key_fingerprint_not_available).capitalizeFirstLetter(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.wireTypography.body01.copy(color = MaterialTheme.wireColorScheme.secondaryText)
        )
    }
}

@Composable
private fun OtherUserDevicesContent(
    fullName: String,
    devices: List<Device>,
    shouldShowE2EIInfo: Boolean,
    lazyListState: LazyListState = rememberLazyListState(),
    onDeviceClick: (Device) -> Unit
) {
    val context = LocalContext.current
    val supportUrl = stringResource(id = R.string.url_why_verify_conversation)

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.wireColorScheme.surface)
    ) {
        item {
            LinkText(
                linkTextData = listOf(
                    LinkTextData(
                        text = stringResource(R.string.other_user_devices_description, fullName)
                    ),
                    LinkTextData(
                        text = stringResource(id = R.string.label_learn_more),
                        tag = "learn_more",
                        annotation = supportUrl,
                        onClick = { CustomTabsHelper.launchUrl(context, supportUrl) }
                    )
                ),
                modifier = Modifier.padding(all = dimensions().spacing16x),
                textColor = colorsScheme().onSurface
            )
        }

        itemsIndexed(devices) { index, item ->
            DeviceItem(
                device = item,
                placeholder = false,
                modifier = Modifier.background(MaterialTheme.wireColorScheme.surface),
                isWholeItemClickable = true,
                onClickAction = onDeviceClick,
                icon = { ArrowRightIcon(contentDescription = R.string.content_description_empty) },
                shouldShowVerifyLabel = true,
                shouldShowE2EIInfo = shouldShowE2EIInfo,
            )
            if (index < devices.lastIndex) WireDivider()
        }
    }
}
