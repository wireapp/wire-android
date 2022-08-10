package com.wire.android.ui.userprofile.other

import androidx.compose.material.ExperimentalMaterialApi
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
    val connectionState: ConnectionState = ConnectionState.NOT_CONNECTED,
    val membership: Membership = Membership.None,
    val groupState: OtherUserProfileGroupState? = null,
    val botService: BotService? = null,
    val blockUserDialogState: BlockUserDialogState? = null,
    private val conversationSheetContent: ConversationSheetContent? = null,
    val bottomSheetContentState: BottomSheetContent? = null
) {

    fun setBottomSheetStateToConversation(): OtherUserProfileState =
        conversationSheetContent?.let { copy(bottomSheetContentState = BottomSheetContent.Conversation(it)) } ?: this

    fun setBottomSheetStateToMuteOptions(): OtherUserProfileState =
        conversationSheetContent?.let { copy(bottomSheetContentState = BottomSheetContent.Mute(it)) } ?: this

    fun setBottomSheetStateToChangeRole(): OtherUserProfileState =
        groupState?.let { copy(bottomSheetContentState = BottomSheetContent.ChangeRole(it)) } ?: this

    fun clearBottomSheetState(): OtherUserProfileState =
        copy(bottomSheetContentState = null)

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

sealed class BottomSheetContent {
    data class Conversation(val conversationData: ConversationSheetContent) : BottomSheetContent()
    data class Mute(val conversationData: ConversationSheetContent) : BottomSheetContent()
    data class ChangeRole(val groupState: OtherUserProfileGroupState?) : BottomSheetContent()
}

data class OtherUserProfileGroupState(
    val groupName: String,
    val role: Member.Role,
    val isSelfAnAdmin: Boolean
)
