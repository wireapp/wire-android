package com.wire.android.ui.home.conversationslist.bottomsheet

import MutingOptionsSheetContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

@Composable
fun ConversationSheetContent(
    conversationSheetState: ConversationSheetState,
    onMutingConversationStatusChange: (MutedConversationStatus) -> Unit,
    addConversationToFavourites: () -> Unit,
    moveConversationToFolder: () -> Unit,
    moveConversationToArchive: () -> Unit,
    clearConversationContent: () -> Unit,
    blockUser: (BlockUserDialogState) -> Unit,
    leaveGroup: (GroupDialogState) -> Unit,
    deleteGroup: (GroupDialogState) -> Unit
) {
    when (conversationSheetState.currentOptionNavigation) {
        ConversationNavigationOptions.Home -> {
            ConversationMainSheetContent(
                conversationSheetContent = conversationSheetState.conversationSheetContent!!,
// TODO(profile): enable when implemented
//
//                addConversationToFavourites = addConversationToFavourites,
//                moveConversationToFolder = moveConversationToFolder,
//                moveConversationToArchive = moveConversationToArchive,
//                clearConversationContent = clearConversationContent,
                blockUserClick = blockUser,
                leaveGroup = leaveGroup,
                deleteGroup = deleteGroup,
                navigateToNotification = conversationSheetState::toMutingNotificationOption
            )
        }
        ConversationNavigationOptions.MutingOptionsNotification -> {
            MutingOptionsSheetContent(
                mutingConversationState = conversationSheetState.conversationSheetContent!!.mutingConversationState,
                onMuteConversation = onMutingConversationStatusChange,
                onBackClick = conversationSheetState::toHome
            )
        }
    }

    BackHandler(conversationSheetState.currentOptionNavigation is ConversationNavigationOptions.MutingOptionsNotification) {
        conversationSheetState.toHome()
    }
}


sealed interface ConversationNavigationOptions {
    object Home : ConversationNavigationOptions, OtherUserNavigationOption
    object MutingOptionsNotification : ConversationNavigationOptions, OtherUserNavigationOption
}

sealed interface OtherUserNavigationOption {
    object ChangeRole : OtherUserNavigationOption
}

sealed class ConversationTypeDetail {
    data class Group(val conversationId: ConversationId, val isCreator: Boolean) : ConversationTypeDetail()
    data class Private(
        val avatarAsset: UserAvatarAsset?,
        val userId: UserId,
        val blockingState: BlockingState
    ) : ConversationTypeDetail()

    data class Connection(val avatarAsset: UserAvatarAsset?) : ConversationTypeDetail()
}

data class ConversationSheetContent(
    val title: String,
    val conversationId: ConversationId,
    val mutingConversationState: MutedConversationStatus,
    val conversationTypeDetail: ConversationTypeDetail,
    val isSelfUserMember: Boolean = true
)
