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
package com.wire.android.ui.home.conversations.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    @Suppress("TooGenericExceptionCaught")
    private val addMembersSearchNavArgs: AddMembersSearchNavArgs? = try {
        savedStateHandle.navArgs()
    } catch (e: RuntimeException) {
        null
    }

    var state: SearchState by mutableStateOf(SearchState(
        isServicesAllowed = addMembersSearchNavArgs?.isServicesAllowed ?: false,
        isGroupCreationContext = addMembersSearchNavArgs == null
    ))
        private set

    private val _userSearchSignal: MutableSharedFlow<String> = MutableSharedFlow()
    val userSearchSignal: Flow<String> = _userSearchSignal
        .debounce(DEFAULT_SEARCH_QUERY_DEBOUNCE)

    private val _serviceSearchSignal: MutableSharedFlow<String> = MutableSharedFlow()
    val serviceSearchSignal: Flow<String> = _serviceSearchSignal
        .debounce(DEFAULT_SEARCH_QUERY_DEBOUNCE)

    fun onServiceSearchQueryChanged(query: TextFieldValue) {
        state = state.copy(serviceSearchQuery = query)
        viewModelScope.launch {
            _serviceSearchSignal.emit(query.text)
        }
    }

    fun onUserSearchQueryChanged(query: TextFieldValue) {
        state = state.copy(userSearchQuery = query)
        viewModelScope.launch {
            _userSearchSignal.emit(query.text)
        }
    }
}

data class SearchState(
    val serviceSearchQuery: TextFieldValue = TextFieldValue(),
    val userSearchQuery: TextFieldValue = TextFieldValue(),
    val isServicesAllowed: Boolean = false,
    val isGroupCreationContext: Boolean = false
)
