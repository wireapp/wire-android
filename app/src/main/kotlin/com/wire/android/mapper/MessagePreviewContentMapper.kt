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

package com.wire.android.mapper

import com.wire.android.R
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.ui.markdown.MarkdownConstants
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.toUIText
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

    return when {
        // when unread event count is empty show last message
        unreadEventCount.isEmpty() -> uiLastMessageContent()
        // when there are only unread message events also show last message
        unreadEventCount.size == 1 && unreadEventCount.keys.first() == UnreadEventType.MESSAGE -> uiLastMessageContent()
        // for the one type events show last message only where their count equals one
        unreadEventCount.size == 1 && unreadEventCount.values.first() == 1 -> uiLastMessageContent()
        // for the rest take 1 or 2 most prioritized events with count to last message
        else -> multipleUnreadEventsToLastMessage(unreadEventCount)
    }
}

private fun multipleUnreadEventsToLastMessage(unreadEventCount: UnreadEventCount): UILastMessageContent {
    val unreadContentTexts = unreadEventCount
        .toSortedMap()
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

    val first = unreadContentTexts.values.first()
    return if (unreadContentTexts.entries.size > 1) {
        val second = unreadContentTexts.values.elementAt(1)
        UILastMessageContent.MultipleMessage(listOf(first, second))
    } else {
        UILastMessageContent.TextMessage(MessageBody(first))
    }
}

private fun String?.userUiText(isSelfMessage: Boolean): UIText = when {
    isSelfMessage -> UIText.StringResource(R.string.member_name_you_label_titlecase)
    this != null -> UIText.DynamicString(this)
    else -> UIText.StringResource(R.string.username_unavailable_label)
}

private fun assetMessageResId(
    assetType: AssetType,
    isSelfMessage: Boolean,
    count: Int
): UIText {
    return if (count > 1) {
        val resId = when (assetType) {
            AssetType.AUDIO -> if (isSelfMessage) R.string.last_message_self_user_shared_multiple_audio
            else R.string.last_message_other_user_shared_multiple_audio

            AssetType.IMAGE -> if (isSelfMessage) R.string.last_message_self_user_shared_multiple_image
            else R.string.last_message_other_user_shared_multiple_image

            AssetType.VIDEO -> if (isSelfMessage) R.string.last_message_self_user_shared_multiple_video
            else R.string.last_message_other_user_shared_multiple_video

            AssetType.GENERIC_ASSET -> if (isSelfMessage) R.string.last_message_self_user_shared_multiple_asset
            else R.string.last_message_other_user_shared_multiple_asset
        }
        UIText.StringResource(resId, count)
    } else {
        val resId = when (assetType) {
            AssetType.AUDIO -> if (isSelfMessage) R.string.last_message_self_user_shared_audio
            else R.string.last_message_other_user_shared_audio

            AssetType.IMAGE -> if (isSelfMessage) R.string.last_message_self_user_shared_image
            else R.string.last_message_other_user_shared_image

            AssetType.VIDEO -> if (isSelfMessage) R.string.last_message_self_user_shared_video
            else R.string.last_message_other_user_shared_video

            AssetType.GENERIC_ASSET -> if (isSelfMessage) R.string.last_message_self_user_shared_asset
            else R.string.last_message_other_user_shared_asset
        }
        UIText.StringResource(resId)
    }
}

@Suppress("LongMethod", "ComplexMethod", "NestedBlockDepth")
fun MessagePreview.uiLastMessageContent(): UILastMessageContent {
    return when (content) {
        is WithUser -> {
            val userContent = (content as WithUser)
            val userUIText = userContent.username.userUiText(isSelfMessage)
            when ((userContent)) {
                is WithUser.Asset -> UILastMessageContent.SenderWithMessage(
                    sender = userUIText,
                    message = assetMessageResId((content as WithUser.Asset).type, isSelfMessage, (content as WithUser.Asset).count)
                )

                is WithUser.ConversationNameChange -> UILastMessageContent.SenderWithMessage(
                    userUIText,
                    UIText.StringResource(
                        if (isSelfMessage) {
                            R.string.last_message_self_changed_conversation_name
                        } else {
                            R.string.last_message_other_changed_conversation_name
                        }
                    )
                )

                is WithUser.Knock -> UILastMessageContent.SenderWithMessage(
                    userUIText,
                    UIText.StringResource(
                        if (isSelfMessage) {
                            R.string.last_message_self_user_knock
                        } else {
                            R.string.last_message_other_user_knock
                        }
                    )
                )

                is WithUser.MemberJoined -> UILastMessageContent.SenderWithMessage(
                    userUIText,
                    UIText.StringResource(
                        if (isSelfMessage) {
                            R.string.last_message_self_user_joined_conversation
                        } else {
                            R.string.last_message_other_user_joined_conversation
                        }
                    )
                )

                is WithUser.MemberLeft -> UILastMessageContent.SenderWithMessage(
                    userUIText,
                    UIText.StringResource(
                        if (isSelfMessage) {
                            R.string.last_message_self_user_left_conversation
                        } else {
                            R.string.last_message_other_user_left_conversation
                        }
                    )
                )

                is WithUser.MembersAdded -> {
                    val membersAddedContent = (content as WithUser.MembersAdded)
                    val isSelfAdded = membersAddedContent.isSelfUserAdded
                    val otherUsersSize = membersAddedContent.otherUserIdList.size

                    val previewMessageContent = when {
                        isSelfMessage && otherUsersSize > 0 -> {
                            UIText.PluralResource(R.plurals.last_message_self_added_users, otherUsersSize, otherUsersSize)
                        }

                        !isSelfMessage && isSelfAdded -> {
                            if (otherUsersSize == 0) {
                                UIText.StringResource(R.string.last_message_other_added_only_self_user)
                            } else {
                                UIText.PluralResource(R.plurals.last_message_other_added_self_user, otherUsersSize, otherUsersSize)
                            }
                        }

                        else -> {
                            UIText.PluralResource(R.plurals.last_message_other_added_other_users, otherUsersSize, otherUsersSize)
                        }
                    }

                    UILastMessageContent.TextMessage(MessageBody(previewMessageContent))
                }

                is WithUser.ConversationMembersRemoved -> {
                    val conversationMembersRemovedContent = (content as WithUser.ConversationMembersRemoved)
                    val isSelfRemoved = conversationMembersRemovedContent.isSelfUserRemoved
                    val otherUsersSize = conversationMembersRemovedContent.otherUserIdList.size

                    val previewMessageContent = when {
                        isSelfMessage && otherUsersSize > 0 -> {
                            UIText.PluralResource(R.plurals.last_message_self_removed_users, otherUsersSize, otherUsersSize)
                        }

                        !isSelfMessage && isSelfRemoved -> {
                            if (otherUsersSize == 0) {
                                UIText.StringResource(R.string.last_message_other_removed_only_self_user)
                            } else {
                                UIText.PluralResource(
                                    R.plurals.last_message_other_removed_self_user_and_others,
                                    otherUsersSize,
                                    otherUsersSize
                                )
                            }
                        }

                        else -> {
                            UIText.PluralResource(R.plurals.last_message_other_removed_other_users, otherUsersSize, otherUsersSize)
                        }
                    }

                    UILastMessageContent.TextMessage(MessageBody(previewMessageContent))
                }

                is WithUser.TeamMembersRemoved -> {
                    val teamMembersRemovedContent = (content as WithUser.TeamMembersRemoved)
                    val previewMessageContent =
                        UIText.PluralResource(R.plurals.last_message_team_member_removed, teamMembersRemovedContent.otherUserIdList.size)

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

                is WithUser.TeamMemberRemoved -> UILastMessageContent.None
                is WithUser.Text -> UILastMessageContent.SenderWithMessage(
                    sender = userUIText,
                    message = (content as WithUser.Text).messageBody.let { UIText.DynamicString(it) },
                    separator = ":${MarkdownConstants.NON_BREAKING_SPACE}"
                )

                is WithUser.Composite -> {
                    val text = (content as WithUser.Composite).messageBody?.let { UIText.DynamicString(it) }
                        ?: UIText.StringResource(R.string.last_message_composite_with_missing_text)
                    UILastMessageContent.SenderWithMessage(
                        sender = userUIText,
                        message = text,
                        separator = ":${MarkdownConstants.NON_BREAKING_SPACE}"
                    )
                }

                is WithUser.MissedCall -> UILastMessageContent.TextMessage(
                    MessageBody(UIText.PluralResource(R.plurals.unread_event_call, 1, 1))
                )

                is WithUser.MembersCreationAdded -> UILastMessageContent.None
                is WithUser.MembersFailedToAdd -> UILastMessageContent.None
                is WithUser.Location -> UILastMessageContent.SenderWithMessage(
                    userUIText,
                    UIText.StringResource(
                        if (isSelfMessage) {
                            R.string.last_message_self_user_shared_location
                        } else {
                            R.string.last_message_other_user_shared_location
                        }
                    )
                )

                is WithUser.Deleted -> UILastMessageContent.SenderWithMessage(
                    sender = userUIText,
                    message = UIText.StringResource(R.string.deleted_message_text),
                    separator = ":${MarkdownConstants.NON_BREAKING_SPACE}"
                )
            }
        }

        is MessagePreviewContent.FederatedMembersRemoved -> {
            val membersRemovedContent = (content as MessagePreviewContent.FederatedMembersRemoved)
            val isSelfRemoved = membersRemovedContent.isSelfUserRemoved
            val otherUsersSize = membersRemovedContent.otherUserIdList.size

            val previewMessageContent = when {
                isSelfRemoved -> {
                    if (otherUsersSize == 0) {
                        UIText.StringResource(R.string.last_message_other_removed_only_self_user)
                    } else {
                        UIText.PluralResource(
                            R.plurals.last_message_other_removed_self_user_and_others,
                            otherUsersSize,
                            otherUsersSize
                        )
                    }
                }

                else -> {
                    UIText.PluralResource(R.plurals.last_message_other_removed_other_users, otherUsersSize, otherUsersSize)
                }
            }

            UILastMessageContent.TextMessage(MessageBody(previewMessageContent))
        }

        is MessagePreviewContent.Ephemeral -> {
            val ephemeralContent = (content as MessagePreviewContent.Ephemeral)
            if (ephemeralContent.isGroupConversation) {
                UILastMessageContent.TextMessage(
                    MessageBody(UIText.StringResource(R.string.ephemeral_group_channel_event_message))
                )
            } else {
                UILastMessageContent.TextMessage(
                    MessageBody(UIText.StringResource(R.string.ephemeral_one_to_one_event_message))
                )
            }
        }

        MessagePreviewContent.CryptoSessionReset -> UILastMessageContent.None
        MessagePreviewContent.VerificationChanged.VerifiedMls ->
            UILastMessageContent.VerificationChanged(R.string.last_message_verified_conversation_mls)

        MessagePreviewContent.VerificationChanged.VerifiedProteus ->
            UILastMessageContent.VerificationChanged(R.string.last_message_verified_conversation_proteus)

        MessagePreviewContent.VerificationChanged.DegradedMls ->
            UILastMessageContent.VerificationChanged(R.string.last_message_conversations_verification_degraded_mls)

        MessagePreviewContent.VerificationChanged.DegradedProteus ->
            UILastMessageContent.VerificationChanged(R.string.last_message_conversations_verification_degraded_proteus)

        is MessagePreviewContent.Draft -> {
            with(content as MessagePreviewContent.Draft) {
                val messageUiText = if (attachmentsCount > 0) {
                    val pluralId = attachmentPlural(attachmentType)
                    UIText.PluralResource(pluralId, attachmentsCount, attachmentsCount)
                } else {
                    message?.toUIText() ?: "".toUIText()
                }

                UILastMessageContent.SenderWithMessage(
                    sender = UIText.StringResource(R.string.label_draft),
                    message = messageUiText,
                    separator = ":${MarkdownConstants.NON_BREAKING_SPACE}"
                )
            }
        }

        Unknown -> UILastMessageContent.None
    }
}

private fun attachmentPlural(type: String?): Int =
    when {
        type == null -> R.plurals.draft_files_count
        "image/" in type -> R.plurals.draft_image_count
        "video/" in type -> R.plurals.draft_video_count
        "audio/" in type -> R.plurals.draft_audio_count
        else -> R.plurals.draft_files_count
    }

