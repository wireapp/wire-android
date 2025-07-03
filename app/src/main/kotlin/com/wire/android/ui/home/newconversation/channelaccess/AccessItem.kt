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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun AccessScreenItem(
    channelAccessType: ChannelAccessType,
    selectedAccess: ChannelAccessType,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    onItemClicked: (ChannelAccessType) -> Unit
) {
    val isSelected = channelAccessType == selectedAccess
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = dimensions().spacing1x)
            .background(color = MaterialTheme.wireColorScheme.surface)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Removes the ripple effect
                onClick = {
                    if (isEnabled) {
                        onItemClicked(channelAccessType)
                    }
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = { onItemClicked(channelAccessType) },
            enabled = isEnabled
        )
        Text(
            text = stringResource(channelAccessType.labelResId),
            style = MaterialTheme.wireTypography.body01,
            color = if (isEnabled) colorsScheme().onBackground else Color.Gray,
            modifier = Modifier.padding(vertical = dimensions().spacing16x)
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewAccessScreenItem() = WireTheme {
    AccessScreenItem(
        channelAccessType = ChannelAccessType.PRIVATE,
        selectedAccess = ChannelAccessType.PRIVATE,
        onItemClicked = {}
    )
}
