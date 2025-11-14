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
package com.wire.android.ui.home.conversations.messages.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.messages.ReactionPill
import com.wire.android.ui.home.conversations.model.MessageFooter
import com.wire.android.ui.home.conversations.model.Reaction
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

private const val BUBBLE_MAX_REACTIONS_IN_ROW = 4

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageReactionsItem(
    messageFooter: MessageFooter,
    messageStyle: MessageStyle,
    onReactionClicked: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    itemsAlignment: Alignment.Horizontal = Alignment.Start,
    onLongClick: (() -> Unit)? = null,
) {
    // to eliminate adding unnecessary paddings when the list is empty
    if (messageFooter.reactionMap.isNotEmpty()) {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x, itemsAlignment),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing6x, Alignment.Top),
            maxItemsInEachRow = if (messageStyle.isBubble()) BUBBLE_MAX_REACTIONS_IN_ROW else Int.MAX_VALUE,
        ) {
            messageFooter.reactionMap
                .toSortedMap()
                .forEach { (emoji, reaction) ->
                    ReactionPill(
                        emoji = emoji,
                        count = reaction.count,
                        isOwn = reaction.isSelf,
                        onTap = {
                            onReactionClicked(messageFooter.messageId, emoji)
                        },
                        onLongClick = onLongClick
                    )
                }
        }
    }
}

@PreviewMultipleThemes
@Composable
fun LongMessageReactionsItemPreview() = WireTheme(accent = Accent.Green) {
    Box(modifier = Modifier.width(300.dp)) {
        MessageReactionsItem(
            messageStyle = MessageStyle.NORMAL,
            messageFooter = MessageFooter(
                messageId = "messageId",
                reactionMap = mapOf(
                    "👍" to Reaction(1, isSelf = false),
                    "👎" to Reaction(2, isSelf = false),
                    "👏" to Reaction(3, isSelf = false),
                    "🤔" to Reaction(4, isSelf = false),
                    "🤷" to Reaction(5, isSelf = false),
                    "🤦" to Reaction(6, isSelf = false),
                    "🤢" to Reaction(7, isSelf = true),
                ),
            ),
            onReactionClicked = { _, _ -> }
        )
    }
}

@PreviewMultipleThemes
@Composable
fun LongMessageReactionsBubbleItemPreview() = WireTheme(accent = Accent.Petrol) {
    Box(modifier = Modifier.width(300.dp)) {
        MessageReactionsItem(
            messageStyle = MessageStyle.BUBBLE_OTHER,
            messageFooter = MessageFooter(
                messageId = "messageId",
                reactionMap = mapOf(
                    "👍" to Reaction(1, isSelf = false),
                    "👎" to Reaction(2, isSelf = false),
                    "👏" to Reaction(3, isSelf = false),
                    "🤔" to Reaction(4, isSelf = false),
                    "🤷" to Reaction(5, isSelf = false),
                    "🤦" to Reaction(6, isSelf = false),
                    "🤢" to Reaction(7, isSelf = true),
                ),
            ),
            onReactionClicked = { _, _ -> }
        )
    }
}
