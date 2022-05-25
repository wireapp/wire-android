package com.wire.android.ui.calling.incoming

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.calling.ConversationName

data class IncomingCallState(
    val conversationName: ConversationName? = null,
    val avatarAssetId: UserAvatarAsset? = null,
    val isMicrophoneMuted: Boolean = false,
    val isCameraOn: Boolean = false,
    val isSpeakerOn: Boolean = false
)
