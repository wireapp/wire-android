package com.wire.android.ui.userprofile.other

data class OtherUserProfileState(
    val avatarAssetByteArray: ByteArray? = null,
    val isDataLoading : Boolean = false,
    val isAvatarLoading: Boolean = false,
    val fullName: String = "",
    val userName: String = "",
    val teamName: String = "",
    val email: String = "",
    val phone: String = "",
    val connectionStatus: ConnectionStatus = ConnectionStatus.Unknown,
)

sealed class ConnectionStatus {
    object Unknown : ConnectionStatus()
    object Connected : ConnectionStatus()
    data class NotConnected(val isConnectionRequestPending: Boolean = false) : ConnectionStatus()
}
