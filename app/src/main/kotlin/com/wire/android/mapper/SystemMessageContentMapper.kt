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
import com.wire.android.ui.home.conversations.findUser
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.util.formatFullDateShortTime
import com.wire.android.util.orDefault
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.message.MessageContent.MemberChange
import com.wire.kalium.logic.data.message.MessageContent.MemberChange.Added
import com.wire.kalium.logic.data.message.MessageContent.MemberChange.CreationAdded
import com.wire.kalium.logic.data.message.MessageContent.MemberChange.FailedToAdd
import com.wire.kalium.logic.data.message.MessageContent.MemberChange.Removed
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.data.user.UserId
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@Suppress("TooManyFunctions")
class SystemMessageContentMapper @Inject constructor(
    private val messageResourceProvider: MessageResourceProvider
) {

    fun mapMessage(
        message: Message.System,
        members: List<User>
    ) = when (val content = message.content) {
        is MemberChange -> mapMemberChangeMessage(content, message.senderUserId, members)
        is MessageContent.MissedCall -> mapMissedCallMessage(message.senderUserId, members)
        is MessageContent.ConversationRenamed -> mapConversationRenamedMessage(message.senderUserId, content, members)
        is MessageContent.TeamMemberRemoved -> mapTeamMemberRemovedMessage(content)
        is MessageContent.CryptoSessionReset -> mapResetSession(message.senderUserId, members)
        is MessageContent.NewConversationReceiptMode -> mapNewConversationReceiptMode(content)
        is MessageContent.ConversationReceiptModeChanged -> mapConversationReceiptModeChanged(message.senderUserId, content, members)
        is MessageContent.HistoryLost -> mapConversationHistoryLost()
        is MessageContent.ConversationMessageTimerChanged -> mapConversationTimerChanged(message.senderUserId, content, members)
        is MessageContent.ConversationCreated -> mapConversationCreated(message.senderUserId, message.date, members)
        is MessageContent.MLSWrongEpochWarning -> mapMLSWrongEpochWarning()
    }

    private fun mapConversationCreated(senderUserId: UserId, date: String, userList: List<User>): UIMessageContent.SystemMessage {
        val sender = userList.findUser(userId = senderUserId)
        val authorName = mapMemberName(
            user = sender,
            type = SelfNameType.ResourceTitleCase
        )
        return UIMessageContent.SystemMessage.ConversationMessageCreated(
            author = authorName,
            isAuthorSelfUser = sender is SelfUser,
            date.formatFullDateShortTime().orDefault(date).toUpperCase()
        )
    }

    private fun mapConversationTimerChanged(
        senderUserId: UserId,
        content: MessageContent.ConversationMessageTimerChanged,
        userList: List<User>
    ): UIMessageContent.SystemMessage {
        val sender = userList.findUser(userId = senderUserId)
        val authorName = mapMemberName(
            user = sender,
            type = SelfNameType.ResourceTitleCase
        )

        return if (content.messageTimer != null) {
            UIMessageContent.SystemMessage.ConversationMessageTimerActivated(
                author = authorName,
                isAuthorSelfUser = sender is SelfUser,
                selfDeletionDuration = (content.messageTimer ?: 0L).milliseconds.toSelfDeletionDuration()
            )
        } else {
            UIMessageContent.SystemMessage.ConversationMessageTimerDeactivated(
                author = authorName,
                isAuthorSelfUser = sender is SelfUser
            )
        }
    }

    private fun mapResetSession(
        senderUserId: UserId,
        userList: List<User>
    ): UIMessageContent.SystemMessage {
        val sender = userList.findUser(userId = senderUserId)
        val authorName = mapMemberName(user = sender, type = SelfNameType.ResourceTitleCase)
        return UIMessageContent.SystemMessage.CryptoSessionReset(authorName)
    }

    private fun mapMissedCallMessage(
        senderUserId: UserId,
        userList: List<User>
    ): UIMessageContent.SystemMessage {
        val sender = userList.findUser(userId = senderUserId)
        val authorName = mapMemberName(
            user = sender,
            type = SelfNameType.ResourceTitleCase
        )
        return if (sender is SelfUser) {
            UIMessageContent.SystemMessage.MissedCall.YouCalled(authorName)
        } else {
            UIMessageContent.SystemMessage.MissedCall.OtherCalled(authorName)
        }
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

    private fun mapConversationReceiptModeChanged(
        senderUserId: UserId,
        content: MessageContent.ConversationReceiptModeChanged,
        userList: List<User>
    ): UIMessageContent.SystemMessage {
        val sender = userList.findUser(userId = senderUserId)
        val authorName = mapMemberName(
            user = sender,
            type = SelfNameType.ResourceTitleCase
        )
        return UIMessageContent.SystemMessage.ConversationReceiptModeChanged(
            author = authorName,
            receiptMode = when (content.receiptMode) {
                true -> UIText.StringResource(R.string.label_system_message_receipt_mode_on)
                else -> UIText.StringResource(R.string.label_system_message_receipt_mode_off)
            },
            isAuthorSelfUser = sender is SelfUser
        )
    }

    private fun mapTeamMemberRemovedMessage(
        content: MessageContent.TeamMemberRemoved
    ): UIMessageContent.SystemMessage = UIMessageContent.SystemMessage.TeamMemberRemoved(content)

    private fun mapConversationRenamedMessage(
        senderUserId: UserId,
        content: MessageContent.ConversationRenamed,
        userList: List<User>
    ): UIMessageContent.SystemMessage {
        val sender = userList.findUser(userId = senderUserId)
        val authorName = mapMemberName(
            user = sender,
            type = SelfNameType.ResourceTitleCase
        )
        return UIMessageContent.SystemMessage.RenamedConversation(authorName, content)
    }

    fun mapMemberChangeMessage(
        content: MemberChange,
        senderUserId: UserId,
        userList: List<User>
    ): UIMessageContent.SystemMessage? {
        val sender = userList.findUser(userId = senderUserId)
        val isAuthorSelfAction = content.members.size == 1 && senderUserId == content.members.first()
        val isSelfTriggered = sender is SelfUser
        val authorName = mapMemberName(user = sender, type = SelfNameType.ResourceTitleCase)
        val memberNameList = content.members.map {
            mapMemberName(
                user = userList.findUser(userId = it),
                type = SelfNameType.ResourceLowercase
            )
        }
        return when (content) {
            is Added ->
                if (isAuthorSelfAction) {
                    UIMessageContent.SystemMessage.MemberJoined(author = authorName, isSelfTriggered = isSelfTriggered)
                } else {
                    UIMessageContent.SystemMessage.MemberAdded(
                        author = authorName,
                        memberNames = memberNameList,
                        isSelfTriggered = isSelfTriggered
                    )
                }

            is Removed ->
                if (isAuthorSelfAction) {
                    UIMessageContent.SystemMessage.MemberLeft(author = authorName, isSelfTriggered = isSelfTriggered)
                } else {
                    UIMessageContent.SystemMessage.MemberRemoved(
                        author = authorName,
                        memberNames = memberNameList,
                        isSelfTriggered = isSelfTriggered
                    )
                }

            is CreationAdded -> {
                UIMessageContent.SystemMessage.ConversationStartedWithMembers(memberNames = memberNameList)
            }

            is FailedToAdd ->
                UIMessageContent.SystemMessage.MemberFailedToAdd(mapFailedToAddUsersByDomain(content.members, userList))
        }
    }

    private fun mapFailedToAddUsersByDomain(members: List<UserId>, userList: List<User>): Map<String, List<UIText>> {
        val memberNameList = members.groupBy { it.domain }.mapValues {
            it.value.map { userId ->
                mapMemberName(
                    user = userList.findUser(userId = userId),
                    type = SelfNameType.ResourceLowercase
                )
            }
        }
        return memberNameList
    }

    private fun mapConversationHistoryLost(): UIMessageContent.SystemMessage = UIMessageContent.SystemMessage.HistoryLost()
    private fun mapMLSWrongEpochWarning(): UIMessageContent.SystemMessage = UIMessageContent.SystemMessage.MLSWrongEpochWarning()
    fun mapMemberName(user: User?, type: SelfNameType = SelfNameType.NameOrDeleted): UIText = when (user) {
        is OtherUser -> user.name?.let { UIText.DynamicString(it) } ?: UIText.StringResource(messageResourceProvider.memberNameDeleted)
        is SelfUser -> when (type) {
            SelfNameType.ResourceLowercase -> UIText.StringResource(messageResourceProvider.memberNameYouLowercase)
            SelfNameType.ResourceTitleCase -> UIText.StringResource(messageResourceProvider.memberNameYouTitlecase)
            SelfNameType.NameOrDeleted -> user.name?.let { UIText.DynamicString(it) }
                ?: UIText.StringResource(messageResourceProvider.memberNameDeleted)
        }

        else -> UIText.StringResource(messageResourceProvider.memberNameDeleted)
    }

    enum class SelfNameType {
        ResourceLowercase, ResourceTitleCase, NameOrDeleted
    }
}
