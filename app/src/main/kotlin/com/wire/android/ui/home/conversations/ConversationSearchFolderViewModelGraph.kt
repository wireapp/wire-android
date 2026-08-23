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
import com.wire.android.di.metro.wireAssistedMetroViewModel
import com.wire.android.di.metro.wireMetroViewModel
import com.wire.android.ui.home.conversations.folder.NewFolderViewModel
import com.wire.android.ui.home.conversations.promoteadmin.PromoteAdminViewModel
import com.wire.android.ui.home.conversations.promoteadmin.PromoteAdminNavArgs
import com.wire.android.ui.home.conversations.search.AddMembersSearchNavArgs
import com.wire.android.ui.home.conversations.search.adddembertoconversation.AddMembersToConversationViewModel
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesNavArgs
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesViewModel
import com.wire.android.di.metro.WireAssistedViewModelFactoryGroup

@WireAssistedViewModelFactoryGroup
object ConversationSearchFolderManualViewModelFactoryGroup

@Composable
fun newFolderViewModel(): NewFolderViewModel =
    wireMetroViewModel()

@Composable
fun addMembersToConversationViewModel(args: AddMembersSearchNavArgs): AddMembersToConversationViewModel =
    conversationSearchFolderAssistedViewModel { addMembersToConversationViewModel(args) }

@Composable
fun searchConversationMessagesViewModel(args: SearchConversationMessagesNavArgs): SearchConversationMessagesViewModel =
    conversationSearchFolderAssistedViewModel { searchConversationMessagesViewModel(args) }

@Composable
fun promoteAdminViewModel(args: PromoteAdminNavArgs): PromoteAdminViewModel =
    conversationSearchFolderAssistedViewModel { promoteAdminViewModel(args) }

@Composable
private inline fun <reified VM> conversationSearchFolderAssistedViewModel(
    crossinline create: ConversationSearchFolderManualViewModelFactory.() -> VM,
): VM where VM : androidx.lifecycle.ViewModel =
    wireAssistedMetroViewModel<
        VM,
        ConversationSearchFolderManualViewModelFactory,
        >(create = { _ -> create() })
