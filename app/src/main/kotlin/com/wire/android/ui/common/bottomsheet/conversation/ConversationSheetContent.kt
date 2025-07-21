/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.ui.common.bottomsheet.conversation

import MutingOptionsSheetContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.wire.android.R
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.home.conversations.folder.ConversationFoldersNavArgs
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.ui.home.conversationslist.model.DeleteGroupDialogState
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.LeaveGroupDialogState
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

@Composable
fun ConversationSheetContent(
    conversationSheetState: ConversationSheetState,
    conversationOptionsData: ConversationOptionsData,
    changeFavoriteState: (ChangeFavoriteStateData) -> Unit,
    moveConversationToFolder: ((ConversationFoldersNavArgs) -> Unit)?,
    removeFromFolder: (RemoveFromFolderData) -> Unit,
    updateConversationArchiveStatus: (DialogState) -> Unit,
    clearConversationContent: (DialogState) -> Unit,
    blockUser: (BlockUserDialogState) -> Unit,
    unblockUser: (UnblockUserDialogState) -> Unit,
    leaveGroup: (LeaveGroupDialogState) -> Unit,
    deleteGroup: (DeleteGroupDialogState) -> Unit,
    deleteGroupLocally: (DeleteGroupDialogState) -> Unit,
    updateMutedConversationStatus: (conversationId: ConversationId, MutedConversationStatus) -> Unit,
) {
    var currentPage by rememberSaveable { mutableStateOf(conversationSheetState.initialPage) }
    when (currentPage) {
        ConversationSheetPage.Main -> {
            ConversationMainSheetContent(
                data = conversationOptionsData,
                changeFavoriteState = changeFavoriteState,
                moveConversationToFolder = moveConversationToFolder,
                removeFromFolder = removeFromFolder,
                updateConversationArchiveStatus = updateConversationArchiveStatus,
                clearConversationContent = clearConversationContent,
                blockUserClick = blockUser,
                unblockUserClick = unblockUser,
                leaveGroup = leaveGroup,
                deleteGroup = deleteGroup,
                deleteGroupLocally = deleteGroupLocally,
                openMutingOptions = { currentPage = ConversationSheetPage.MutingNotification },
            )
        }

        ConversationSheetPage.MutingNotification -> {
            val goBack: () -> Unit = {
                if (conversationSheetState.initialPage == ConversationSheetPage.Main) {
                    currentPage = ConversationSheetPage.Main
                }
            }
            MutingOptionsSheetContent(
                mutingConversationState = conversationOptionsData.mutingConversationState,
                onMuteConversation = { mutedStatus ->
                    updateMutedConversationStatus(conversationOptionsData.conversationId, mutedStatus)
                    goBack()
                },
                onBackClick = goBack
            )
        }
    }

    BackHandler(conversationSheetState.initialPage == ConversationSheetPage.Main && currentPage != ConversationSheetPage.Main) {
        currentPage = ConversationSheetPage.Main
    }
}

sealed interface ConversationTypeDetail {
    sealed interface Group : ConversationTypeDetail {
        val conversationId: ConversationId
        val isFromTheSameTeam: Boolean

        data class Regular(
            override val conversationId: ConversationId,
            override val isFromTheSameTeam: Boolean,
        ) : Group

        data class Channel(
            override val conversationId: ConversationId,
            override val isFromTheSameTeam: Boolean,
            val isPrivate: Boolean,
            val isSelfUserTeamAdmin: Boolean
        ) : Group
    }

    data class Private(
        val avatarAsset: UserAvatarAsset?,
        val userId: UserId,
        val blockingState: BlockingState,
        val isUserDeleted: Boolean
    ) : ConversationTypeDetail

    data class Connection(val avatarAsset: UserAvatarAsset?) : ConversationTypeDetail

    val labelResource: Int
        get() = if (this is Group) R.string.group_label else R.string.conversation_label
}
