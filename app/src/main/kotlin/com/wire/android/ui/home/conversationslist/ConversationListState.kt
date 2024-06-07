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

package com.wire.android.ui.home.conversationslist

import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

data class ConversationListState(
    val searchQuery: String = "",
    val foldersWithConversations: ImmutableMap<ConversationFolder, List<ConversationItem>> = persistentMapOf(),
    val hasNoConversations: Boolean = false,
    val conversationSearchResult: ImmutableMap<ConversationFolder, List<ConversationItem>> = persistentMapOf(),
    val missedCalls: ImmutableList<ConversationItem> = persistentListOf(),
    val callHistory: ImmutableList<ConversationItem> = persistentListOf(),
    val unreadMentions: ImmutableList<ConversationItem> = persistentListOf(),
    val allMentions: ImmutableList<ConversationItem> = persistentListOf(),
)

data class ConversationListCallState(
    val hasEstablishedCall: Boolean = false,
    val shouldShowJoinAnywayDialog: Boolean = false,
    val shouldShowCallingPermissionDialog: Boolean = false
)
