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

package com.wire.android.ui.authentication.devices.remove

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun RemoveDeviceTopBar(elevation: Dp, onBackButtonClicked: () -> Unit) {
    WireCenterAlignedTopAppBar(
        elevation = elevation,
        title = stringResource(R.string.remove_device_title),
        navigationIconType = NavigationIconType.Close,
        onNavigationPressed = onBackButtonClicked
    ) {
        Text(
            text = stringResource(id = R.string.remove_device_message),
            style = MaterialTheme.wireTypography.body01,
            color = MaterialTheme.wireColorScheme.onBackground,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.wireDimensions.removeDeviceHorizontalPadding,
                vertical = MaterialTheme.wireDimensions.removeDeviceMessageVerticalPadding
            )
        )
        Text(
            text = stringResource(id = R.string.remove_device_label),
            style = MaterialTheme.wireTypography.title03,
            color = MaterialTheme.wireColorScheme.labelText,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.wireDimensions.removeDeviceHorizontalPadding,
                vertical = MaterialTheme.wireDimensions.removeDeviceLabelVerticalPadding
            )
        )
    }
}

@Preview(showBackground = false)
@Composable
fun PreviewLoginTopBar() {
    RemoveDeviceTopBar(0.dp) {}
}
