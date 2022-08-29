package com.wire.android.ui.userprofile.self.model

import com.wire.android.model.UserAvatarData
import com.wire.kalium.logic.data.user.UserId


data class OtherAccount(
    val id: UserId,
    val fullName: String,
    val avatarData: UserAvatarData = UserAvatarData(),
    val teamName: String? = null
)
