package com.wire.android.ui.common.bottomsheet.conversation

import MutingOptionsSheetContent
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.wire.android.R
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

@Composable
fun ConversationSheetContent(
    conversationSheetState: ConversationSheetState,
    onMutingConversationStatusChange: () -> Unit,
    addConversationToFavourites: () -> Unit,
    moveConversationToFolder: () -> Unit,
    moveConversationToArchive: () -> Unit,
    clearConversationContent: (DialogState) -> Unit,
    blockUser: (BlockUserDialogState) -> Unit,
    unblockUser: (UnblockUserDialogState) -> Unit,
    leaveGroup: (GroupDialogState) -> Unit,
    deleteGroup: (GroupDialogState) -> Unit,
    isBottomSheetVisible: () -> Boolean = { true }
) {
    // it may be null as initial state
    if (conversationSheetState.conversationSheetContent == null) return

    when (conversationSheetState.currentOptionNavigation) {
        ConversationOptionNavigation.Home -> {
            ConversationMainSheetContent(
                conversationSheetContent = conversationSheetState.conversationSheetContent!!,
// TODO(profile): enable when implemented
//
//                addConversationToFavourites = addConversationToFavourites,
//                moveConversationToFolder = moveConversationToFolder,
//                moveConversationToArchive = moveConversationToArchive,
                clearConversationContent = clearConversationContent,
                blockUserClick = blockUser,
                unblockUserClick = unblockUser,
                leaveGroup = leaveGroup,
                deleteGroup = deleteGroup,
                navigateToNotification = conversationSheetState::toMutingNotificationOption
            )
        }
        ConversationOptionNavigation.MutingNotificationOption -> {
            MutingOptionsSheetContent(
                mutingConversationState = conversationSheetState.conversationSheetContent!!.mutingConversationState,
                onMuteConversation = { mutedStatus ->
                    conversationSheetState.muteConversation(mutedStatus)
                    onMutingConversationStatusChange()
                    conversationSheetState.toHome()
                },
                onBackClick = conversationSheetState::toHome
            )
        }
    }

    BackHandler(
        conversationSheetState.currentOptionNavigation is ConversationOptionNavigation.MutingNotificationOption
                && isBottomSheetVisible()
    ) {
        conversationSheetState.toHome()
    }
}

sealed class ConversationOptionNavigation {
    object Home : ConversationOptionNavigation()
    object MutingNotificationOption : ConversationOptionNavigation()
}

sealed class ConversationTypeDetail {
    data class Group(val conversationId: ConversationId, val isCreator: Boolean) : ConversationTypeDetail()
    data class Private(
        val avatarAsset: UserAvatarAsset?,
        val userId: UserId,
        val blockingState: BlockingState
    ) : ConversationTypeDetail()

    data class Connection(val avatarAsset: UserAvatarAsset?) : ConversationTypeDetail()

    val labelResource: Int
        get() = if (this is Group) R.string.group_label else R.string.conversation_label
}

data class ConversationSheetContent(
    val title: String,
    val conversationId: ConversationId,
    val mutingConversationState: MutedConversationStatus,
    val conversationTypeDetail: ConversationTypeDetail,
    val isSelfUserMember: Boolean = true
) {
    fun canEditNotifications(): Boolean = isSelfUserMember
            && ((conversationTypeDetail is ConversationTypeDetail.Private
            && (conversationTypeDetail.blockingState != BlockingState.BLOCKED))
            || conversationTypeDetail is ConversationTypeDetail.Group)

    fun canDeleteGroup(): Boolean = conversationTypeDetail is ConversationTypeDetail.Group && conversationTypeDetail.isCreator

    fun canLeaveTheGroup(): Boolean = conversationTypeDetail is ConversationTypeDetail.Group && isSelfUserMember

    fun canBlockUser(): Boolean =
        conversationTypeDetail is ConversationTypeDetail.Private && conversationTypeDetail.blockingState == BlockingState.NOT_BLOCKED

    fun canUnblockUser(): Boolean =
        conversationTypeDetail is ConversationTypeDetail.Private && conversationTypeDetail.blockingState == BlockingState.BLOCKED

    fun canAddToFavourite(): Boolean =
        (conversationTypeDetail is ConversationTypeDetail.Private && conversationTypeDetail.blockingState != BlockingState.BLOCKED)
                || conversationTypeDetail is ConversationTypeDetail.Group
}
