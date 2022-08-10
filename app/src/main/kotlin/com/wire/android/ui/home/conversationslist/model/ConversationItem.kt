package com.wire.android.ui.home.conversationslist.model

import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.common.UserInfoLabel
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId

sealed class ConversationItem {
    abstract val conversationId: ConversationId
    abstract val mutedStatus: MutedConversationStatus
    abstract val isLegalHold: Boolean
    abstract val lastEvent: ConversationLastEvent

    data class GroupConversation(
        val groupName: String,
        override val conversationId: ConversationId,
        override val mutedStatus: MutedConversationStatus,
        override val isLegalHold: Boolean = false,
        override val lastEvent: ConversationLastEvent,
        val unreadMessagesCount: Long = 0,
        val hasOnGoingCall: Boolean = false
    ) : ConversationItem()

    data class PrivateConversation(
        val userAvatarData: UserAvatarData,
        val conversationInfo: ConversationInfo,
        val userId: UserId,
        val blockingState: BlockingState,
        val connectionState: ConnectionState,
        override val conversationId: ConversationId,
        override val mutedStatus: MutedConversationStatus,
        override val isLegalHold: Boolean = false,
        override val lastEvent: ConversationLastEvent,
        val unreadMessagesCount: Long = 0
    ) : ConversationItem()

    data class ConnectionConversation(
        val userAvatarData: UserAvatarData,
        val conversationInfo: ConversationInfo,
        val connectionState: ConnectionState,
        override val conversationId: ConversationId,
        override val mutedStatus: MutedConversationStatus,
        override val isLegalHold: Boolean = false,
        override val lastEvent: ConversationLastEvent
    ) : ConversationItem()
}

data class ConversationInfo(
    val name: String,
    val membership: Membership = Membership.None
)

enum class BlockingState {
    CAN_NOT_BE_BLOCKED, // we should not be able to block our own team-members
    BLOCKED,
    NOT_BLOCKED
}

fun ConversationItem.PrivateConversation.toUserInfoLabel() =
    UserInfoLabel(
        labelName = conversationInfo.name,
        isLegalHold = isLegalHold,
        membership = conversationInfo.membership
    )

fun ConversationItem.ConnectionConversation.toUserInfoLabel() =
    UserInfoLabel(
        labelName = conversationInfo.name,
        isLegalHold = isLegalHold,
        membership = conversationInfo.membership
    )
