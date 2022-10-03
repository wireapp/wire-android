package com.wire.android.ui.userprofile.other

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.client.OtherUserClient
import com.wire.kalium.logic.data.conversation.Conversation.Member
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
    val conversationSheetContent: ConversationSheetContent? = null,
    val botService: BotService? = null,
    val otherUserClients: List<OtherUserClient> = listOf()
) {
//    companion object {
//        val PREVIEW = OtherUserProfileState(
//            userId = UserId("some_user", "domain.com"),
//            fullName = "name",
//            userName = "username",
//            teamName = "team",
//            email = "email",
//            groupInfoAvailability = OtherUserProfileGroupInfo(
//                "group name", Member.Role.Member, true, ConversationId("some_user", "domain.com")
//            )
//        )
//    }
}

data class OtherUserProfileGroupInfo(
    val conversationId: ConversationId,
    val groupName: String,
    val role: Member.Role,
    val isSelfAdmin: Boolean
)

sealed class GroupInfoAvailibility {
    object NotAvailable : GroupInfoAvailibility()
    data class Available(val otherUserProfileGroupInfo: OtherUserProfileGroupInfo) : GroupInfoAvailibility()
}
