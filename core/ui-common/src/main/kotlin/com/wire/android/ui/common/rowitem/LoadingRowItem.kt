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
package com.wire.android.ui.common.rowitem

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.wire.android.model.Clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.shimmerPlaceholder
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.PreviewMultipleThemes

@Composable
fun LoadingRowItem(modifier: Modifier = Modifier) {
    RowItemTemplate(
        modifier = modifier.padding(start = dimensions().spacing8x),
        leadingIcon = {
            Box(
                modifier = Modifier
                    .padding(dimensions().avatarClickablePadding)
                    .clip(CircleShape)
                    .shimmerPlaceholder(visible = true)
                    .border(dimensions().avatarBorderWidth, colorsScheme().outline)
                    .size(dimensions().avatarDefaultSize)
            )
        },
        title = {
            Box(
                modifier = Modifier
                    .height(dimensions().spacing16x)
                    .padding(vertical = dimensions().spacing1x)
                    .shimmerPlaceholder(visible = true)
                    .fillMaxWidth(0.75f)
            )
        },
        subtitle = {
            Box(
                modifier = Modifier
                    .padding(top = dimensions().spacing8x)
                    .shimmerPlaceholder(visible = true)
                    .fillMaxWidth(0.5f)
                    .height(dimensions().spacing6x)
            )
        },
        clickable = remember { Clickable(false) },
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewLoadingRowItem() = WireTheme {
    LoadingRowItem()
}
