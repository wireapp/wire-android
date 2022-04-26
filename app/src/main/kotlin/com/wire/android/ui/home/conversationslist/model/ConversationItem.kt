package com.wire.android.ui.home.conversationslist.model

import com.wire.android.model.UserAvatarAsset
import com.wire.android.model.UserStatus
import com.wire.android.ui.main.conversationlist.common.UserInfoLabel
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId

sealed class ConversationItem(val conversationType: ConversationType) {
    val id = conversationType.conversationId
}

class GeneralConversation(conversationType: ConversationType) : ConversationItem(conversationType)
class ConversationMissedCall(val callInfo: CallInfo, conversationType: ConversationType) : ConversationItem(conversationType)
class ConversationUnreadMention(val mentionInfo: MentionInfo, conversationType: ConversationType) : ConversationItem(conversationType)

sealed class ConversationType {
    abstract val conversationId: ConversationId
    abstract val mutedStatus: MutedConversationStatus

    data class GroupConversation(
        val groupColorValue: Long,
        val groupName: String,
        override val conversationId: ConversationId,
        override val mutedStatus: MutedConversationStatus
    ) : ConversationType()

    data class PrivateConversation(
        val userInfo: UserInfo,
        val conversationInfo: ConversationInfo,
        override val conversationId: ConversationId,
        override val mutedStatus: MutedConversationStatus
    ) : ConversationType()
}

data class ConversationInfo(
    val name: String,
    val membership: Membership = Membership.None,
    val isLegalHold: Boolean = false
)

data class UserInfo(
    val avatarAsset: UserAvatarAsset? = null,
    val availabilityStatus: UserStatus = UserStatus.NONE
)

fun ConversationType.PrivateConversation.toUserInfoLabel() =
    UserInfoLabel(
        labelName = conversationInfo.name,
        isLegalHold = conversationInfo.isLegalHold,
        membership = conversationInfo.membership,
    )
