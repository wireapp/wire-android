package com.wire.android.ui.calling

data class OngoingCallState(
    val conversationName: String?,
    val avatarAssetByteArray: ByteArray? = null,
    val isMuted: Boolean = true,
    val isCameraOn: Boolean = false,
    val isSpeakerOn: Boolean = false
)
