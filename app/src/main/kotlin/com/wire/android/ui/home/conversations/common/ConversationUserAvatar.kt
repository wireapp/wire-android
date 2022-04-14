package com.wire.android.ui.home.conversations.common

import androidx.compose.runtime.Composable
import com.wire.android.model.UserAvatarAsset
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.UserProfileAvatar

@Composable
fun ConversationUserAvatar(avatarAsset: UserAvatarAsset?) {
    UserProfileAvatar(userAvatarAsset = avatarAsset, status = UserStatus.AVAILABLE)
}
