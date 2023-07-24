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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.SavedStateViewModel
import com.wire.kalium.logic.data.id.MessageButtonId
import com.wire.kalium.logic.data.id.MessageId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.message.composite.SendButtonActionMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompositeMessagesViewModel @Inject constructor(
    private val sendButtonActionMessageUseCase: SendButtonActionMessageUseCase,
    qualifiedIdMapper: QualifiedIdMapper,
    savedStateHandle: SavedStateHandle,
) : SavedStateViewModel(savedStateHandle) {

    val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )

    var pendingButtons = mutableStateMapOf<MessageId, MessageButtonId>()
        @VisibleForTesting
        set

    fun onButtonClicked(messageId: String, buttonId: String) {
        if (pendingButtons.containsKey(messageId)) return

        pendingButtons[messageId] = buttonId
        viewModelScope.launch {
            sendButtonActionMessageUseCase(conversationId, messageId, buttonId)
        }.invokeOnCompletion {
            pendingButtons.remove(messageId)
        }
    }
}
