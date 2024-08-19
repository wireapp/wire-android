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

package com.wire.android.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun AttachmentButton(@DrawableRes icon: Int, labelStyle: TextStyle, modifier: Modifier = Modifier, text: String = "", onClick: () -> Unit) {
    Column(
        modifier = modifier
            .padding(dimensions().spacing4x)
            .clip(RoundedCornerShape(size = MaterialTheme.wireDimensions.buttonSmallCornerSize))
            .clickable { onClick() }
            .padding(dimensions().spacing8x),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(dimensions().attachmentButtonSize)
                .background(MaterialTheme.wireColorScheme.primaryButtonEnabled, CircleShape)
                .padding(dimensions().spacing2x)
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                contentScale = ContentScale.Inside,
                modifier = Modifier
                    .padding(dimensions().spacing8x)
                    .align(Alignment.Center),
                colorFilter = ColorFilter.tint(MaterialTheme.wireColorScheme.onPrimaryButtonEnabled)
            )
        }
        VerticalSpace.x4()
        Spacer(modifier = Modifier.weight(1F))
        Text(
            text = text,
            maxLines = 2,
            textAlign = TextAlign.Center,
            style = labelStyle,
            color = MaterialTheme.wireColorScheme.onBackground,
        )
        Spacer(modifier = Modifier.weight(1F))
    }
}

// This composable has not fixed height to adapt to GridView in [AttachmentOptionsComponent]
@Preview(showBackground = true)
@Composable
fun PreviewAttachmentButton() {
    AttachmentButton(
        icon = R.drawable.ic_location,
        labelStyle = MaterialTheme.wireTypography.button03,
        modifier = Modifier,
        text = "Share Location"
    ) { }
}
