package com.wire.android.ui.userprofile.other

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.conversation.Member
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
    val botService: BotService? = null,
    val blockUserDialogSate: BlockUserDialogState? = null,
    private val conversationSheetContent: ConversationSheetContent? = null
) {

    private val startingBottomSheetContent = conversationSheetContent?.let { BottomSheetState.Conversation(it) }
    var bottomSheetState: BottomSheetState? by mutableStateOf(startingBottomSheetContent)
        private set

    fun setBottomSheetContentToConversation() {
        conversationSheetContent?.let {
            bottomSheetState = BottomSheetState.Conversation(it)
        }
    }

    fun setBottomSheetContentToChangeRole() {
        groupState?.let {
            bottomSheetState = BottomSheetState.ChangeRole(it)
        }
    }

    companion object {
        val PREVIEW = OtherUserProfileState(
            fullName = "name",
            userName = "username",
            teamName = "team",
            email = "email",
            groupState = OtherUserProfileGroupState("group name", Member.Role.Member, true)
        )
    }
}

sealed class BottomSheetState {
    data class Conversation(val conversationData: ConversationSheetContent) : BottomSheetState()
    data class ChangeRole(val groupState: OtherUserProfileGroupState?) : BottomSheetState()
}

data class OtherUserProfileGroupState(
    val groupName: String,
    val role: Member.Role,
    val isSelfAnAdmin: Boolean
)
