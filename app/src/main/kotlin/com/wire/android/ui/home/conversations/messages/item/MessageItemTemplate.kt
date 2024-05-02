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
package com.wire.android.ui.home.conversations.messages.item

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.common.dimensions

@Composable
fun MessageItemTemplate(
    showAuthor: Boolean = true,
    useSmallBottomPadding: Boolean = false,
    fullAvatarOuterPadding: Dp,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(
                end = dimensions().messageItemHorizontalPadding,
                top = if (showAuthor) dimensions().spacing0x else dimensions().spacing4x,
                bottom = if (useSmallBottomPadding) dimensions().spacing2x else dimensions().messageItemBottomPadding
            )
    ) {
        Box(Modifier.width(dimensions().spacing56x), contentAlignment = Alignment.TopEnd) {
            leading()
        }
        Spacer(Modifier.width(dimensions().messageItemHorizontalPadding - fullAvatarOuterPadding))
        Box(Modifier.weight(1F)) {
            content()
        }
    }
}
