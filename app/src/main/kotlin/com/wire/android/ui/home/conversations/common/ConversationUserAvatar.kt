package com.wire.android.ui.home.conversations.common

import androidx.compose.runtime.Composable
import com.wire.android.model.UserStatus
import com.wire.android.ui.common.UserProfileAvatar

@Composable
fun ConversationUserAvatar(avatarUrl: String) {
    UserProfileAvatar(avatarUrl = avatarUrl, status = UserStatus.AVAILABLE)
}
