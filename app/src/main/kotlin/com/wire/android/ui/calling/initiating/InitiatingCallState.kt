package com.wire.android.ui.calling.initiating

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.kalium.logic.data.call.ConversationType

data class InitiatingCallState(
    val conversationName: String? = null,
    val avatarAssetId: UserAvatarAsset? = null,
    val isMuted: Boolean = true,
    val isCameraOn: Boolean = false,
    val isCameraFlipped: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val conversationType: ConversationType = ConversationType.OneOnOne
)
