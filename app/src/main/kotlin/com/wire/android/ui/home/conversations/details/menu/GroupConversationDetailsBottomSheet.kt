package com.wire.android.ui.home.conversations.details.menu

import MutingOptionsSheetContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.wire.android.ui.home.conversations.details.options.BottomSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationMainSheetContent
import com.wire.android.ui.home.conversationslist.model.GroupDialogState

@Composable
fun GroupConversationDetailsBottomSheetContent(
    bottomSheetState: BottomSheetContent?,
    eventsHandler: GroupConversationDetailsBottomSheetEventsHandler,
    deleteGroup: (GroupDialogState) -> Unit,
    leaveGroup: (GroupDialogState) -> Unit,
    closeBottomSheet: () -> Unit
) {
    when (bottomSheetState) {
        is BottomSheetContent.Conversation -> {
            val conversationId = bottomSheetState.conversationData.conversationId
            ConversationMainSheetContent(
                conversationSheetContent = bottomSheetState.conversationData,
// TODO(profile): enable when implemented
//
//                addConversationToFavourites = { eventsHandler.onAddConversationToFavourites(conversationId) },
//                moveConversationToFolder = { eventsHandler.onMoveConversationToFolder(conversationId) },
//                moveConversationToArchive = { eventsHandler.onMoveConversationToArchive(conversationId) },
//                clearConversationContent = { eventsHandler.onClearConversationContent(conversationId) },
                blockUserClick = {},
                leaveGroup = leaveGroup,
                deleteGroup = deleteGroup,
                navigateToNotification = eventsHandler::setBottomSheetStateToMuteOptions,
                unblockUserClick = {}
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
        null -> {}
    }

    BackHandler(bottomSheetState != null) {
        if (bottomSheetState is BottomSheetContent.Mute) eventsHandler.setBottomSheetStateToConversation()
        else closeBottomSheet()
    }
}
