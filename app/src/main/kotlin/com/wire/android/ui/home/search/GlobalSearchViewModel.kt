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
package com.wire.android.ui.home.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.ui.common.DEFAULT_SEARCH_QUERY_DEBOUNCE
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.UiTextResolver
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.SearchMessagesGloballyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GlobalSearchViewModel(
    private val searchMessagesGlobally: SearchMessagesGloballyUseCase,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val dispatchers: DispatcherProvider,
    private val uiTextResolver: UiTextResolver,
) : ViewModel() {

    val searchQueryTextState: TextFieldState = TextFieldState()

    private val conversationNameCache = mutableMapOf<ConversationId, String>()
    private val mutableState = MutableStateFlow(GlobalSearchState())
    val state: StateFlow<GlobalSearchState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            searchQueryTextState.textAsFlow()
                .map { it.toString() }
                .distinctUntilChanged()
                .debounce(DEFAULT_SEARCH_QUERY_DEBOUNCE)
                .collect { searchQuery ->
                    search(searchQuery)
                }
        }
    }

    fun onFilterSelected(filter: GlobalSearchFilter) {
        if (filter.isEnabled) {
            mutableState.value = mutableState.value.copy(selectedFilter = filter)
        }
    }

    private suspend fun search(searchQuery: String) {
        val normalizedQuery = searchQuery.trim()
        if (normalizedQuery.isBlank()) {
            mutableState.value = GlobalSearchState(searchQuery = searchQuery)
            return
        }

        mutableState.value = mutableState.value.copy(
            searchQuery = normalizedQuery,
            isLoading = true,
            hasError = false,
        )

        when (val result = searchMessagesGlobally(normalizedQuery, GLOBAL_SEARCH_LIMIT)) {
            is SearchMessagesGloballyUseCase.Result.Success -> {
                val items = withContext(dispatchers.io()) {
                    result.messages.mapNotNull { message ->
                        message.toGlobalSearchResultItem()
                    }
                }
                mutableState.value = mutableState.value.copy(
                    results = items,
                    isLoading = false,
                    hasError = false,
                )
            }

            is SearchMessagesGloballyUseCase.Result.Failure -> {
                mutableState.value = mutableState.value.copy(
                    results = emptyList(),
                    isLoading = false,
                    hasError = true,
                )
            }
        }
    }

    private suspend fun Message.Standalone.toGlobalSearchResultItem(): GlobalSearchResultItem? {
        val preview = content.previewText() ?: return null
        return GlobalSearchResultItem(
            messageId = id,
            conversationId = conversationId,
            senderName = senderName(),
            conversationName = conversationName(),
            preview = preview,
        )
    }

    private fun MessageContent.previewText(): String? = when (this) {
        is MessageContent.Text -> value
        is MessageContent.Multipart -> value
        is MessageContent.Composite -> textContent?.value
        else -> null
    }?.takeIf { it.isNotBlank() }

    private fun Message.Standalone.senderName(): String =
        (this as? Message.Sendable)?.senderUserName
            ?: sender?.name
            ?: deletedAccountLabel()

    private suspend fun Message.Standalone.conversationName(): String =
        conversationNameCache[conversationId] ?: runCatching {
            observeConversationDetails(conversationId).firstOrNull()
        }
            .getOrNull()
            ?.let { result ->
                when (result) {
                    is ObserveConversationDetailsUseCase.Result.Success ->
                        result.conversationDetails.displayName()

                    is ObserveConversationDetailsUseCase.Result.Failure -> null
                }
            }
            .orEmpty()
            .ifBlank { deletedAccountLabel() }
            .also { conversationNameCache[conversationId] = it }

    private fun ConversationDetails.displayName(): String = when (this) {
        is ConversationDetails.OneOne -> otherUser.name.orEmpty()
        else -> conversation.name.orEmpty()
    }

    private fun deletedAccountLabel(): String =
        uiTextResolver.resolve(UIText.StringResource(R.string.member_name_deleted_label))

    private companion object {
        const val GLOBAL_SEARCH_LIMIT = 100
    }
}
