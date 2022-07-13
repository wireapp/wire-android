package com.wire.android.mapper

import com.wire.android.ui.home.conversations.avatar
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.data.user.type.UserType
import javax.inject.Inject

class UIParticipantMapper @Inject constructor(
    private val userTypeMapper: UserTypeMapper,
    private val wireSessionImageLoader: WireSessionImageLoader
    ) {
    fun toUIParticipant(user: User): UIParticipant = with(user) {
        val userType = when(this) {
            is OtherUser -> this.userType
            // TODO(refactor): does self user need a type ?
            is SelfUser -> UserType.INTERNAL
        }
        UIParticipant(
            id = id,
            name = name.orEmpty(),
            handle = handle.orEmpty(),
            avatarData = avatar(wireSessionImageLoader),
            isSelf = user is SelfUser,
            membership = userTypeMapper.toMembership(userType)
        )
    }
}
