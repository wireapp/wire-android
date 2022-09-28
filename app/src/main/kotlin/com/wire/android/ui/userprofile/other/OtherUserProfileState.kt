package com.wire.android.ui.userprofile.other

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationTypeDetail
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.client.OtherUserClient
import com.wire.kalium.logic.data.conversation.Conversation.Member
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.BotService
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId


data class OtherUserProfileState(
    val userId: UserId,
    val conversationId: ConversationId? = null,
    val userAvatarAsset: UserAvatarAsset? = null,
    val isLoading: Boolean = true,
    val fullName: String = "",
    val userName: String = "",
    val teamName: String = "",
    val email: String = "",
    val phone: String = "",
    val connectionState: ConnectionState = ConnectionState.NOT_CONNECTED,
    val membership: Membership = Membership.None,
    val groupInfoAvailability: GroupInfoAvailibility = GroupInfoAvailibility.NotAvailable,
    val conversationDetailOnDemand: ConversationDetailOnDemand = ConversationDetailOnDemand.NotRequested,
    val botService: BotService? = null,
    val otherUserClients: List<OtherUserClient> = listOf()
) {
    fun setBottomSheetStateToConversation(): OtherUserProfileState =
        conversationSheetContent?.let { copy(bottomSheetContentState = OtherUserBottomSheetContent.Conversation(it)) } ?: this

    fun setBottomSheetStateToMuteOptions(): OtherUserProfileState =
        conversationSheetContent?.let { copy(bottomSheetContentState = OtherUserBottomSheetContent.Mute(it)) } ?: this

    fun setBottomSheetStateToChangeRole(): OtherUserProfileState =
        groupInfoAvailability?.let { copy(bottomSheetContentState = OtherUserBottomSheetContent.ChangeRole(it)) } ?: this

    fun updateMuteStatus(status: MutedConversationStatus): OtherUserProfileState {
        return conversationSheetContent?.let {
            val newConversationSheetContent = conversationSheetContent.copy(mutingConversationState = status)
            val newBottomSheetContentState = when (bottomSheetContentState) {
                is OtherUserBottomSheetContent.Mute -> bottomSheetContentState.copy(
                    conversationData = bottomSheetContentState.conversationData.copy(mutingConversationState = status)
                )
                is OtherUserBottomSheetContent.Conversation -> bottomSheetContentState.copy(
                    conversationData = bottomSheetContentState.conversationData.copy(mutingConversationState = status)
                )
                is OtherUserBottomSheetContent.ChangeRole -> bottomSheetContentState
                null -> null
            }
            copy(conversationSheetContent = newConversationSheetContent, bottomSheetContentState = newBottomSheetContentState)
        } ?: this
    }

    fun clearBottomSheetState(): OtherUserProfileState =
        copy(bottomSheetContentState = null)

    companion object {
        val PREVIEW = OtherUserProfileState(
            userId = UserId("some_user", "domain.com"),
            fullName = "name",
            userName = "username",
            teamName = "team",
            email = "email",
            groupInfoAvailability = OtherUserProfileGroupInfo(
                "group name", Member.Role.Member, true, ConversationId("some_user", "domain.com")
            )
        )
    }
}

sealed class BottomSheetContent {
    data class Conversation(val conversationData: ConversationSheetContent) : BottomSheetContent()
    data class Mute(val conversationData: ConversationSheetContent) : BottomSheetContent()
    data class ChangeRole(val otherUserProfileGroupInfo: OtherUserProfileGroupInfo) : BottomSheetContent()
}

data class OtherUserProfileGroupInfo(
    val groupName: String,
    val role: Member.Role,
    val isSelfAdmin: Boolean,
    val conversationId: ConversationId
)

sealed class GroupInfoAvailibility {
    object NotAvailable : GroupInfoAvailibility()
    data class Available(val otherUserProfileGroupInfo: OtherUserProfileGroupInfo) : GroupInfoAvailibility()
}

sealed class ConversationDetailOnDemand {
    object NotRequested : ConversationDetailOnDemand()
    data class Requested(
        val title: String,
        val conversationId: ConversationId,
        val mutingConversationState: MutedConversationStatus,
        val conversationTypeDetail: ConversationTypeDetail,
        val isSelfUserMember: Boolean = true
    ) : ConversationDetailOnDemand()
}
