/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.ui.common.bottomsheet.conversation

import MutingOptionsSheetContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.wire.android.R
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

@Composable
fun ConversationSheetContent(
    conversationSheetState: ConversationSheetState,
    onMutingConversationStatusChange: () -> Unit,
    addConversationToFavourites: () -> Unit,
    moveConversationToFolder: () -> Unit,
    moveConversationToArchive: (DialogState) -> Unit,
    clearConversationContent: (DialogState) -> Unit,
    blockUser: (BlockUserDialogState) -> Unit,
    unblockUser: (UnblockUserDialogState) -> Unit,
    leaveGroup: (GroupDialogState) -> Unit,
    deleteGroup: (GroupDialogState) -> Unit,
    closeBottomSheet: () -> Unit = {},
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
                moveConversationToArchive = moveConversationToArchive,
                clearConversationContent = clearConversationContent,
                blockUserClick = blockUser,
                unblockUserClick = unblockUser,
                leaveGroup = leaveGroup,
                deleteGroup = deleteGroup,
                navigateToNotification = conversationSheetState::toMutingNotificationOption
            )
        }

        ConversationOptionNavigation.MutingNotificationOption -> {
            val goBack: () -> Unit = {
                if (conversationSheetState.startOptionNavigation == ConversationOptionNavigation.Home)
                    conversationSheetState.toHome()
                else
                    closeBottomSheet()
            }
            MutingOptionsSheetContent(
                mutingConversationState = conversationSheetState.conversationSheetContent!!.mutingConversationState,
                onMuteConversation = { mutedStatus ->
                    conversationSheetState.muteConversation(mutedStatus)
                    onMutingConversationStatusChange()
                    goBack()
                },
                onBackClick = goBack
            )
        }
    }

    BackHandler(
        conversationSheetState.currentOptionNavigation == ConversationOptionNavigation.MutingNotificationOption
                && conversationSheetState.startOptionNavigation != ConversationOptionNavigation.MutingNotificationOption
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
    val selfRole: Conversation.Member.Role?,
    val isTeamConversation: Boolean
) {

    private val isSelfUserMember: Boolean get() = selfRole != null

    fun canEditNotifications(): Boolean = isSelfUserMember
            && ((conversationTypeDetail is ConversationTypeDetail.Private
            && (conversationTypeDetail.blockingState != BlockingState.BLOCKED))
            || conversationTypeDetail is ConversationTypeDetail.Group)

    fun canDeleteGroup(): Boolean =
        conversationTypeDetail is ConversationTypeDetail.Group &&
                selfRole == Conversation.Member.Role.Admin &&
                conversationTypeDetail.isCreator && isTeamConversation

    fun canLeaveTheGroup(): Boolean = conversationTypeDetail is ConversationTypeDetail.Group && isSelfUserMember

    fun canBlockUser(): Boolean =
        conversationTypeDetail is ConversationTypeDetail.Private && conversationTypeDetail.blockingState == BlockingState.NOT_BLOCKED

    fun canUnblockUser(): Boolean =
        conversationTypeDetail is ConversationTypeDetail.Private && conversationTypeDetail.blockingState == BlockingState.BLOCKED

    fun canAddToFavourite(): Boolean =
        (conversationTypeDetail is ConversationTypeDetail.Private && conversationTypeDetail.blockingState != BlockingState.BLOCKED)
                || conversationTypeDetail is ConversationTypeDetail.Group
}
