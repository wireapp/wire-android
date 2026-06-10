/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
@file:Suppress("MatchingDeclarationName")

package com.wire.android.ui.home.conversations

import androidx.compose.runtime.Composable
import com.wire.android.di.viewModelScopedPreviewOrNull
import com.wire.android.di.metro.sessionKeyedAssistedMetroViewModel
import com.wire.android.di.metro.sessionKeyedAssistedMetroViewModelAs
import com.wire.android.di.metro.sessionKeyedMetroViewModel
import com.wire.android.ui.home.conversations.folder.ConversationFoldersStateArgs
import com.wire.android.ui.home.conversations.folder.ConversationFoldersVM
import com.wire.android.ui.home.conversations.folder.ConversationFoldersVMImpl
import com.wire.android.ui.home.conversations.folder.MoveConversationToFolderArgs
import com.wire.android.ui.home.conversations.folder.MoveConversationToFolderVM
import com.wire.android.ui.home.conversations.folder.MoveConversationToFolderVMImpl
import com.wire.android.ui.home.conversations.folder.NewFolderViewModel
import com.wire.android.ui.home.conversations.promoteadmin.PromoteAdminViewModel
import com.wire.android.ui.home.conversations.search.SearchUserViewModel
import com.wire.android.ui.home.conversations.search.adddembertoconversation.AddMembersToConversationViewModel
import com.wire.android.ui.home.conversations.search.apps.SearchAppsViewModel
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesViewModel
import com.wire.kalium.logic.data.conversation.Conversation
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory

interface ConversationSearchFolderManualViewModelFactory : ManualViewModelAssistedFactory {
    fun conversationFoldersViewModel(args: ConversationFoldersStateArgs): ConversationFoldersVMImpl
    fun moveConversationToFolderViewModel(args: MoveConversationToFolderArgs): MoveConversationToFolderVMImpl
    fun searchAppsViewModel(protocolInfo: Conversation.ProtocolInfo?): SearchAppsViewModel
}

@Composable
fun conversationFoldersViewModel(
    args: ConversationFoldersStateArgs
): ConversationFoldersVM =
    sessionKeyedAssistedMetroViewModelAs<
        ConversationFoldersVMImpl,
        ConversationFoldersVM,
        ConversationSearchFolderManualViewModelFactory,
        >(
        key = "conversation_folders_${args.selectedFolderId}",
        previewProvider = { viewModelScopedPreviewOrNull<ConversationFoldersVM>() },
    ) {
        conversationFoldersViewModel(args)
    }

@Composable
fun moveConversationToFolderViewModel(
    args: MoveConversationToFolderArgs
): MoveConversationToFolderVM =
    sessionKeyedAssistedMetroViewModelAs<
        MoveConversationToFolderVMImpl,
        MoveConversationToFolderVM,
        ConversationSearchFolderManualViewModelFactory,
        >(
        key = "move_conversation_to_folder_${args.conversationId}_${args.currentFolderId}",
        previewProvider = { viewModelScopedPreviewOrNull<MoveConversationToFolderVM>() },
    ) {
        moveConversationToFolderViewModel(args)
    }

@Composable
fun newFolderViewModel(): NewFolderViewModel =
    sessionKeyedMetroViewModel()

@Composable
fun addMembersToConversationViewModel(): AddMembersToConversationViewModel =
    sessionKeyedMetroViewModel()

@Composable
fun searchConversationMessagesViewModel(): SearchConversationMessagesViewModel =
    sessionKeyedMetroViewModel()

@Composable
fun promoteAdminViewModel(): PromoteAdminViewModel =
    sessionKeyedMetroViewModel()
