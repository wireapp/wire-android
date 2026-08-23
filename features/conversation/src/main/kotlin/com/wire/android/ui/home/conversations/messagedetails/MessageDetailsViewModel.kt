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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.metro.WireAssistedViewModelBinding
import com.wire.android.ui.home.conversations.MessageDetailsManualViewModelFactoryGroup
import com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReactionsForMessageUseCase
import com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReceiptsForMessageUseCase
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.receipt.ReceiptType
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch

@WireAssistedViewModelBinding(
    group = MessageDetailsManualViewModelFactoryGroup::class,
    factoryMethod = "messageDetailsViewModel",
)
class MessageDetailsViewModel @AssistedInject constructor(
    @Assisted navigationArgs: MessageDetailsNavArgs,
    private val observeReactionsForMessage: ObserveReactionsForMessageUseCase,
    private val observeReceiptsForMessage: ObserveReceiptsForMessageUseCase
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(navigationArgs: MessageDetailsNavArgs): MessageDetailsViewModel
    }

    private val conversationId: QualifiedID = navigationArgs.conversationId
    private val messageId: String = navigationArgs.messageId
    private val isSelfMessage: Boolean = navigationArgs.isSelfMessage

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
