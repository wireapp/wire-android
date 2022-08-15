package com.wire.android.ui.userprofile.other

import com.wire.android.model.PreservedState
import com.wire.android.ui.userprofile.group.RemoveConversationMemberState
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.Job

interface OtherUserProfileEventsHandler {
    fun onSendConnectionRequest()
    fun onOpenConversation()
    fun onCancelConnectionRequest()
    fun onAcceptConnectionRequest()
    fun onIgnoreConnectionRequest()
    fun navigateBack(): Job
    fun onChangeMemberRole(role: Member.Role)
    fun hideBlockUserDialog()
    fun showBlockUserDialog(userId: UserId, userName: String)
    fun onBlockUser(userId: UserId, userName: String)
    fun onMutingConversationStatusChange(conversationId: ConversationId?, status: MutedConversationStatus)
    fun onAddConversationToFavourites(conversationId: ConversationId)
    fun onMoveConversationToFolder(conversationId: ConversationId)
    fun onMoveConversationToArchive(conversationId: ConversationId)
    fun onClearConversationContent(conversationId: ConversationId)
    fun setBottomSheetStateToConversation()
    fun setBottomSheetStateToMuteOptions()
    fun setBottomSheetStateToChangeRole()
    fun showRemoveConversationMemberDialog()
    fun hideRemoveConversationMemberDialog()
    fun onRemoveConversationMember(state: PreservedState<RemoveConversationMemberState>)
    fun hideUnblockUserDialog()
    fun showUnblockUserDialog(userName: String)
    fun onUnblockUser(userId: UserId)
    suspend fun showInfoMessage(type: InfoMessageType)

    companion object {
        val PREVIEW = object : OtherUserProfileEventsHandler {
            override fun onSendConnectionRequest() {}
            override fun onOpenConversation() {}
            override fun onCancelConnectionRequest() {}
            override fun onAcceptConnectionRequest() {}
            override fun onIgnoreConnectionRequest() {}
            override fun navigateBack(): Job {
                TODO()
            }
            override fun onChangeMemberRole(role: Member.Role) {}
            override fun hideBlockUserDialog() {}
            override fun showBlockUserDialog(userId: UserId, userName: String) {}
            override fun onBlockUser(userId: UserId, userName: String) {}
            override fun onMutingConversationStatusChange(conversationId: ConversationId?, status: MutedConversationStatus) {}
            override fun onAddConversationToFavourites(conversationId: ConversationId) {}
            override fun onMoveConversationToFolder(conversationId: ConversationId) {}
            override fun onMoveConversationToArchive(conversationId: ConversationId) {}
            override fun onClearConversationContent(conversationId: ConversationId) {}
            override fun setBottomSheetStateToConversation() {}
            override fun setBottomSheetStateToMuteOptions() {}
            override fun setBottomSheetStateToChangeRole() {}
            override fun showRemoveConversationMemberDialog() {}
            override fun hideRemoveConversationMemberDialog() {}
            override fun onRemoveConversationMember(state: PreservedState<RemoveConversationMemberState>) {}
            override fun hideUnblockUserDialog() {}
            override fun showUnblockUserDialog(userName: String) {}
            override fun onUnblockUser(userId: UserId) {}
            override suspend fun showInfoMessage(type: InfoMessageType) {}
        }
    }
}
