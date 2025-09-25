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
package com.wire.android.ui.home.conversations.model.messagetypes.multipart.standalone

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.applyIf
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun AssetNotAvailablePreview(
    messageStyle: MessageStyle,
    modifier: Modifier = Modifier,
    accent: Accent = Accent.Unknown
) {
    Column(
        modifier = modifier
            .applyIf(messageStyle == MessageStyle.BUBBLE_SELF) {
                background(
                    colorsScheme().bubbleContainerAccentBackgroundColor.getOrDefault(
                        accent,
                        colorsScheme().defaultBubbleContainerBackgroundColor
                    )
                )
            }
            .applyIf(messageStyle == MessageStyle.BUBBLE_OTHER) {
                background(
                    colorsScheme().surface
                )
            }
            .fillMaxSize()
            .height(dimensions().spacing72x)
            .padding(dimensions().spacing8x),
    ) {
        val iconColorColor = when (messageStyle) {
            MessageStyle.BUBBLE_SELF -> colorsScheme().inverseOnSurface
            MessageStyle.BUBBLE_OTHER -> colorsScheme().inverseSurface
            MessageStyle.NORMAL -> colorsScheme().secondaryText
        }

        Image(
            modifier = Modifier.size(dimensions().spacing16x),
            painter = painterResource(R.drawable.ic_file_not_available),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconColorColor)
        )
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        )

        val notAvailableTextColor = when (messageStyle) {
            MessageStyle.BUBBLE_SELF -> colorsScheme().surface
            MessageStyle.BUBBLE_OTHER -> colorsScheme().inverseSurface
            MessageStyle.NORMAL -> colorsScheme().secondaryText
        }

        Text(
            text = stringResource(R.string.asset_message_failed_download_text),
            style = typography().body02,
            color = notAvailableTextColor
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewAssetNotAvailablePreview() {
    WireTheme {
        AssetNotAvailablePreview(messageStyle = MessageStyle.BUBBLE_SELF)
    }
}
