package com.wire.android.model

import com.wire.kalium.logic.data.user.UserAvailabilityStatus

data class UserAvatarData(
    val asset: ImageAsset.UserAvatarAsset? = null,
    val availabilityStatus: UserAvailabilityStatus = UserAvailabilityStatus.NONE,
)
