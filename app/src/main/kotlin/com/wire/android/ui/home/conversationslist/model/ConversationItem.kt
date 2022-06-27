package com.wire.android.ui.home.conversationslist.model

import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.common.UserInfoLabel
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId

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
        val hasOnGoingCall: Boolean = false
    ) : ConversationItem()

    data class PrivateConversation(
        val userAvatarData: UserAvatarData,
        val conversationInfo: ConversationInfo,
        override val conversationId: ConversationId,
        override val mutedStatus: MutedConversationStatus,
        override val isLegalHold: Boolean = false,
        override val lastEvent: ConversationLastEvent
    ) : ConversationItem()

    data class ConnectionConversation(
        val userAvatarData: UserAvatarData,
        val conversationInfo: ConversationInfo,
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
