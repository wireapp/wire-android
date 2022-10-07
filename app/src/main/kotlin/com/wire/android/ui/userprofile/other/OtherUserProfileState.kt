package com.wire.android.ui.userprofile.other

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
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
    val conversationSheetContent: ConversationSheetContent? = null,
    val otherUserClients: List<OtherUserClient> = listOf()
) {
    fun updateMuteStatus(status: MutedConversationStatus): OtherUserProfileState {
        return conversationSheetContent?.let {
            val newConversationSheetContent = conversationSheetContent.copy(mutingConversationState = status)
            copy(conversationSheetContent = newConversationSheetContent)
        } ?: this
    }

    companion object {
        val PREVIEW = OtherUserProfileState(
            userId = UserId("some_user", "domain.com"),
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
