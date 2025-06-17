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
package com.wire.android.ui.home.conversations.migration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationMigrationViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val observeConversationDetails: ObserveConversationDetailsUseCase
) : ViewModel() {

    /**
     * Represents the target conversation, after a conversation migration.
     * The target conversation is the active one-on-one conversation ID if the current conversation
     * is migrated to a different conversation.
     * If this conversation was not migrated to another one, the target conversation is null.
     */
    var migratedConversationId by mutableStateOf<ConversationId?>(null)
        private set

    private val conversationNavArgs = savedStateHandle.navArgs<ConversationNavArgs>()
    private val conversationId: QualifiedID = conversationNavArgs.conversationId

    init {
        viewModelScope.launch {
            observeConversationDetails(conversationId)
                .filterIsInstance<ObserveConversationDetailsUseCase.Result.Success>()
                .map { it.conversationDetails }
                .filterIsInstance<ConversationDetails.OneOne>()
                .collectLatest {
                    val activeOneOnOneConversationId = it.otherUser.activeOneOnOneConversationId
                    val wasThisConversationMigrated = activeOneOnOneConversationId != conversationId
                    if (wasThisConversationMigrated) {
                        migratedConversationId = activeOneOnOneConversationId
                    }
                }
        }
    }
}
