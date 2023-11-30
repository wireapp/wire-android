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
package com.wire.android.ui.home.conversations.search.messages

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.wire.android.ui.home.conversations.search.SearchPeopleViewModel
import com.wire.android.ui.home.conversations.usecase.GetConversationMessagesFromSearchUseCase
import com.wire.android.ui.navArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.QualifiedID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchConversationMessagesViewModel @Inject constructor(
    private val getSearchMessagesForConversation: GetConversationMessagesFromSearchUseCase,
    private val dispatchers: DispatcherProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val searchConversationMessagesNavArgs: SearchConversationMessagesNavArgs = savedStateHandle.navArgs()
    private val conversationId: QualifiedID = searchConversationMessagesNavArgs.conversationId

    @OptIn(SavedStateHandleSaveableApi::class)
    var searchConversationMessagesState by savedStateHandle.saveable(
        stateSaver = Saver<SearchConversationMessagesState, String>(
            save = { it.searchQuery.text },
            restore = {
                SearchConversationMessagesState(
                    conversationId = conversationId,
                    searchQuery = TextFieldValue(it)
                )
            }
        )
    ) { mutableStateOf(SearchConversationMessagesState(conversationId)) }

    private val mutableSearchQueryFlow = MutableStateFlow(searchConversationMessagesState.searchQuery.text)

    init {
        viewModelScope.launch {
            mutableSearchQueryFlow
                .debounce(SearchPeopleViewModel.DEFAULT_SEARCH_QUERY_DEBOUNCE)
                .collectLatest { searchTerm ->
                    getSearchMessagesForConversation(
                        searchTerm = searchTerm,
                        conversationId = conversationId,
                        lastReadIndex = 0
                    ).flowOn(dispatchers.io()).collectLatest {
                        searchConversationMessagesState = searchConversationMessagesState.copy(
                            searchResult = flowOf(it),
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun searchQueryChanged(searchQuery: TextFieldValue) {
        val textQueryChanged = searchConversationMessagesState.searchQuery.text != searchQuery.text
        // we set the state with a searchQuery, immediately to update the UI first
        searchConversationMessagesState = searchConversationMessagesState.copy(
            searchQuery = searchQuery,
            isLoading = true
        )
        if (textQueryChanged && searchQuery.text.isNotBlank()) {
            viewModelScope.launch {
                mutableSearchQueryFlow.emit(searchQuery.text.trim())
            }
        }
    }
}
