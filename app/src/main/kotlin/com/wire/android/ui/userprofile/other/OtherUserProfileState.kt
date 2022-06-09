package com.wire.android.ui.userprofile.other

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.kalium.logic.data.user.ConnectionState

data class OtherUserProfileState(
    val userAvatarAsset: UserAvatarAsset? = null,
    val isDataLoading: Boolean = false,
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
    object Pending: ConnectionStatus()
    object Sent: ConnectionStatus()
    object NotConnected : ConnectionStatus()
}

fun ConnectionState.toOtherUserProfileConnectionStatus() = when (this) {
    ConnectionState.NOT_CONNECTED -> ConnectionStatus.NotConnected
    ConnectionState.CANCELLED -> ConnectionStatus.NotConnected
    ConnectionState.PENDING -> ConnectionStatus.Pending
    ConnectionState.SENT -> ConnectionStatus.Sent
    ConnectionState.ACCEPTED -> ConnectionStatus.Connected
    else -> ConnectionStatus.Unknown // TODO: what about other states?
}
