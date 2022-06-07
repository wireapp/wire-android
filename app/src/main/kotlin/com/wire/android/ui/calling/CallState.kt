package com.wire.android.ui.calling

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.kalium.logic.data.call.ConversationType

data class CallState(
    val conversationName: ConversationName? = null,
    val avatarAssetId: UserAvatarAsset? = null,
    val isMuted: Boolean = false,
    val isCameraOn: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isCameraFlipped: Boolean = false,
    val conversationType: ConversationType = ConversationType.OneOnOne
)
