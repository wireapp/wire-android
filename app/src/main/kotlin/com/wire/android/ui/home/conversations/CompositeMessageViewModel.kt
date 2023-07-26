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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_MESSAGE_ID
import com.wire.kalium.logic.data.id.MessageButtonId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.message.composite.SendButtonActionMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompositeMessageViewModel @Inject constructor(
    private val sendButtonActionMessageUseCase: SendButtonActionMessageUseCase,
    qualifiedIdMapper: QualifiedIdMapper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )

    private val messageId: String = savedStateHandle.get<String>(EXTRA_MESSAGE_ID)!!

    var pendingButtonId: MessageButtonId? by mutableStateOf(null)
        @VisibleForTesting
        set

    fun sendButtonActionMessage(buttonId: String) {
        if (pendingButtonId != null) return

        pendingButtonId = buttonId
        viewModelScope.launch {
            sendButtonActionMessageUseCase(conversationId, messageId, buttonId)
        }.invokeOnCompletion {
            pendingButtonId = null
        }
    }

    companion object {
        const val ARGS_KEY = "CompositeMessageViewModelKey"
    }
}
