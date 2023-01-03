package com.wire.android.ui.userprofile.other

import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.userprofile.group.RemoveConversationMemberState
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.Job

@Suppress("TooManyFunctions")
interface OtherUserProfileEventsHandler {
    fun navigateBack(): Job
    fun onBlockUser(blockUserState: BlockUserDialogState)
    fun onRemoveConversationMember(state: RemoveConversationMemberState)
    fun onUnblockUser(userId: UserId)
    fun getOtherUserClients()

    companion object {
        @Suppress("TooManyFunctions")
        val PREVIEW = object : OtherUserProfileEventsHandler {
            override fun onBlockUser(blockUserState: BlockUserDialogState) {}
            override fun onRemoveConversationMember(state: RemoveConversationMemberState) {}
            override fun onUnblockUser(userId: UserId) {}
            override fun getOtherUserClients() {}
            override fun navigateBack(): Job {
                TODO()
            }
        }
    }
}

interface OtherUserProfileFooterEventsHandler {
    fun onSendConnectionRequest()
    fun onOpenConversation()
    fun onCancelConnectionRequest()
    fun onAcceptConnectionRequest()
    fun onIgnoreConnectionRequest()

    companion object {
        @Suppress("TooManyFunctions")
        val PREVIEW = object : OtherUserProfileFooterEventsHandler {
            override fun onSendConnectionRequest() {}
            override fun onOpenConversation() {}
            override fun onCancelConnectionRequest() {}
            override fun onAcceptConnectionRequest() {}
            override fun onIgnoreConnectionRequest() {}

        }
    }
}

@Suppress("TooManyFunctions")
interface OtherUserProfileBottomSheetEventsHandler {
    fun onChangeMemberRole(role: Conversation.Member.Role)
    fun onMutingConversationStatusChange(conversationId: ConversationId?, status: MutedConversationStatus)
    fun onAddConversationToFavourites(conversationId: ConversationId? = null)
    fun onMoveConversationToFolder(conversationId: ConversationId? = null)
    fun onMoveConversationToArchive(conversationId: ConversationId? = null)
    fun onClearConversationContent(dialogState: DialogState)
    fun loadConversationBottomSheetContent()

    companion object {
        @Suppress("TooManyFunctions")
        val PREVIEW = object : OtherUserProfileBottomSheetEventsHandler {
            override fun onChangeMemberRole(role: Conversation.Member.Role) {}
            override fun onMutingConversationStatusChange(conversationId: ConversationId?, status: MutedConversationStatus) {}
            override fun onAddConversationToFavourites(conversationId: ConversationId?) {}
            override fun onMoveConversationToFolder(conversationId: ConversationId?) {}
            override fun onMoveConversationToArchive(conversationId: ConversationId?) {}
            override fun onClearConversationContent(dialogState: DialogState) {}
            override fun loadConversationBottomSheetContent() {}
        }
    }
}
