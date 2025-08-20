 /*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.newconversation.channelaccess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.selectableBackground
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun PermissionItem(
    channelAddPermissionType: ChannelAddPermissionType,
    selectedPermission: ChannelAddPermissionType,
    modifier: Modifier = Modifier,
    onItemClicked: (ChannelAddPermissionType) -> Unit
) {
    val isSelected = channelAddPermissionType == selectedPermission

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = dimensions().spacing1x)
            .selectableBackground(isSelected, onClick = { onItemClicked(channelAddPermissionType) })
            .background(color = MaterialTheme.wireColorScheme.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = { onItemClicked(channelAddPermissionType) })
        Text(
            text = stringResource(channelAddPermissionType.labelResId),
            style = MaterialTheme.wireTypography.body01,
            color = MaterialTheme.wireColorScheme.onBackground,
            modifier = Modifier.padding(vertical = dimensions().spacing16x)
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewPermissionItem() = WireTheme {
    PermissionItem(
        channelAddPermissionType = ChannelAddPermissionType.ADMINS,
        selectedPermission = ChannelAddPermissionType.ADMINS,
        onItemClicked = {}
    )
}
