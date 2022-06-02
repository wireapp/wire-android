package com.wire.android.mapper

import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.ui.home.conversations.findSender
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.User
import com.wire.android.ui.home.conversations.name
import com.wire.android.ui.home.conversations.previewAsset
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.message.Message
import javax.inject.Inject

class MessageMapper @Inject constructor(
    private val userTypeMapper: UserTypeMapper,
    private val messageContentMapper: MessageContentMapper,
) {
    suspend fun toUIMessages(
        memberDetails: List<MemberDetails>,
        messages: List<Message>
    ): List<UIMessage> {
        return messages.map { message ->
            val sender = memberDetails.findSender(message.senderUserId)

            UIMessage(
                messageContent = messageContentMapper.fromMessage(message),
                messageSource = if (sender is MemberDetails.Self) MessageSource.Self else MessageSource.OtherUser,
                messageHeader = MessageHeader(
                    // TODO: Designs for deleted users?
                    username = sender.name?.let { UIText.DynamicString(it) } ?: UIText.StringResource(R.string.member_name_deleted_label),
                    membership = if (sender is MemberDetails.Other) userTypeMapper.toMembership(sender.userType) else Membership.None,
                    isLegalHold = false,
                    time = message.date,
                    messageStatus = if (message.status == Message.Status.FAILED) MessageStatus.SendFailure else MessageStatus.Untouched,
                    messageId = message.id
                ),
                user = User(
                    avatarAsset = sender.previewAsset, availabilityStatus = UserStatus.NONE
                )
            )
        }
    }
}
