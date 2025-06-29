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

package com.wire.android.ui.home.drawer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.GlobalDataStore
import com.wire.kalium.logic.feature.conversation.ObserveArchivedUnreadConversationsCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class HomeDrawerViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val observeArchivedUnreadConversationsCountUseCase: ObserveArchivedUnreadConversationsCountUseCase,
    private val globalDataStore: GlobalDataStore,
) : ViewModel() {

    var drawerState by mutableStateOf(
        HomeDrawerState(
            unreadArchiveConversationsCount = 0,
            showFilesOption = false,
        )
    )
        private set

    init {
        observeUnreadArchiveConversationsCount()
        observeWireCellsFeatureState()
    }

    private fun observeWireCellsFeatureState() = viewModelScope.launch {
        globalDataStore.wireCellsEnabled().collect {
            drawerState = drawerState.copy(showFilesOption = it)
        }
    }

    private fun observeUnreadArchiveConversationsCount() {
        viewModelScope.launch {
            observeArchivedUnreadConversationsCountUseCase()
                .collect { drawerState = drawerState.copy(unreadArchiveConversationsCount = it.toInt()) }
        }
    }
}
