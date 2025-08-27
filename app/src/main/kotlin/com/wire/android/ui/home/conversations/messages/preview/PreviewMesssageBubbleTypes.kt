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
package com.wire.android.ui.home.conversations.messages.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.messages.item.MessageClickActions
import com.wire.android.ui.home.conversations.messages.item.RegularMessageItem
import com.wire.android.ui.home.conversations.mock.mockFooterWithMultipleReactions
import com.wire.android.ui.home.conversations.mock.mockHeaderWithExpiration
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.home.conversations.mock.mockMessageWithTextContent
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.Message
import kotlinx.datetime.Instant
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@PreviewMultipleThemes
@Composable
fun PreviewBubbleSelfTextMessage() {
    WireTheme {
        RegularMessageItem(
            message = mockMessageWithText.copy(
                header = mockMessageWithText.header.copy(
                    username = UIText.DynamicString(
                        "Pablo Diego José Francisco de Paula Juan Nepomuceno María de los Remedios Cipriano de la Santísima Trinidad " +
                                "Ruiz y Picasso"
                    )
                )
            ),
            conversationDetailsData = ConversationDetailsData.None(null),
            clickActions = MessageClickActions.Content(),
            isBubbleUiEnabled = true
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewBubbleSelfOtherMessage() {
    WireTheme {
        Box(
            modifier = Modifier.background(Color.Gray)
        ) {
            RegularMessageItem(
                message = mockMessageWithText.copy(
                    header = mockMessageWithText.header.copy(
                        username = UIText.DynamicString(
                            "Pablo Diego José Francisco de Paula Juan Nepomuceno María de los Remedios Cipriano de la Santísima Trinidad " +
                                    "Ruiz y Picasso"
                        )
                    ),
                    source = MessageSource.OtherUser
                ),
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewBubbleSelfMultipleTextMessage() {
    WireTheme {
        Column(modifier = Modifier.background(Color.Gray)) {
            RegularMessageItem(
                message = mockMessageWithTextContent("Hi").copy(
                    header = mockMessageWithText.header.copy(
                        username = UIText.DynamicString(
                            "Paul Nagel"
                        )
                    )
                ),
                showAuthor = true,
                useSmallBottomPadding = true,
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true
            )
            RegularMessageItem(
                message = mockMessageWithTextContent("Middle message").copy(
                    header = mockMessageWithText.header.copy(
                        username = UIText.DynamicString(
                            "Paul Nagel"
                        )
                    )
                ),
                showAuthor = false,
                useSmallBottomPadding = true,
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true
            )
            RegularMessageItem(
                message = mockMessageWithTextContent("Last message").copy(
                    header = mockMessageWithText.header.copy(
                        username = UIText.DynamicString(
                            "Paul Nagel"
                        )
                    )
                ),
                showAuthor = false,
                useSmallBottomPadding = false,
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewBubbleOtherMultipleTextMessage() {
    WireTheme {
        Column(modifier = Modifier.background(Color.Gray)) {
            RegularMessageItem(
                message = mockMessageWithTextContent("Hi").copy(
                    source = MessageSource.OtherUser,
                    header = mockMessageWithText.header.copy(
                        username = UIText.DynamicString(
                            "Paul Nagel"
                        )
                    )
                ),
                showAuthor = true,
                useSmallBottomPadding = true,
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true
            )
            RegularMessageItem(
                message = mockMessageWithTextContent("Middle message").copy(
                    source = MessageSource.OtherUser,
                    header = mockMessageWithText.header.copy(
                        username = UIText.DynamicString(
                            "Paul Nagel"
                        )
                    )
                ),
                showAuthor = false,
                useSmallBottomPadding = true,
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true
            )
            RegularMessageItem(
                message = mockMessageWithTextContent("Last message").copy(
                    source = MessageSource.OtherUser,
                    header = mockMessageWithText.header.copy(
                        username = UIText.DynamicString(
                            "Paul Nagel"
                        )
                    )
                ),
                showAuthor = false,
                useSmallBottomPadding = false,
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewBubbleMultipleTextMessagesWithReactions() {
    WireTheme {
        Column(modifier = Modifier.background(Color.Gray)) {
            RegularMessageItem(
                message = mockMessageWithTextContent("Hello").copy(
                    header = mockMessageWithText.header.copy(
                        username = UIText.DynamicString(
                            "Paul Nagel"
                        )
                    ),
                    messageFooter = mockFooterWithMultipleReactions,
                ),
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true,
            )
            RegularMessageItem(
                message = mockMessageWithTextContent("Hello").copy(
                    source = MessageSource.OtherUser,
                    header = mockMessageWithText.header.copy(
                        username = UIText.DynamicString(
                            "Paul Nagel"
                        )
                    ),
                    messageFooter = mockFooterWithMultipleReactions,
                ),
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true,
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewBubbleMultipleDeletedMessages() {
    WireTheme {
        Column(modifier = Modifier.background(Color.Gray)) {
            RegularMessageItem(
                message = mockMessageWithTextContent("Hello").copy(
                    header = mockHeaderWithExpiration(
                        ExpirationStatus.Expirable(
                            expireAfter = 1.toDuration(DurationUnit.MINUTES),
                            selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.Started(Instant.DISTANT_FUTURE),
                        ),
                        isDeleted = true
                    ).copy(
                        username = UIText.DynamicString(
                            "Paul Nagel"
                        )
                    ),
                ),
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true,
            )
            RegularMessageItem(
                message = mockMessageWithTextContent("Hello").copy(
                    source = MessageSource.OtherUser,
                    header = mockHeaderWithExpiration(
                        ExpirationStatus.Expirable(
                            expireAfter = 1.toDuration(DurationUnit.MINUTES),
                            selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.Started(Instant.DISTANT_FUTURE),
                        ),
                        isDeleted = true
                    ).copy(
                        username = UIText.DynamicString(
                            "Paul Nagel"
                        )
                    ),
                ),
                conversationDetailsData = ConversationDetailsData.None(null),
                clickActions = MessageClickActions.Content(),
                isBubbleUiEnabled = true,
            )
        }
    }
}
