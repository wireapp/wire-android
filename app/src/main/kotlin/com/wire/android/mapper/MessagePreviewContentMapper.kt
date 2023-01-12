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
    if (sortedUnreadContent.size > 1) {
        val unreadContentTexts = sortedUnreadContent
            .mapNotNull { type ->
                when (type.key) {
                    UnreadEventType.KNOCK -> UIText.PluralResource(R.plurals.unread_event_knock, type.value, type.value)
                    UnreadEventType.MISSED_CALL -> UIText.PluralResource(R.plurals.unread_event_call, type.value, type.value)
                    UnreadEventType.MENTION -> UIText.PluralResource(R.plurals.unread_event_mention, type.value, type.value)
                    UnreadEventType.REPLY -> UIText.PluralResource(R.plurals.unread_event_reply, type.value, type.value)
                    UnreadEventType.MESSAGE -> UIText.PluralResource(R.plurals.unread_event_message, type.value, type.value)
                    UnreadEventType.IGNORED -> null
                    null -> null
                }
            }
        if (unreadContentTexts.size > 1) {
            val first = unreadContentTexts.first()
            val second = unreadContentTexts.elementAt(1)
            return UILastMessageContent.MultipleMessage(listOf(first, second))
        } else if (unreadContentTexts.isNotEmpty()) {
            return UILastMessageContent.TextMessage(MessageBody(unreadContentTexts.first()))
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

                    UILastMessageContent.MultipleMessage(
                        listOf(
                            userUIText,
                            UIText.StringResource(R.string.last_message_added),
                        )
                            .plus(if (isSelfAdded) listOf(UIText.StringResource(R.string.member_name_you_label_lowercase)) else listOf())
                            .plusIf(
                                { isSelfAdded && membersAddedContent.otherUserIdList.isNotEmpty() },
                                listOf(UIText.StringResource(R.string.label_and))
                            )
                            .plusIf(
                                { membersAddedContent.otherUserIdList.isNotEmpty() },
                                UIText.PluralResource(
                                    R.plurals.last_message_people,
                                    membersAddedContent.otherUserIdList.size,
                                    membersAddedContent.otherUserIdList.size
                                )
                            )
                            .plus(UIText.StringResource(R.string.last_message_to_conversation))
                    )
                }

                is WithUser.MembersRemoved -> {
                    val membersRemovedContent = (content as WithUser.MembersRemoved)
                    val isSelfRemoved = membersRemovedContent.isSelfUserRemoved

                    UILastMessageContent.MultipleMessage(
                        listOf(
                            userUIText,
                            UIText.StringResource(R.string.last_message_removed),
                        )
                            .plus(if (isSelfRemoved) listOf(UIText.StringResource(R.string.member_name_you_label_lowercase)) else listOf())
                            .plusIf(
                                { isSelfRemoved && membersRemovedContent.otherUserIdList.isNotEmpty() },
                                listOf(UIText.StringResource(R.string.label_and))
                            )
                            .plusIf(
                                { membersRemovedContent.otherUserIdList.isNotEmpty() },
                                UIText.PluralResource(
                                    R.plurals.last_message_people,
                                    membersRemovedContent.otherUserIdList.size,
                                    membersRemovedContent.otherUserIdList.size
                                )
                            )
                            .plus(UIText.StringResource(R.string.last_message_from_conversation))
                    )
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
