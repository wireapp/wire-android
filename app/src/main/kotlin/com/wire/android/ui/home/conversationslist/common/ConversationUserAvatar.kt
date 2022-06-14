package com.wire.android.ui.home.conversationslist.common

import androidx.compose.runtime.Composable
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.UserProfileAvatar

@Composable
fun ConversationUserAvatar(avatarData: UserAvatarData) {
    UserProfileAvatar(avatarData)
}
