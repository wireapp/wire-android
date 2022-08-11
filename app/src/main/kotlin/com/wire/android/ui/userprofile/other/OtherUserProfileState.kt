package com.wire.android.ui.userprofile.other

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.BotService
import com.wire.kalium.logic.data.user.ConnectionState

@OptIn(ExperimentalMaterialApi::class)
data class OtherUserProfileState(
    val userAvatarAsset: UserAvatarAsset? = null,
    val isDataLoading: Boolean = false,
    val isAvatarLoading: Boolean = false,
    val fullName: String = "",
    val userName: String = "",
    val teamName: String = "",
    val email: String = "",
    val phone: String = "",
    val connectionStatus: ConnectionState = ConnectionState.NOT_CONNECTED,
    val membership: Membership = Membership.None,
    val groupState: OtherUserProfileGroupState? = null,
    val bottomSheetState: ModalBottomSheetValue = ModalBottomSheetValue.Hidden,
    val botService: BotService? = null
) {
    companion object {
        val PREVIEW = OtherUserProfileState(
            fullName = "name",
            userName = "username",
            teamName = "team",
            email = "email",
            groupState = OtherUserProfileGroupState(
                "group name", Member.Role.Member, true, ConversationId("some_user", "domain.com")
            )
        )
    }
}

data class OtherUserProfileGroupState(
    val groupName: String,
    val role: Member.Role,
    val isSelfAdmin: Boolean,
    val conversationId: ConversationId
)

sealed class ConnectionStatus {
    object Unknown : ConnectionStatus()
    object Connected : ConnectionStatus()
    object Pending : ConnectionStatus()
    object Sent : ConnectionStatus()
    object NotConnected : ConnectionStatus()
}

fun ConnectionState.toOtherUserProfileConnectionStatus() = when (this) {
    ConnectionState.NOT_CONNECTED -> ConnectionStatus.NotConnected
    ConnectionState.CANCELLED -> ConnectionStatus.NotConnected
    ConnectionState.PENDING -> ConnectionStatus.Pending
    ConnectionState.SENT -> ConnectionStatus.Sent
    ConnectionState.ACCEPTED -> ConnectionStatus.Connected
    ConnectionState.BLOCKED,
    ConnectionState.IGNORED,
    ConnectionState.MISSING_LEGALHOLD_CONSENT -> ConnectionStatus.Unknown // TODO: implement rest states
}
