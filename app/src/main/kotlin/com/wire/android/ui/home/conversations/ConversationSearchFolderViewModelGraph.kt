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
package com.wire.android.ui.home.conversations

import androidx.compose.runtime.Composable
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.di.metro.metroSavedStateViewModel
import com.wire.android.di.metro.metroViewModel
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

interface ConversationSearchFolderViewModelGraph : MetroViewModelGraph {
    val conversationSearchFolderViewModelFactory: ConversationSearchFolderViewModelFactory
}

@Composable
fun conversationFoldersViewModel(args: ConversationFoldersStateArgs): ConversationFoldersVM =
    metroViewModel<ConversationSearchFolderViewModelGraph, ConversationFoldersVMImpl>(
        key = "conversation_folders_${args.selectedFolderId}"
    ) {
        conversationSearchFolderViewModelFactory.conversationFoldersViewModel(args)
    }

@Composable
fun moveConversationToFolderViewModel(args: MoveConversationToFolderArgs): MoveConversationToFolderVM =
    metroViewModel<ConversationSearchFolderViewModelGraph, MoveConversationToFolderVMImpl>(
        key = "move_conversation_to_folder_${args.conversationId}_${args.currentFolderId}"
    ) {
        conversationSearchFolderViewModelFactory.moveConversationToFolderViewModel(args)
    }

@Composable
fun newFolderViewModel(): NewFolderViewModel =
    metroViewModel<ConversationSearchFolderViewModelGraph, NewFolderViewModel> {
        conversationSearchFolderViewModelFactory.newFolderViewModel()
    }

@Composable
fun searchUserViewModel(): SearchUserViewModel =
    metroSavedStateViewModel<ConversationSearchFolderViewModelGraph, SearchUserViewModel> {
        conversationSearchFolderViewModelFactory.searchUserViewModel(it)
    }

@Composable
fun addMembersToConversationViewModel(): AddMembersToConversationViewModel =
    metroSavedStateViewModel<ConversationSearchFolderViewModelGraph, AddMembersToConversationViewModel> {
        conversationSearchFolderViewModelFactory.addMembersToConversationViewModel(it)
    }

@Composable
fun searchConversationMessagesViewModel(): SearchConversationMessagesViewModel =
    metroSavedStateViewModel<ConversationSearchFolderViewModelGraph, SearchConversationMessagesViewModel> {
        conversationSearchFolderViewModelFactory.searchConversationMessagesViewModel(it)
    }

@Composable
fun searchAppsViewModel(protocolInfo: Conversation.ProtocolInfo?): SearchAppsViewModel =
    metroViewModel<ConversationSearchFolderViewModelGraph, SearchAppsViewModel>(
        key = "search_apps_protocol_info_${protocolInfo?.name()}"
    ) {
        conversationSearchFolderViewModelFactory.searchAppsViewModel(protocolInfo)
    }

@Composable
fun promoteAdminViewModel(): PromoteAdminViewModel =
    metroSavedStateViewModel<ConversationSearchFolderViewModelGraph, PromoteAdminViewModel> {
        conversationSearchFolderViewModelFactory.promoteAdminViewModel(it)
    }
