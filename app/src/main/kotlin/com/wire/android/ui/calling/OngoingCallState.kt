package com.wire.android.ui.calling

import com.wire.android.model.ImageAsset.UserAvatarAsset

data class OngoingCallState(
    val conversationName: String? = null,
    val avatarAssetId: UserAvatarAsset? = null,
    val isMuted: Boolean = false,
    val isCameraOn: Boolean = false,
    val isSpeakerOn: Boolean = false
)
