package com.wire.android.ui.userprofile.other

import MutingOptionsSheetContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationMainSheetContent
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

@Composable
fun OtherUserProfileBottomSheetContent(
    bottomSheetState: BottomSheetContent?,
    onMutingConversationStatusChange: (ConversationId?, MutedConversationStatus) -> Unit,
    addConversationToFavourites: () -> Unit,
    moveConversationToFolder: () -> Unit,
    moveConversationToArchive: () -> Unit,
    clearConversationContent: () -> Unit,
    blockUser: (UserId, String) -> Unit,
    changeMemberRole: (Member.Role) -> Unit,
    openMuteOptionsSheet: () -> Unit,
    openConversationSheet: () -> Unit,
    closeBottomSheet: () -> Unit
) {
    when (bottomSheetState) {
        is BottomSheetContent.Conversation ->
            ConversationMainSheetContent(
                conversationSheetContent = bottomSheetState.conversationData,
                addConversationToFavourites = addConversationToFavourites,
                moveConversationToFolder = moveConversationToFolder,
                moveConversationToArchive = moveConversationToArchive,
                clearConversationContent = clearConversationContent,
                blockUserClick = blockUser,
                leaveGroup = { },
                navigateToNotification = openMuteOptionsSheet
            )
        is BottomSheetContent.Mute ->
            MutingOptionsSheetContent(
                mutingConversationState = bottomSheetState.conversationData.mutingConversationState,
                onMuteConversation = { onMutingConversationStatusChange(bottomSheetState.conversationData.conversationId, it) },
                onBackClick = openConversationSheet
            )
        is BottomSheetContent.ChangeRole ->
            EditGroupRoleBottomSheet(
                groupState = bottomSheetState.groupState,
                changeMemberRole = changeMemberRole,
                closeChangeRoleBottomSheet = closeBottomSheet
            )
        null -> {
        }
    }

    BackHandler(bottomSheetState != null) {
        if (bottomSheetState is BottomSheetContent.Mute) openConversationSheet()
        else closeBottomSheet()
    }
}
