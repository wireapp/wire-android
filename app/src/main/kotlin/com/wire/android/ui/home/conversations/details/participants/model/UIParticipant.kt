package com.wire.android.ui.home.conversations.details.participants.model

import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.user.UserId

data class UIParticipant(
    val id: UserId,
    val name: String,
    val handle: String,
    val isSelf: Boolean,
    val avatarData: UserAvatarData = UserAvatarData(),
    val membership: Membership = Membership.None,
)
