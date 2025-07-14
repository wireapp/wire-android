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

package com.wire.android.ui.home.conversations.messagedetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReactionsForMessageUseCase
import com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReceiptsForMessageUseCase
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.receipt.ReceiptType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageDetailsViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val observeReactionsForMessage: ObserveReactionsForMessageUseCase,
    private val observeReceiptsForMessage: ObserveReceiptsForMessageUseCase
) : ViewModel() {

    private val messageDetailsNavArgs: MessageDetailsNavArgs = savedStateHandle.navArgs()
    private val conversationId: QualifiedID = messageDetailsNavArgs.conversationId
    private val messageId: String = messageDetailsNavArgs.messageId
    private val isSelfMessage: Boolean = messageDetailsNavArgs.isSelfMessage

    var messageDetailsState: MessageDetailsState by mutableStateOf(MessageDetailsState())

    init {
        viewModelScope.launch {
            messageDetailsState = messageDetailsState.copy(
                isSelfMessage = isSelfMessage
            )
        }
        viewModelScope.launch {
            observeReactionsForMessage(
                conversationId = conversationId,
                messageId = messageId
            ).collect {
                messageDetailsState = messageDetailsState.copy(
                    reactionsData = it
                )
            }
        }
        viewModelScope.launch {
            observeReceiptsForMessage(
                conversationId = conversationId,
                messageId = messageId,
                type = ReceiptType.READ
            ).collect {
                messageDetailsState = messageDetailsState.copy(
                    readReceiptsData = it
                )
            }
        }
    }
}
