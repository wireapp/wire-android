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

package com.wire.android.ui.home.settings.appearance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.ui.theme.ThemeOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomizationViewModel @Inject constructor(
    private val globalDataStore: GlobalDataStore,
) : ViewModel() {
    var state by mutableStateOf(CustomizationState())
        private set

    init {
        observeThemeState()
        observePressEnterToSendState()
        observeMessageBubble()
    }

    private fun observePressEnterToSendState() {
        viewModelScope.launch {
            globalDataStore.enterToSendFlow().collect { state = state.copy(pressEnterToSentState = it) }
        }
    }

    private fun observeThemeState() {
        viewModelScope.launch {
            globalDataStore.selectedThemeOptionFlow().collect { option -> state = state.copy(selectedThemeOption = option) }
        }
    }

    private fun observeMessageBubble() {
        viewModelScope.launch {
            globalDataStore.messageBubbleEnabled().collect { option -> state = state.copy(messageBubbleEnabled = option) }
        }
    }

    fun selectPressEnterToSendOption(option: Boolean) {
        viewModelScope.launch {
            globalDataStore.setEnterToSend(option)
        }
    }

    fun selectThemeOption(option: ThemeOption) {
        viewModelScope.launch {
            globalDataStore.setThemeOption(option)
        }
    }

    fun selectMessageBubble(option: Boolean) {
        viewModelScope.launch {
            globalDataStore.setMessageBubbleEnabled(option)
        }
    }
}
