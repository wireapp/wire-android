package com.wire.android.ui.userprofile.other

import MutingOptionsSheetContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationMainSheetContent

@Composable
fun OtherUserProfileBottomSheetContent(
    bottomSheetState: BottomSheetContent?,
    eventsHandler: OtherUserProfileBottomSheetEventsHandler,
    blockUser: (BlockUserDialogState) -> Unit,
    closeBottomSheet: () -> Unit
) {
    when (bottomSheetState) {
        is BottomSheetContent.Conversation -> {
            val conversationId = bottomSheetState.conversationData.conversationId
            ConversationMainSheetContent(
                conversationSheetContent = bottomSheetState.conversationData,
                addConversationToFavourites = { eventsHandler.onAddConversationToFavourites(conversationId) },
                moveConversationToFolder = { eventsHandler.onMoveConversationToFolder(conversationId) },
                moveConversationToArchive = { eventsHandler.onMoveConversationToArchive(conversationId) },
                clearConversationContent = { eventsHandler.onClearConversationContent(conversationId) },
                blockUserClick = blockUser,
                leaveGroup = { },
                deleteGroup = { },
                navigateToNotification = eventsHandler::setBottomSheetStateToMuteOptions
            )
        }
        is BottomSheetContent.Mute ->
            MutingOptionsSheetContent(
                mutingConversationState = bottomSheetState.conversationData.mutingConversationState,
                onMuteConversation = {
                    eventsHandler.onMutingConversationStatusChange(bottomSheetState.conversationData.conversationId, it)
                },
                onBackClick = eventsHandler::setBottomSheetStateToConversation
            )
        is BottomSheetContent.ChangeRole ->
            EditGroupRoleBottomSheet(
                groupState = bottomSheetState.groupState,
                changeMemberRole = eventsHandler::onChangeMemberRole,
                closeChangeRoleBottomSheet = closeBottomSheet
            )
        null -> {}
    }

    BackHandler(bottomSheetState != null) {
        if (bottomSheetState is BottomSheetContent.Mute) eventsHandler.setBottomSheetStateToConversation()
        else closeBottomSheet()
    }
}
