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
 */
package com.wire.android.ui.home.archive

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.mapper.ConversationDetailsMapper
import com.wire.android.ui.home.conversationslist.model.withFolders
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val observeConversationListDetails: ObserveConversationListDetailsUseCase,
    private val conversationDetailsMapper: ConversationDetailsMapper
) : ViewModel() {

    var archivedConversationListState by mutableStateOf(ArchivedConversationListState())

    init {
        viewModelScope.launch {
            observeConversationListDetails(includeArchived = true)
                .map {
                    it.map { conversationDetails ->
                        conversationDetailsMapper.toConversationItem(
                            conversationDetails = conversationDetails
                        )
                    }
                }
                .map { conversationItems ->
                    conversationItems.withFolders().toImmutableMap()
                }
                .collect { conversationsWithFolders ->
                    archivedConversationListState = archivedConversationListState.copy(
                        foldersWithConversations = conversationsWithFolders.toImmutableMap(),
                        hasNoConversations = conversationsWithFolders.isEmpty()
                    )
                }
        }
    }
}