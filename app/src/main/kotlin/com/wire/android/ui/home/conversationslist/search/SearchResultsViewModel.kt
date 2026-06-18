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

package com.wire.android.ui.home.conversationslist.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.mapper.MessageMapper
import com.wire.android.ui.common.DEFAULT_SEARCH_QUERY_DEBOUNCE
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.usecase.GetUsersForMessageUseCase
import com.wire.android.util.EMPTY
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.feature.message.SearchMessagesSemanticallyGloballyUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

class SearchResultsViewModel(
    private val searchMessagesSemanticallyGlobally: SearchMessagesSemanticallyGloballyUseCase,
    private val identifyDiscussionTopicsFromSemanticSearch: IdentifyDiscussionTopicsFromSemanticSearchUseCase,
    private val getUsersForMessage: GetUsersForMessageUseCase,
    private val messageMapper: MessageMapper,
    private val dispatcher: DispatcherProvider,
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow(String.EMPTY)

    private val _messagesSearchState = MutableStateFlow<MessagesSearchState>(MessagesSearchState.EmptyQuery)
    val messagesSearchState = _messagesSearchState.asStateFlow()

    private val _discussionsSearchState = MutableStateFlow<DiscussionsSearchState>(DiscussionsSearchState.EmptyQuery)
    val discussionsSearchState = _discussionsSearchState.asStateFlow()

    private var discussionTopicsJob: Job? = null
    private var searchGeneration = 0

    init {
        observeSearchQuery()
    }

    fun onSearchQueryChanged(query: String) {
        searchQueryFlow.value = query
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce { if (it.isBlank()) 0L else DEFAULT_SEARCH_QUERY_DEBOUNCE }
                .distinctUntilChanged()
                .mapLatest { query ->
                    val generation = ++searchGeneration
                    discussionTopicsJob?.cancel()
                    if (query.isBlank()) {
                        _discussionsSearchState.value = DiscussionsSearchState.EmptyQuery
                        MessagesSearchState.EmptyQuery
                    } else {
                        searchMessages(query, generation)
                    }
                }
                .flowOn(dispatcher.io())
                .collect { _messagesSearchState.value = it }
        }
    }

    private suspend fun searchMessages(query: String, generation: Int): MessagesSearchState {
        _messagesSearchState.value = MessagesSearchState.Loading
        _discussionsSearchState.value = DiscussionsSearchState.Loading
        return when (val result = searchMessagesSemanticallyGlobally(query)) {
            is SearchMessagesSemanticallyGloballyUseCase.Result.Success -> {
                if (result.messages.isEmpty()) {
                    _discussionsSearchState.value = DiscussionsSearchState.NoResults
                } else {
                    identifyDiscussionTopics(result.messages, generation)
                }
                val messages = result.messages.mapNotNull { it.toUIMessage() }
                if (messages.isEmpty()) {
                    MessagesSearchState.NoResults
                } else {
                    MessagesSearchState.Success(messages)
                }
            }

            is SearchMessagesSemanticallyGloballyUseCase.Result.Failure -> {
                _discussionsSearchState.value = DiscussionsSearchState.NoResults
                MessagesSearchState.Failure
            }
        }
    }

    private fun identifyDiscussionTopics(messages: List<Message.Standalone>, generation: Int) {
        discussionTopicsJob = viewModelScope.launch(dispatcher.io()) {
            runCatching {
                identifyDiscussionTopicsFromSemanticSearch(messages)
            }.onSuccess { discussions ->
                if (generation == searchGeneration) {
                    _discussionsSearchState.value = if (discussions.isEmpty()) {
                        DiscussionsSearchState.NoResults
                    } else {
                        DiscussionsSearchState.Success(discussions)
                    }
                }
            }.onFailure { throwable ->
                appLogger.w("SemanticSearchDiscussionTopic: failed to identify discussion topics", throwable)
                if (generation == searchGeneration) {
                    _discussionsSearchState.value = DiscussionsSearchState.NoResults
                }
            }
        }
    }

    private suspend fun Message.Standalone.toUIMessage(): UIMessage? {
        val usersForMessage = getUsersForMessage(this)
        return messageMapper.toUIMessage(usersForMessage, this)
    }
}
