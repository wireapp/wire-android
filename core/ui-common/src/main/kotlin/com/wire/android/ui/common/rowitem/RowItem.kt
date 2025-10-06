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

package com.wire.android.ui.common.rowitem

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.wire.android.model.Clickable
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions

@Composable
fun RowItem(
    clickable: Clickable,
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    backgroundColor: Color = colorsScheme().surface,
    divider: @Composable () -> Unit = { RowItemDivider() },
    content: @Composable (RowScope.() -> Unit),
) {
    SurfaceBackgroundWrapper(backgroundColor = backgroundColor) {
        Column {
            Row(
                verticalAlignment = verticalAlignment,
                modifier = Modifier
                    .clickable(clickable)
                    .then(
                        modifier
                            .defaultMinSize(minHeight = MaterialTheme.wireDimensions.conversationItemRowHeight)
                            .fillMaxWidth()
                    )
            ) {
                content()
            }
            divider()
        }
    }
}

@SuppressLint("ComposeModifierMissing")
@Composable
fun RowItemDivider(
    color: Color = MaterialTheme.wireColorScheme.divider,
    height: Dp = MaterialTheme.wireDimensions.dividerThickness,
    startPadding: Dp = MaterialTheme.wireDimensions.spacing0x
) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding)
            .height(height = height)
            .background(color)
    )
}
