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
package com.wire.android.ui.markdown

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun MarkdownThematicBreak(messageStyle: MessageStyle, modifier: Modifier = Modifier) {
    val color = when (messageStyle) {
        MessageStyle.BUBBLE_SELF -> colorsScheme().selfBubble.onPrimary
        MessageStyle.BUBBLE_OTHER -> colorsScheme().otherBubble.onPrimary
        MessageStyle.NORMAL -> MaterialTheme.wireColorScheme.outline
    }
    WireDivider(modifier = modifier.padding(vertical = dimensions().spacing8x), color = color)
}
