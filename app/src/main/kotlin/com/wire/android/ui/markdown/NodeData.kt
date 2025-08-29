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

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.wire.android.ui.home.conversations.messages.item.MessageStyle
import com.wire.android.ui.theme.WireColorScheme
import com.wire.android.ui.theme.WireTypography
import com.wire.kalium.logic.data.user.UserId

data class NodeData(
    val modifier: Modifier = Modifier,
    val color: Color = Color.Unspecified,
    val style: TextStyle,
    val colorScheme: WireColorScheme,
    val typography: WireTypography,
    val mentions: List<DisplayMention>,
    val searchQuery: String,
    val disableLinks: Boolean = false,
    val actions: NodeActions? = null,
    val messageStyle: MessageStyle = MessageStyle.NORMAL
)

data class NodeActions(
    val onLongClick: (() -> Unit)? = null,
    val onOpenProfile: (String) -> Unit,
    val onLinkClick: (String) -> Unit
)

data class DisplayMention(
    val userId: UserId,
    val length: Int,
    val isSelfMention: Boolean,
    val mentionUserName: String
)
