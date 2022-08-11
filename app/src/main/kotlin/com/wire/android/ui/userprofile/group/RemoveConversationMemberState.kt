package com.wire.android.ui.userprofile.group

import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.id.QualifiedID as ConversationId

data class RemoveConversationMemberState(
    val conversationId: ConversationId,
    val fullName: String,
    val userName: String,
    val userId: UserId
)
