/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.mapper

import com.wire.android.R
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.UnreadEventCount
import com.wire.kalium.logic.data.message.AssetType
import com.wire.kalium.logic.data.message.MessagePreview
import com.wire.kalium.logic.data.message.MessagePreviewContent
import com.wire.kalium.logic.data.message.MessagePreviewContent.Unknown
import com.wire.kalium.logic.data.message.MessagePreviewContent.WithUser
import com.wire.kalium.logic.data.message.UnreadEventType

@Suppress("ReturnCount")
fun MessagePreview?.toUIPreview(unreadEventCount: UnreadEventCount): UILastMessageContent {
    if (this == null) {
        return UILastMessageContent.None
    }

    val sortedUnreadContent = unreadEventCount
        .toSortedMap()

    // we want to show last message content instead of counter when there are only one type of unread events
    if (sortedUnreadContent.isNotEmpty()) {
        val unreadContentTexts = sortedUnreadContent
            .mapNotNull { type ->
                when (type.key) {
                    UnreadEventType.KNOCK -> UnreadEventType.KNOCK to UIText.PluralResource(
                        R.plurals.unread_event_knock,
                        type.value,
                        type.value
                    )

                    UnreadEventType.MISSED_CALL -> UnreadEventType.MISSED_CALL to UIText.PluralResource(
                        R.plurals.unread_event_call,
                        type.value,
                        type.value
                    )

                    UnreadEventType.MENTION -> UnreadEventType.MENTION to UIText.PluralResource(
                        R.plurals.unread_event_mention,
                        type.value,
                        type.value
                    )

                    UnreadEventType.REPLY -> UnreadEventType.REPLY to UIText.PluralResource(
                        R.plurals.unread_event_reply,
                        type.value,
                        type.value
                    )

                    UnreadEventType.MESSAGE -> UnreadEventType.MESSAGE to UIText.PluralResource(
                        R.plurals.unread_event_message,
                        type.value,
                        type.value
                    )

                    UnreadEventType.IGNORED -> null
                    null -> null
                }
            }.associate { it }
        if (unreadContentTexts.size > 1) {
            val first = unreadContentTexts.values.first()
            val second = unreadContentTexts.values.elementAt(1)
            return UILastMessageContent.MultipleMessage(listOf(first, second))
        } else if (unreadContentTexts.isNotEmpty()) {
            val unreadContent = unreadContentTexts.entries.first()
            if (unreadContent.key != UnreadEventType.MESSAGE) {
                return UILastMessageContent.TextMessage(MessageBody(unreadContent.value))
            }
        }
    }

    return uiLastMessageContent()
}

private fun String?.userUiText(isSelfMessage: Boolean): UIText = when {
    isSelfMessage -> UIText.StringResource(R.string.member_name_you_label_titlecase)
    this != null -> UIText.DynamicString(this)
    else -> UIText.StringResource(R.string.username_unavailable_label)
}

@Suppress("LongMethod", "ComplexMethod")
fun MessagePreview.uiLastMessageContent(): UILastMessageContent {
    return when (content) {
        is WithUser -> {
            val userContent = (content as WithUser)
            val userUIText = userContent.username.userUiText(isSelfMessage)
            when ((userContent)) {
                is WithUser.Asset -> when ((content as WithUser.Asset).type) {
                    AssetType.AUDIO ->
                        UILastMessageContent.SenderWithMessage(userUIText, UIText.StringResource(R.string.last_message_audio))

                    AssetType.IMAGE ->
                        UILastMessageContent.SenderWithMessage(userUIText, UIText.StringResource(R.string.last_message_image))

                    AssetType.VIDEO ->
                        UILastMessageContent.SenderWithMessage(userUIText, UIText.StringResource(R.string.last_message_video))

                    AssetType.ASSET ->
                        UILastMessageContent.SenderWithMessage(userUIText, UIText.StringResource(R.string.last_message_asset))

                    AssetType.FILE ->
                        UILastMessageContent.SenderWithMessage(userUIText, UIText.StringResource(R.string.last_message_file))
                }

                is WithUser.ConversationNameChange -> UILastMessageContent.SenderWithMessage(
                    userUIText,
                    UIText.StringResource(R.string.last_message_change_conversation_name)
                )

                is WithUser.Knock -> UILastMessageContent.SenderWithMessage(
                    userUIText,
                    UIText.StringResource(R.string.last_message_knock)
                )

                is WithUser.MemberJoined -> UILastMessageContent.SenderWithMessage(
                    userUIText,
                    UIText.StringResource(R.string.last_message_joined_conversation)
                )

                is WithUser.MemberLeft -> UILastMessageContent.SenderWithMessage(
                    userUIText,
                    UIText.StringResource(R.string.last_message_left_conversation)
                )

                is WithUser.MembersAdded -> {
                    val membersAddedContent = (content as WithUser.MembersAdded)
                    val isSelfAdded = membersAddedContent.isSelfUserAdded
                    val otherUsersSize = membersAddedContent.otherUserIdList.size

                    val previewMessageContent = when {
                        // This case would never be applicable. If self added self, this will be a MemberJoined
                        isSelfMessage && isSelfAdded -> {
                            UIText.StringResource(R.string.last_message_joined_conversation)
                        }

                        isSelfMessage && otherUsersSize > 0 -> {
                            UIText.PluralResource(R.plurals.last_message_self_added_users, otherUsersSize, otherUsersSize)
                        }

                        !isSelfMessage && isSelfAdded -> {
                            UIText.PluralResource(R.plurals.last_message_other_added_self_user, otherUsersSize, otherUsersSize)
                        }

                        else -> {
                            UIText.PluralResource(R.plurals.last_message_other_added_other_users, otherUsersSize, otherUsersSize)
                        }
                    }

                    UILastMessageContent.TextMessage(MessageBody(previewMessageContent))
                }

                is WithUser.MembersRemoved -> {
                    val membersRemovedContent = (content as WithUser.MembersRemoved)
                    val isSelfRemoved = membersRemovedContent.isSelfUserRemoved
                    val otherUsersSize = membersRemovedContent.otherUserIdList.size

                    val previewMessageContent = when {
                        // This case would never be applicable. If self added self, this will be a MemberLeft
                        isSelfMessage && isSelfRemoved -> {
                            UIText.StringResource(R.string.last_message_left_conversation)
                        }

                        isSelfMessage && otherUsersSize > 0 -> {
                            UIText.PluralResource(R.plurals.last_message_self_removed_users, otherUsersSize, otherUsersSize)
                        }

                        !isSelfMessage && isSelfRemoved -> {
                            UIText.PluralResource(R.plurals.last_message_other_removed_self_user, otherUsersSize, otherUsersSize)
                        }

                        else -> {
                            UIText.PluralResource(R.plurals.last_message_other_removed_other_users, otherUsersSize, otherUsersSize)
                        }
                    }

                    UILastMessageContent.TextMessage(MessageBody(previewMessageContent))
                }

                is WithUser.MentionedSelf -> UILastMessageContent.SenderWithMessage(
                    userUIText,
                    UIText.StringResource(R.string.last_message_mentioned)
                )

                is WithUser.QuotedSelf -> UILastMessageContent.SenderWithMessage(
                    userUIText,
                    UIText.StringResource(R.string.last_message_replied)
                )

                is WithUser.TeamMemberRemoved -> UILastMessageContent.None // TODO
                is WithUser.Text -> UILastMessageContent.SenderWithMessage(
                    sender = userUIText,
                    message = UIText.DynamicString((content as WithUser.Text).messageBody),
                    separator = ": "
                )

                is WithUser.MissedCall -> UILastMessageContent.TextMessage(
                    MessageBody(UIText.PluralResource(R.plurals.unread_event_call, 1, 1))
                )
            }
        }

        MessagePreviewContent.CryptoSessionReset -> UILastMessageContent.None
        Unknown -> UILastMessageContent.None
    }
}

fun <T> List<T>.plusIf(condition: () -> Boolean, element: T): List<T> = plus(if (condition()) listOf(element) else listOf())
fun <T> List<T>.plusIf(condition: () -> Boolean, elements: List<T>): List<T> = plus(if (condition()) elements else listOf())
