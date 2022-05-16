package com.wire.android.ui.userprofile.other

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.kalium.logic.data.user.ConnectionState
import java.util.UUID

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
    val errorState: ErrorState? = null
)

sealed class ConnectionStatus {
    object Unknown : ConnectionStatus()
    object Connected : ConnectionStatus()
    data class NotConnected(val isConnectionRequestPending: Boolean = false) : ConnectionStatus()
}

/**
 * We are adding a [randomEventIdentifier] as [UUID], so the error can be discarded every time after being generated.
 */
sealed class ErrorState(@StringRes val messageResId: Int, private val randomEventIdentifier: UUID) {
    class ConnectionRequestError : ErrorState(R.string.connection_request_sent_error, UUID.randomUUID())
    class LoadUserInformationError : ErrorState(R.string.error_unknown_message, UUID.randomUUID())
}

fun ConnectionState.toOtherUserProfileConnectionStatus() = when (this) {
    ConnectionState.NOT_CONNECTED -> ConnectionStatus.NotConnected(false)
    ConnectionState.CANCELLED -> ConnectionStatus.NotConnected(false)
    ConnectionState.PENDING -> ConnectionStatus.NotConnected(true)
    ConnectionState.SENT -> ConnectionStatus.NotConnected(true)
    ConnectionState.ACCEPTED -> ConnectionStatus.Connected
    else -> ConnectionStatus.Unknown // TODO: what about other states?
}
