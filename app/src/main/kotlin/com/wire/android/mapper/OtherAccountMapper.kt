package com.wire.android.mapper

import com.wire.android.ui.home.conversations.avatar
import com.wire.android.ui.userprofile.self.model.OtherAccount
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.SelfUser
import javax.inject.Inject

class OtherAccountMapper @Inject constructor(
    private val wireSessionImageLoader: WireSessionImageLoader
) {
    fun toOtherAccount(selfUser: SelfUser, team: Team?): OtherAccount = OtherAccount(
        id = selfUser.id,
        fullName = selfUser.name ?: "",
        avatarData = selfUser.avatar(wireSessionImageLoader, selfUser.connectionStatus),
        teamName = team?.name
    )
}
