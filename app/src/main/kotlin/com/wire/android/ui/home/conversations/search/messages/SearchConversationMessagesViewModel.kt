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
package com.wire.android.ui.home.conversations.search.messages

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.ui.common.DEFAULT_SEARCH_QUERY_DEBOUNCE
import com.wire.android.ui.home.conversations.usecase.GetConversationMessagesFromSearchUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.QualifiedID
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach

@HiltViewModel(assistedFactory = SearchConversationMessagesViewModel.Factory::class)
class SearchConversationMessagesViewModel @AssistedInject constructor(
    @Assisted searchConversationMessagesNavArgs: SearchConversationMessagesNavArgs,
    private val getSearchMessagesForConversation: GetConversationMessagesFromSearchUseCase,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    val conversationId: QualifiedID = searchConversationMessagesNavArgs.conversationId
    val groupName: String = searchConversationMessagesNavArgs.groupName
    val isCellsConversation: Boolean = searchConversationMessagesNavArgs.isCellsConversation

    val searchQueryTextState: TextFieldState = TextFieldState()
    var searchConversationMessagesState by mutableStateOf(SearchConversationMessagesState(conversationId))
        private set

    init {
        val messagesResultFlow = searchQueryTextState.textAsFlow()
            .distinctUntilChanged()
            .debounce(DEFAULT_SEARCH_QUERY_DEBOUNCE)
            .onEach {
                searchConversationMessagesState = searchConversationMessagesState.copy(
                    isLoading = true
                )
            }
            .flatMapLatest { searchTerm ->
                getSearchMessagesForConversation(
                    searchTerm = searchTerm.toString(),
                    conversationId = conversationId,
                    lastReadIndex = 0
                ).onEach {
                    searchConversationMessagesState = searchConversationMessagesState.copy(
                        isLoading = false,
                        searchQuery = searchTerm.toString()
                    )
                }.flowOn(dispatchers.io())
            }
        searchConversationMessagesState = searchConversationMessagesState.copy(
            searchResult = messagesResultFlow,
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(args: SearchConversationMessagesNavArgs): SearchConversationMessagesViewModel
    }
}
