package com.wire.android.mapper.message.content

import com.wire.android.R
import com.wire.android.ui.home.conversations.findUser
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.data.user.UserId
import javax.inject.Inject

class SystemMessageContentMapper
@Inject constructor(
    private val messageResourceProvider: MessageResourceProvider
) {
    fun mapSystemMessage(
        message: Message.System,
        members: List<User>
    ): UIMessageContent.SystemMessage? = when (val content = message.content) {
        is MessageContent.MemberChange -> mapMemberChangeMessage(content, message.senderUserId, members)
        is MessageContent.MissedCall -> mapMissedCallMessage(message.senderUserId, members)
        is MessageContent.ConversationRenamed -> mapConversationRenamedMessage(message.senderUserId, content, members)
        is MessageContent.TeamMemberRemoved -> mapTeamMemberRemovedMessage(content)
        is MessageContent.CryptoSessionReset -> mapResetSession(message.senderUserId, members)
        is MessageContent.ConversationReceiptModeChanged -> mapConversationReceiptModeChanged(message.senderUserId, content, members)
        is MessageContent.HistoryLost -> mapConversationHistoryLost()
        is MessageContent.NewConversationReceiptMode -> mapNewConversationReceiptMode(content)
    }

    private fun mapResetSession(
        senderUserId: UserId,
        userList: List<User>
    ): UIMessageContent.SystemMessage {
        val sender = userList.findUser(userId = senderUserId)
        val authorName = toSystemMessageMemberName(user = sender, type = MessageContentMapper.SelfNameType.ResourceTitleCase)
        return UIMessageContent.SystemMessage.CryptoSessionReset(authorName)
    }

    private fun mapConversationReceiptModeChanged(
        senderUserId: UserId,
        content: MessageContent.ConversationReceiptModeChanged,
        userList: List<User>
    ): UIMessageContent.SystemMessage {
        val sender = userList.findUser(userId = senderUserId)
        val authorName = toSystemMessageMemberName(
            user = sender,
            type = MessageContentMapper.SelfNameType.ResourceTitleCase
        )
        return UIMessageContent.SystemMessage.ConversationReceiptModeChanged(
            author = authorName,
            receiptMode = when (content.receiptMode) {
                true -> UIText.StringResource(R.string.label_system_message_receipt_mode_on)
                else -> UIText.StringResource(R.string.label_system_message_receipt_mode_off)
            }
        )
    }

    private fun mapMissedCallMessage(
        senderUserId: UserId,
        userList: List<User>
    ): UIMessageContent.SystemMessage {
        val sender = userList.findUser(userId = senderUserId)
        val authorName = toSystemMessageMemberName(
            user = sender,
            type = MessageContentMapper.SelfNameType.ResourceTitleCase
        )
        return if (sender is SelfUser) {
            UIMessageContent.SystemMessage.MissedCall.YouCalled(authorName)
        } else {
            UIMessageContent.SystemMessage.MissedCall.OtherCalled(authorName)
        }
    }

    private fun mapTeamMemberRemovedMessage(
        content: MessageContent.TeamMemberRemoved,
    ): UIMessageContent.SystemMessage = UIMessageContent.SystemMessage.TeamMemberRemoved(content)

    private fun mapConversationRenamedMessage(
        senderUserId: UserId,
        content: MessageContent.ConversationRenamed,
        userList: List<User>
    ): UIMessageContent.SystemMessage {
        val sender = userList.findUser(userId = senderUserId)
        val authorName = toSystemMessageMemberName(
            user = sender,
            type = MessageContentMapper.SelfNameType.ResourceTitleCase
        )
        return UIMessageContent.SystemMessage.RenamedConversation(authorName, content)
    }

    private fun mapNewConversationReceiptMode(
        content: MessageContent.NewConversationReceiptMode
    ): UIMessageContent.SystemMessage {
        return UIMessageContent.SystemMessage.NewConversationReceiptMode(
            receiptMode = when (content.receiptMode) {
                true -> UIText.StringResource(R.string.label_system_message_receipt_mode_on)
                else -> UIText.StringResource(R.string.label_system_message_receipt_mode_off)
            }
        )
    }

    fun mapMemberChangeMessage(
        content: MessageContent.MemberChange,
        senderUserId: UserId,
        userList: List<User>
    ): UIMessageContent.SystemMessage? {
        val sender = userList.findUser(userId = senderUserId)
        val isAuthorSelfAction = content.members.size == 1 && senderUserId == content.members.first()
        val authorName = toSystemMessageMemberName(user = sender, type = MessageContentMapper.SelfNameType.ResourceTitleCase)
        val memberNameList = content.members.map {
            toSystemMessageMemberName(
                user = userList.findUser(userId = it),
                type = MessageContentMapper.SelfNameType.ResourceLowercase
            )
        }
        return when (content) {
            is MessageContent.MemberChange.Added ->
                if (isAuthorSelfAction) {
                    null // we don't want to show "You added you to the conversation"
                } else {
                    UIMessageContent.SystemMessage.MemberAdded(author = authorName, memberNames = memberNameList)
                }

            is MessageContent.MemberChange.Removed ->
                if (isAuthorSelfAction) {
                    UIMessageContent.SystemMessage.MemberLeft(author = authorName)
                } else {
                    UIMessageContent.SystemMessage.MemberRemoved(author = authorName, memberNames = memberNameList)
                }
        }
    }

    private fun mapConversationHistoryLost(): UIMessageContent.SystemMessage = UIMessageContent.SystemMessage.HistoryLost()

    fun toSystemMessageMemberName(
        user: User?,
        type: MessageContentMapper.SelfNameType = MessageContentMapper.SelfNameType.NameOrDeleted
    ): UIText = when (user) {
        is OtherUser -> user.name?.let { UIText.DynamicString(it) } ?: UIText.StringResource(messageResourceProvider.memberNameDeleted)
        is SelfUser -> when (type) {
            MessageContentMapper.SelfNameType.ResourceLowercase -> UIText.StringResource(messageResourceProvider.memberNameYouLowercase)
            MessageContentMapper.SelfNameType.ResourceTitleCase -> UIText.StringResource(messageResourceProvider.memberNameYouTitleCase)
            MessageContentMapper.SelfNameType.NameOrDeleted -> user.name?.let { UIText.DynamicString(it) }
                ?: UIText.StringResource(messageResourceProvider.memberNameDeleted)
        }

        else -> UIText.StringResource(messageResourceProvider.memberNameDeleted)
    }
}
