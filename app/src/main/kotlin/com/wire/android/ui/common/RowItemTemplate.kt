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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.wire.android.model.Clickable
import com.wire.android.ui.home.conversationslist.common.RowItem
import com.wire.android.ui.theme.DEFAULT_WEIGHT

@Composable
fun RowItemTemplate(
    leadingIcon: @Composable () -> Unit = {},
    title: @Composable () -> Unit = {},
    titleStartPadding: Dp = dimensions().spacing8x,
    subtitle: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    clickable: Clickable = Clickable(false) {},
    modifier: Modifier = Modifier
) {
    RowItem(
        clickable = clickable,
        modifier = modifier
    ) {
        leadingIcon()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = titleStartPadding)
        ) {
            title()
            subtitle()
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
    subTitle: @Composable () -> Unit = {},
    clickable: Clickable,
    trailingIcon: @Composable () -> Unit = { },
    modifier: Modifier = Modifier
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
