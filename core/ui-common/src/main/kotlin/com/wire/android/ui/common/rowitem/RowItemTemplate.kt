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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.wire.android.model.Clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions

@Composable
fun RowItemTemplate(
    modifier: Modifier = Modifier,
    leadingIcon: @Composable () -> Unit = {},
    title: @Composable () -> Unit = {},
    titleStartPadding: Dp = dimensions().spacing8x,
    contentTopPadding: Dp = dimensions().spacing8x,
    contentBottomPadding: Dp = dimensions().spacing8x,
    actionsEndPadding: Dp = dimensions().spacing8x,
    subtitle: @Composable () -> Unit = {},
    actions: @Composable (() -> Unit)? = null,
    wrapTitleContentWidth: Boolean = false,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    backgroundColor: Color = colorsScheme().surface,
    divider: @Composable () -> Unit = { RowItemDivider() },
    clickable: Clickable = Clickable(false) {}
) {
    RowItem(
        clickable = clickable,
        modifier = modifier,
        verticalAlignment = verticalAlignment,
        divider = divider,
        backgroundColor = backgroundColor,
    ) {
        leadingIcon()
        Column(
            modifier = Modifier
                .padding(start = titleStartPadding, top = contentTopPadding, bottom = contentBottomPadding)
                .then(
                    if (wrapTitleContentWidth) Modifier.wrapContentWidth() else Modifier.weight(1f)
                )
        ) {
            title()
            subtitle()
        }
        if (wrapTitleContentWidth) {
            // Add a spacer to push the actions to the end of the row when weight is not set
            Spacer(modifier = Modifier.weight(1f))
        }
        actions?.let {
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(start = dimensions().spacing8x, end = actionsEndPadding)
            ) {
                actions()
            }
        }
    }
}
