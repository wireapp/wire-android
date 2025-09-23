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
package com.wire.android.ui.home.conversations.model.messagetypes.location

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.applyIf
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.home.conversations.messages.item.isBubble
import com.wire.android.ui.home.conversations.messages.item.textColor
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun LocationMessageContent(
    locationName: String,
    locationUrl: String,
    onLocationClick: Clickable,
    messageStyle: MessageStyle,
    modifier: Modifier = Modifier
) {
    val linkColor = messageStyle.textColor()

    val textColor = when (messageStyle) {
        MessageStyle.BUBBLE_SELF -> colorsScheme().onPrimary
        MessageStyle.BUBBLE_OTHER -> colorsScheme().onBackground
        MessageStyle.NORMAL -> colorsScheme().onBackground
    }

    Column(
        modifier = modifier
            .clickable(onLocationClick)
            .applyIf(!messageStyle.isBubble()) {
                padding(top = dimensions().spacing4x)
                    .clip(shape = RoundedCornerShape(dimensions().messageAssetBorderRadius))
                    .border(
                        width = dimensions().spacing1x,
                        color = MaterialTheme.wireColorScheme.secondaryButtonDisabledOutline,
                        shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
                    )
                    .background(
                        color = MaterialTheme.wireColorScheme.surfaceVariant,
                        shape = RoundedCornerShape(dimensions().messageAssetBorderRadius)
                    )
            }
            .defaultMinSize(minHeight = dimensions().spacing64x),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Spacer(modifier = Modifier.height(dimensions().spacing12x))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(horizontal = dimensions().spacing8x)),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_location),
                contentDescription = stringResource(id = R.string.content_description_location_icon),
                tint = textColor,
                modifier = Modifier
                    .size(MaterialTheme.wireDimensions.wireIconButtonSize)
            )
            Spacer(modifier = Modifier.width(dimensions().spacing4x))
            Text(
                text = locationName,
                style = MaterialTheme.wireTypography.body02,
                fontSize = 15.sp,
                color = textColor,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = locationUrl,
            style = MaterialTheme.wireTypography.subline01.copy(color = linkColor),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = dimensions().spacing8x)
        )

        Spacer(modifier = Modifier.height(dimensions().spacing12x))
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewLocationMessageContent() {
    LocationMessageContent(
        locationName = "Rapa Nui, 2770000, CL",
        locationUrl = "https://www.google.com/maps/place/Rapa+Nui",
        onLocationClick = Clickable(),
        messageStyle = MessageStyle.NORMAL
    )
}
