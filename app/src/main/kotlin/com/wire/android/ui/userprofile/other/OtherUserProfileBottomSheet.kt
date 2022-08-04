package com.wire.android.ui.userprofile.other

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationOptionNavigation
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.rememberConversationSheetState
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.id.ConversationId

fun getBottomSheetContent(
    bottomSheetState: BottomSheetState?,
    onMutingConversationStatusChange: (ConversationId?, MutedConversationStatus) -> Unit,
    addConversationToFavourites: () -> Unit,
    moveConversationToFolder: () -> Unit,
    moveConversationToArchive: () -> Unit,
    clearConversationContent: () -> Unit,
    blockUser: (UserId, String) -> Unit
): @Composable (ColumnScope.() -> Unit) =
    when (bottomSheetState) {
        is BottomSheetState.Conversation -> conversationBottomSheet(
            bottomSheetState.content,
            onMutingConversationStatusChange,
            addConversationToFavourites,
            moveConversationToFolder,
            moveConversationToArchive,
            clearConversationContent,
            blockUser
        )
        is BottomSheetState.ChangeRole, //TODO
        null -> {
            {}
        }
    }

private fun conversationBottomSheet(
    conversationSheetContent: ConversationSheetContent,
    onMutingConversationStatusChange: (ConversationId?, MutedConversationStatus) -> Unit,
    addConversationToFavourites: () -> Unit,
    moveConversationToFolder: () -> Unit,
    moveConversationToArchive: () -> Unit,
    clearConversationContent: () -> Unit,
    blockUser: (UserId, String) -> Unit
): @Composable (ColumnScope.() -> Unit) = {
    val conversationOptionNavigation = ConversationOptionNavigation.Home
    val conversationState = rememberConversationSheetState(
        conversationSheetContent = conversationSheetContent,
        conversationOptionNavigation = conversationOptionNavigation
    )

    ConversationSheetContent(
        conversationSheetState = conversationState,
        // FIXME: Compose - Find a way to not recreate this lambda
        onMutingConversationStatusChange = { mutedStatus ->
            conversationState.muteConversation(mutedStatus)
            onMutingConversationStatusChange(conversationState.conversationId, mutedStatus)
        },
        addConversationToFavourites = addConversationToFavourites,
        moveConversationToFolder = moveConversationToFolder,
        moveConversationToArchive = moveConversationToArchive,
        clearConversationContent = clearConversationContent,
        blockUser = blockUser,
        leaveGroup = { }
    )
}
