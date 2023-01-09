package com.wire.android.mapper

import com.wire.android.R
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.findUser
import com.wire.android.ui.home.conversations.model.MessageFooter
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.MessageTime
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.previewAsset
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.time.ISOFormatter
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.Message.Visibility.HIDDEN
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import javax.inject.Inject

class MessageMapper @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val userTypeMapper: UserTypeMapper,
    private val messageContentMapper: MessageContentMapper,
    private val isoFormatter: ISOFormatter,
    private val wireSessionImageLoader: WireSessionImageLoader
) {

    fun memberIdList(messages: List<Message>): List<UserId> = messages.flatMap { message ->
        listOf(message.senderUserId).plus(
            when (val content = message.content) {
                is MessageContent.MemberChange -> content.members
                else -> listOf()
            }
        )
    }.distinct()

    fun toUIMessages(userList: List<User>, messages: List<Message.Standalone>): List<UIMessage> = messages.mapNotNull { message ->
        val sender = userList.findUser(message.senderUserId)
        val content = messageContentMapper.fromMessage(
            message = message,
            userList = userList
        )

        val footer = if (message is Message.Regular) {
            // TODO find ugly and proper heart emoji and merge them to ugly one 😅
            val totalHeartsCount = message.reactions.totalReactions
                .filterKeys { isHeart(it) }.values
                .sum()

            val hasSelfHeart = message.reactions.selfUserReactions.any { isHeart(it) }

            MessageFooter(message.id,
                message.reactions.totalReactions
                    .filter { !isHeart(it.key) }
                    .run {
                        if (totalHeartsCount != 0)
                            plus("❤" to totalHeartsCount)
                        else
                            this
                    },
                message.reactions.selfUserReactions
                    .filter { isHeart(it) }.toSet()
                    .run {
                        if (hasSelfHeart)
                            plus("❤")
                        else
                            this
                    }
            )
        } else {
            MessageFooter(message.id)
        }

        // System messages don't have header so without the content there is nothing to be displayed.
        // Also hidden messages should not be displayed, as well preview images
        val shouldNotDisplay =
            message is Message.System && content == null || message.visibility == HIDDEN || content is UIMessageContent.PreviewAssetMessage
        if (shouldNotDisplay) {
            null
        } else {
            UIMessage(
                messageContent = content,
                messageSource = if (sender is SelfUser) MessageSource.Self else MessageSource.OtherUser,
                messageHeader = provideMessageHeader(sender, message),
                messageFooter = footer,
                userAvatarData = getUserAvatarData(sender)
            )
        }
    }

    private fun isHeart(it: String) = it == "❤️" || it == "❤"

    private fun provideMessageHeader(sender: User?, message: Message.Standalone): MessageHeader = MessageHeader(
        username = sender?.name?.let { UIText.DynamicString(it) }
            ?: UIText.StringResource(R.string.username_unavailable_label),
        membership = when (sender) {
            is OtherUser -> userTypeMapper.toMembership(sender.userType)
            is SelfUser, null -> Membership.None
        },
        connectionState = getConnectionState(sender),
        isLegalHold = false,
        messageTime = MessageTime(message.date),
        messageStatus = getMessageStatus(message),
        messageId = message.id,
        userId = sender?.id,
        isSenderDeleted = when (sender) {
            is OtherUser -> sender.deleted
            is SelfUser, null -> false
        },
        isSenderUnavailable = when (sender) {
            is OtherUser -> sender.isUnavailableUser
            is SelfUser, null -> false
        },
        clientId = (message as? Message.Sendable)?.senderClientId
    )

    private fun getMessageStatus(message: Message.Standalone) = when {
        message.status == Message.Status.FAILED -> MessageStatus.SendFailure
        message.visibility == Message.Visibility.DELETED -> MessageStatus.Deleted
        message is Message.Regular && message.editStatus is Message.EditStatus.Edited ->
            MessageStatus.Edited(
                isoFormatter.fromISO8601ToTimeFormat(
                    utcISO = (message.editStatus as Message.EditStatus.Edited).lastTimeStamp
                )
            )
        message is Message.Regular && message.content is MessageContent.FailedDecryption ->
            MessageStatus.DecryptionFailure((message.content as MessageContent.FailedDecryption).isDecryptionResolved)
        else -> MessageStatus.Untouched
    }

    private fun getUserAvatarData(sender: User?) = UserAvatarData(
        asset = sender?.previewAsset(wireSessionImageLoader),
        availabilityStatus = sender?.availabilityStatus ?: UserAvailabilityStatus.NONE,
        connectionState = getConnectionState(sender)
    )

    private fun getConnectionState(sender: User?) =
        when (sender) {
            is OtherUser -> sender.connectionStatus
            is SelfUser, null -> null
        }
}
