package com.wire.android.ui.userprofile.other

import androidx.compose.runtime.Composable
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationOptionNavigation
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.rememberConversationSheetState
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

@Composable
fun OtherUserProfileBottomSheetContent(
    bottomSheetState: BottomSheetState?,
    onMutingConversationStatusChange: (ConversationId?, MutedConversationStatus) -> Unit,
    addConversationToFavourites: () -> Unit,
    moveConversationToFolder: () -> Unit,
    moveConversationToArchive: () -> Unit,
    clearConversationContent: () -> Unit,
    blockUser: (UserId, String) -> Unit,
    changeMemberRole: (Member.Role) -> Unit,
    closeBottomSheet: () -> Unit
) =
    when (bottomSheetState) {
        is BottomSheetState.Conversation -> ConversationBottomSheet(
            bottomSheetState.conversationData,
            onMutingConversationStatusChange,
            addConversationToFavourites,
            moveConversationToFolder,
            moveConversationToArchive,
            clearConversationContent,
            blockUser
        )
        is BottomSheetState.ChangeRole ->
            EditGroupRoleBottomSheet(
                groupState = bottomSheetState.groupState,
                changeMemberRole = changeMemberRole,
                closeChangeRoleBottomSheet = closeBottomSheet
            )
        null -> {
        }
    }

@Composable
private fun ConversationBottomSheet(
    conversationSheetContent: ConversationSheetContent,
    onMutingConversationStatusChange: (ConversationId?, MutedConversationStatus) -> Unit,
    addConversationToFavourites: () -> Unit,
    moveConversationToFolder: () -> Unit,
    moveConversationToArchive: () -> Unit,
    clearConversationContent: () -> Unit,
    blockUser: (UserId, String) -> Unit
) {
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
