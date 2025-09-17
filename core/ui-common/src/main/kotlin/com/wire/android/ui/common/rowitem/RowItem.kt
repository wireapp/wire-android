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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wire.android.model.Clickable
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.common.clickable
import com.wire.android.ui.theme.wireDimensions

@Composable
fun RowItem(
    clickable: Clickable,
    modifier: Modifier = Modifier,
    content: @Composable (RowScope.() -> Unit),
) {
    SurfaceBackgroundWrapper(
        modifier = Modifier
            .padding(vertical = MaterialTheme.wireDimensions.conversationItemPadding)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
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
    }
}
