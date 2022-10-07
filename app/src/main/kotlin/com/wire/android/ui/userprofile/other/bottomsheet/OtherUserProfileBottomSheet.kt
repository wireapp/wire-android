package com.wire.android.ui.userprofile.other.bottomsheet

import androidx.compose.runtime.Composable
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.common.bottomsheet.conversation.rememberConversationSheetState
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.userprofile.other.OtherUserProfileBottomSheetEventsHandler

@Composable
fun OtherUserProfileBottomSheetContent(
    bottomSheetState: OtherUserBottomSheetState,
    eventsHandler: OtherUserProfileBottomSheetEventsHandler,
    blockUser: (BlockUserDialogState) -> Unit,
    unblockUser: (UnblockUserDialogState) -> Unit,
    closeBottomSheet: () -> Unit,
    getBottomSheetVisibility: () -> Boolean
) {
    when (val state = bottomSheetState.bottomSheetContentState) {
        is BottomSheetContent.Conversation -> {
            val conversationSheetState = rememberConversationSheetState(state.conversationData)
            ConversationSheetContent(
                isBottomSheetVisible = getBottomSheetVisibility,
                conversationSheetState = conversationSheetState,
                onMutingConversationStatusChange = {
                    val mutedConversationStatus = conversationSheetState.conversationSheetContent!!.mutingConversationState
                    eventsHandler.onMutingConversationStatusChange(
                        conversationSheetState.conversationId,
                        mutedConversationStatus
                    )
                    bottomSheetState.muteConversation(mutedConversationStatus)
                },
                addConversationToFavourites = eventsHandler::onAddConversationToFavourites,
                moveConversationToFolder = eventsHandler::onMoveConversationToFolder,
                moveConversationToArchive = eventsHandler::onMoveConversationToArchive,
                clearConversationContent = eventsHandler::onClearConversationContent,
                blockUser = blockUser,
                unblockUser = unblockUser,
                leaveGroup = { },
                deleteGroup = { }
            )
        }
        is BottomSheetContent.ChangeRole ->
            EditGroupRoleBottomSheet(
                groupState = state.groupState,
                changeMemberRole = eventsHandler::onChangeMemberRole,
                closeChangeRoleBottomSheet = closeBottomSheet
            )
        null -> {}
    }
}
