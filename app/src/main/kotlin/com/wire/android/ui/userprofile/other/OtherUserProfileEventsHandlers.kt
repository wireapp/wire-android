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

package com.wire.android.ui.userprofile.other

import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.userprofile.group.RemoveConversationMemberState
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

@Suppress("TooManyFunctions")
interface OtherUserProfileEventsHandler {
    fun onBlockUser(blockUserState: BlockUserDialogState)
    fun onRemoveConversationMember(state: RemoveConversationMemberState)
    fun onUnblockUser(userId: UserId)
    fun observeClientList()

    companion object {
        @Suppress("TooManyFunctions")
        val PREVIEW = object : OtherUserProfileEventsHandler {
            override fun onBlockUser(blockUserState: BlockUserDialogState) {}
            override fun onRemoveConversationMember(state: RemoveConversationMemberState) {}
            override fun onUnblockUser(userId: UserId) {}
            override fun observeClientList() {}
        }
    }
}

interface OtherUserProfileFooterEventsHandler {
    fun onSendConnectionRequest()
    fun onOpenConversation()
    fun onCancelConnectionRequest()
    fun onAcceptConnectionRequest()
    fun onIgnoreConnectionRequest()

    companion object {
        @Suppress("TooManyFunctions")
        val PREVIEW = object : OtherUserProfileFooterEventsHandler {
            override fun onSendConnectionRequest() {}
            override fun onOpenConversation() {}
            override fun onCancelConnectionRequest() {}
            override fun onAcceptConnectionRequest() {}
            override fun onIgnoreConnectionRequest() {}
        }
    }
}

@Suppress("TooManyFunctions")
interface OtherUserProfileBottomSheetEventsHandler {
    fun onChangeMemberRole(role: Conversation.Member.Role)
    fun onMutingConversationStatusChange(conversationId: ConversationId?, status: MutedConversationStatus)
    fun onAddConversationToFavourites(conversationId: ConversationId? = null)
    fun onMoveConversationToFolder(conversationId: ConversationId? = null)
    fun onMoveConversationToArchive(dialogState: DialogState)
    fun onClearConversationContent(dialogState: DialogState)

    companion object {
        @Suppress("TooManyFunctions")
        val PREVIEW = object : OtherUserProfileBottomSheetEventsHandler {
            override fun onChangeMemberRole(role: Conversation.Member.Role) {}
            override fun onMutingConversationStatusChange(conversationId: ConversationId?, status: MutedConversationStatus) {}
            override fun onAddConversationToFavourites(conversationId: ConversationId?) {}
            override fun onMoveConversationToFolder(conversationId: ConversationId?) {}
            override fun onMoveConversationToArchive(dialogState: DialogState) {}
            override fun onClearConversationContent(dialogState: DialogState) {}
        }
    }
}
