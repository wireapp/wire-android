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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.wire.android.model.Clickable
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.DEFAULT_WEIGHT

@Composable
fun RowItemTemplate(
    modifier: Modifier = Modifier,
    leadingIcon: @Composable () -> Unit = {},
    title: @Composable () -> Unit = {},
    titleStartPadding: Dp = dimensions().spacing8x,
    subtitle: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    wrapTitleContentWidth: Boolean = false,
    clickable: Clickable = Clickable(false) {}
) {
    RowItem(
        clickable = clickable,
        modifier = modifier
    ) {
        leadingIcon()
        Column(
            modifier = Modifier
                .padding(start = titleStartPadding)
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
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .padding(horizontal = dimensions().spacing8x)
        ) {
            actions()
        }
    }
}

@Composable
fun RowItemTemplate(
    leadingIcon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    clickable: Clickable,
    modifier: Modifier = Modifier,
    subTitle: @Composable () -> Unit = {},
    trailingIcon: @Composable () -> Unit = { }
) {
    RowItem(
        clickable = clickable,
        modifier = modifier
    ) {
        leadingIcon()
        Column(
            modifier = Modifier
                .weight(DEFAULT_WEIGHT),
        ) {
            title()
            subTitle()
        }
        trailingIcon()
    }
}
