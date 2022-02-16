package com.wire.android.ui.home.conversationslist.model

import com.wire.android.model.UserStatus
import com.wire.android.ui.main.conversationlist.common.UserInfoLabel
import com.wire.kalium.logic.data.conversation.ConversationId

sealed class ConversationItem(val conversationType: ConversationType) {
    val id = conversationType.conversationsId
}

class GeneralConversation(conversationType: ConversationType) : ConversationItem(conversationType)
class ConversationMissedCall(val callInfo: CallInfo, conversationType: ConversationType) : ConversationItem(conversationType)
class ConversationUnreadMention(val mentionInfo: MentionInfo, conversationType: ConversationType) : ConversationItem(conversationType)

sealed class ConversationType {
    abstract val conversationsId: ConversationId

    data class GroupConversation(
        val groupColorValue: Long,
        val groupName: String,
        override val conversationsId: ConversationId
    ) : ConversationType()

    data class PrivateConversation(
        val userInfo: UserInfo,
        val conversationInfo: ConversationInfo,
        override val conversationsId: ConversationId
    ) : ConversationType()
}

data class ConversationInfo(
    val name: String,
    val membership: Membership = Membership.None,
    val isLegalHold: Boolean = false
)

data class UserInfo(
    val avatarUrl: String = "",
    val availabilityStatus: UserStatus = UserStatus.NONE
)

fun ConversationType.PrivateConversation.toUserInfoLabel() =
    UserInfoLabel(
        labelName = conversationInfo.name,
        isLegalHold = conversationInfo.isLegalHold,
        membership = conversationInfo.membership,
    )
