package com.wire.android.mapper

import com.wire.android.ui.home.conversations.avatar
import com.wire.android.ui.home.conversations.handle
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.name
import com.wire.android.ui.home.conversations.userId
import com.wire.android.ui.home.conversations.userType
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.MemberDetails
import javax.inject.Inject

class UIParticipantMapper @Inject constructor(
    private val userTypeMapper: UserTypeMapper,
    private val wireSessionImageLoader: WireSessionImageLoader
    ) {
    fun toUIParticipant(memberDetails: MemberDetails): UIParticipant = UIParticipant(
        id = memberDetails.userId,
        name = memberDetails.name.orEmpty(),
        handle = memberDetails.handle.orEmpty(),
        avatarData = memberDetails.avatar(wireSessionImageLoader),
        isSelf = memberDetails is MemberDetails.Self,
        membership = userTypeMapper.toMembership(memberDetails.userType)
    )
}
