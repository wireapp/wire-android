package com.wire.android.ui.calling.initiating

import com.wire.android.model.UserAvatarAsset
import com.wire.android.ui.calling.ConversationName
import com.wire.kalium.logic.data.call.ConversationType

data class InitiatingCallState(
    val conversationName: ConversationName? = null,
    val avatarAssetId: UserAvatarAsset? = null,
    val isMuted: Boolean = true,
    val isCameraOn: Boolean = false,
    val isCameraFlipped: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val conversationType: ConversationType = ConversationType.OneOnOne
)
