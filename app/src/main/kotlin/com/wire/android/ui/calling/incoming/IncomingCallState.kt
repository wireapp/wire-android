package com.wire.android.ui.calling.incoming

data class IncomingCallState(
    val conversationName: String = "Default Conversation Name",
    val avatarAssetByteArray: ByteArray? = null,
    val isMicrophoneOn: Boolean = true,
    val isCameraOn: Boolean = false,
    val isSpeakerOn: Boolean = false
)
