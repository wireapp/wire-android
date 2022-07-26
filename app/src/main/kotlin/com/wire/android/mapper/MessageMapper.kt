package com.wire.android.mapper

import com.wire.android.R
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.findUser
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.previewAsset
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.time.ISOFormatter
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.android.util.uiMessageDateTime
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.withContext
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

    suspend fun toUIMessages(
        userList: List<User>,
        messages: List<Message>
    ): List<UIMessage> = withContext(dispatcherProvider.io()) {
        messages.map { message ->
            val sender = userList.findUser(message.senderUserId)
            val content = messageContentMapper.fromMessage(
                message = message,
                userList = userList
            )
            if (message is Message.System && content == null)
                null // system messages doesn't have header so without the content there is nothing to be displayed
            else
                UIMessage(
                    messageContent = content,
                    messageSource = if (sender is SelfUser) MessageSource.Self else MessageSource.OtherUser,
                    messageHeader = MessageHeader(
                        // TODO: Designs for deleted users?
                        username = sender?.name?.let { UIText.DynamicString(it) }
                            ?: UIText.StringResource(R.string.member_name_deleted_label),
                        membership = when (sender) {
                            is OtherUser -> userTypeMapper.toMembership(sender.userType)
                            is SelfUser, null -> Membership.None
                        },
                        isLegalHold = false,
                        time = message.date.uiMessageDateTime() ?: "",
                        messageStatus = when {
                            message.status == Message.Status.FAILED -> MessageStatus.SendFailure
                            message.visibility == Message.Visibility.DELETED -> MessageStatus.Deleted
                            message is Message.Regular && message.editStatus is Message.EditStatus.Edited ->
                                MessageStatus.Edited(
                                    isoFormatter.fromISO8601ToTimeFormat(
                                        utcISO = (message.editStatus as Message.EditStatus.Edited).lastTimeStamp
                                    )
                                )
                            message.content is MessageContent.FailedDecryption -> MessageStatus.DecryptionFailure
                            else -> MessageStatus.Untouched
                        },
                        messageId = message.id,
                        userId = sender?.id
                    ),
                    userAvatarData = UserAvatarData(asset = sender?.previewAsset(wireSessionImageLoader))
                )
        }.filterNotNull()
    }
}
