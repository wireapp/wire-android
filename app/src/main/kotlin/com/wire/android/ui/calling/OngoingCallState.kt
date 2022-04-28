package com.wire.android.ui.calling

data class OngoingCallState(
    val conversationName: String = "default",
    val avatarAssetByteArray: ByteArray? = null,
    val isMuted: Boolean = false,
    val isCameraOn: Boolean = false,
    val isSpeakerOn: Boolean = false
)
