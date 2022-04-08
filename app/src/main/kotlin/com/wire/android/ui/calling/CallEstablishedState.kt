package com.wire.android.ui.calling

data class CallEstablishedState(
    val conversationName: String = "Default",
    val avatarAssetByteArray: ByteArray? = null,
    val isMuted: Boolean = true,
    val isCameraOn: Boolean = false,
    val isSpeakerOn: Boolean = false
)
