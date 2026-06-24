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
import com.wire.android.ui.home.conversations.messages.item.MessageSwipeAction
import com.wire.android.ui.theme.ThemeOption
import kotlinx.coroutines.launch
import dev.zacsweers.metro.Inject
class CustomizationViewModel @Inject constructor(
    private val globalDataStore: GlobalDataStore,
) : ViewModel() {
    var state by mutableStateOf(CustomizationState())
        private set
    init {
        observeThemeState()
        observePressEnterToSendState()
        observeMessageSwipeActionState()
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
    private fun observeMessageSwipeActionState() {
        viewModelScope.launch {
            globalDataStore.messageSwipeRightActionFlow().collect { action -> state = state.copy(messageSwipeRightAction = action) }
        }
        viewModelScope.launch {
            globalDataStore.messageSwipeLeftActionFlow().collect { action -> state = state.copy(messageSwipeLeftAction = action) }
        }
    }
    fun selectPressEnterToSendOption(option: Boolean) {
        viewModelScope.launch {
            globalDataStore.setEnterToSend(option)
        }
    }
    fun selectMessageSwipeRightAction(action: MessageSwipeAction) {
        viewModelScope.launch {
            globalDataStore.setMessageSwipeRightAction(action)
            if (action == state.messageSwipeLeftAction) {
                globalDataStore.setMessageSwipeLeftAction(state.messageSwipeRightAction)
            }
        }
    }
    fun selectMessageSwipeLeftAction(action: MessageSwipeAction) {
        viewModelScope.launch {
            globalDataStore.setMessageSwipeLeftAction(action)
            if (action == state.messageSwipeRightAction) {
                globalDataStore.setMessageSwipeRightAction(state.messageSwipeLeftAction)
            }
        }
    }
    fun selectThemeOption(option: ThemeOption) {
        viewModelScope.launch {
            globalDataStore.setThemeOption(option)
        }
    }
}
