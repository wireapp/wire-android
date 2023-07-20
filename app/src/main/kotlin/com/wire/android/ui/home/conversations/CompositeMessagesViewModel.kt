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
package com.wire.android.ui.home.conversations

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompositeMessagesViewModel @Inject constructor() : ViewModel() {

    var pendingButtons = mutableStateMapOf<String, String>()
        @VisibleForTesting
        set

    fun onButtonClicked(messageId: String, buttonId: String) {
        if (pendingButtons.containsKey(messageId)) return

        pendingButtons[messageId] = buttonId
        viewModelScope.launch {
            doStuff()
        }.invokeOnCompletion {
            pendingButtons.remove(messageId)
        }
    }

    @Suppress("MagicNumber")
    suspend fun doStuff() {
        delay(5000)
    }
}
