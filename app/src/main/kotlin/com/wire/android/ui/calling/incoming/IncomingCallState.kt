package com.wire.android.ui.calling.incoming

data class IncomingCallState(
    val conversationName: String? = null,
    val avatarAssetByteArray: ByteArray? = null,
    val isMicrophoneMuted: Boolean = false,
    val isCameraOn: Boolean = false,
    val isSpeakerOn: Boolean = false
)
