package com.wire.android.mapper

import com.wire.android.R
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.UnreadEventCount
import com.wire.kalium.logic.data.message.AssetType
import com.wire.kalium.logic.data.message.MessagePreview
import com.wire.kalium.logic.data.message.MessagePreviewContent.Unknown
import com.wire.kalium.logic.data.message.MessagePreviewContent.WithUser
import com.wire.kalium.logic.data.message.UnreadEventType

@Suppress("ReturnCount")
fun MessagePreview?.toUIPreview(
    unreadEventCount: UnreadEventCount,
    unreadMentionsCount: Long,
): UILastMessageContent {
    if (this == null) {
        return UILastMessageContent.None
    }

    val sortedUnreadContent = unreadEventCount
        .plus(if (unreadMentionsCount > 0) mapOf(Pair(UnreadEventType.MENTION, unreadMentionsCount.toInt())) else mapOf())
        .toSortedMap()

    // we want to show last text message content instead of counter when there are only unread text messages
    if (!(sortedUnreadContent.size == 1 && sortedUnreadContent.contains(UnreadEventType.MESSAGE))) {
        val unreadContentTexts = sortedUnreadContent
            .mapNotNull { type ->
                when (type.key) {
                    UnreadEventType.KNOCK -> UIText.PluralResource(R.plurals.unread_event_knock, type.value, type.value)
                    UnreadEventType.MISSED_CALL -> UIText.PluralResource(R.plurals.unread_event_call, type.value, type.value)
                    UnreadEventType.MENTION -> UIText.PluralResource(R.plurals.unread_event_mention, type.value, type.value)
                    // TODO we need to decrease number of text messages by mentions count because currently they contain them
                    UnreadEventType.MESSAGE -> {
                        val messagesWithoutMentions = type.value - unreadMentionsCount.toInt()
                        if (messagesWithoutMentions > 0) {
                            UIText.PluralResource(
                                R.plurals.unread_event_message,
                                messagesWithoutMentions,
                                messagesWithoutMentions
                            )
                        } else {
                            null
                        }
                    }
                    UnreadEventType.IGNORED -> null
                    null -> null
                }
            }
        if (unreadContentTexts.size > 1) {
            val first = unreadContentTexts.first()
            val second = unreadContentTexts.elementAt(1)
            return UILastMessageContent.MultipleMessage(first, second)
        } else if (unreadContentTexts.isNotEmpty()) {
            return UILastMessageContent.TextMessage(MessageBody(unreadContentTexts.first()))
        }
    }

    return uiLastMessageContent()
}

fun String?.userUiText(isSelfMessage: Boolean): UIText = when {
    isSelfMessage -> UIText.StringResource(R.string.member_name_you_label_titlecase)
    this != null -> UIText.DynamicString(this)
    else -> UIText.StringResource(R.string.username_unavailable_label)

}

@Suppress("LongMethod", "ComplexMethod")
private fun MessagePreview.uiLastMessageContent(): UILastMessageContent {
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
                is WithUser.MembersAdded -> UILastMessageContent.SenderWithMessage(
                    userUIText,
                    UIText.PluralResource(
                        R.plurals.last_message_people_added,
                        (content as WithUser.MembersAdded).count,
                        (content as WithUser.MembersAdded).count,
                    )
                )
                is WithUser.MembersRemoved -> UILastMessageContent.SenderWithMessage(
                    userUIText,
                    UIText.PluralResource(
                        R.plurals.last_message_people_removed,
                        (content as WithUser.MembersRemoved).count,
                        (content as WithUser.MembersRemoved).count
                    )
                )
                is WithUser.MentionedSelf -> TODO()
                is WithUser.QuotedSelf -> TODO()
                is WithUser.TeamMemberRemoved -> TODO()
                is WithUser.Text -> UILastMessageContent.SenderWithMessage(
                    sender = userUIText,
                    message = UIText.DynamicString((content as WithUser.Text).messageBody),
                    separator = ": "
                )
                is WithUser.MissedCall -> UILastMessageContent.SenderWithMessage(
                    userUIText,
                    UIText.StringResource(R.string.last_message_call)
                )
            }
        }
        Unknown -> UILastMessageContent.None
    }
}
