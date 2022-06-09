package com.wire.android.ui.home.conversationslist.common

import androidx.compose.runtime.Composable
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.android.ui.common.UserProfileAvatar

@Composable
fun ConversationUserAvatar(avatarAsset: UserAvatarAsset?, status: UserAvailabilityStatus) {
    UserProfileAvatar(userAvatarAsset = avatarAsset, status = status)
}
