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
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageReactionsItem(
    messageFooter: MessageFooter,
    onReactionClicked: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // to eliminate adding unnecessary paddings when the list is empty
    if (messageFooter.reactions.entries.isNotEmpty()) {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing6x, Alignment.Top),
        ) {
            messageFooter.reactions.entries
                .sortedBy { it.key }
                .forEach {
                    val reaction = it.key
                    val count = it.value
                    ReactionPill(
                        emoji = reaction,
                        count = count,
                        isOwn = messageFooter.ownReactions.contains(reaction),
                        onTap = {
                            onReactionClicked(messageFooter.messageId, reaction)
                        },
                    )
                }
        }
    }
}

@PreviewMultipleThemes
@Composable
fun LongMessageReactionsItemPreview() = WireTheme {
    Box(modifier = Modifier.width(200.dp)) {
        MessageReactionsItem(
            messageFooter = MessageFooter(
                messageId = "messageId",
                reactions = mapOf(
                    "👍" to 1,
                    "👎" to 2,
                    "👏" to 3,
                    "🤔" to 4,
                    "🤷" to 5,
                    "🤦" to 6,
                    "🤢" to 7
                ),
                ownReactions = setOf("👍"),
            ),
            onReactionClicked = { _, _ -> }
        )
    }
}
