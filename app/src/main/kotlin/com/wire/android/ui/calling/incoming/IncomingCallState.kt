package com.wire.android.ui.calling.incoming

import com.wire.android.ui.calling.ConversationName

data class IncomingCallState(
    val conversationName: ConversationName? = null,
    val avatarAssetByteArray: ByteArray? = null,
    val isMicrophoneMuted: Boolean = false,
    val isCameraOn: Boolean = false,
    val isSpeakerOn: Boolean = false
)
