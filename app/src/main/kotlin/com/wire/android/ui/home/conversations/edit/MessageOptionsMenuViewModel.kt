/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.ScopedArgs
import com.wire.android.di.ViewModelScopedPreview
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.usecase.ObserveMessageForConversationUseCase
import com.wire.kalium.logic.data.id.QualifiedID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable

@ViewModelScopedPreview
interface MessageOptionsMenuViewModel {
    fun observeMessageStateFlow(messageId: String): StateFlow<MessageOptionsMenuState> =
        MutableStateFlow(MessageOptionsMenuState.Message(mockMessageWithText))
}

class MessageOptionsMenuViewModelImpl(
    private val observeMessageForConversation: ObserveMessageForConversationUseCase,
    private val args: MessageOptionsMenuArgs,
) : MessageOptionsMenuViewModel, ViewModel() {

    private val currentIdFlow = MutableStateFlow<String?>(null)
    private val stateFlow = currentIdFlow
        .flatMapLatest { messageId ->
            messageId?.let {
                observeMessageForConversation(args.conversationId, messageId)
                    .map {
                        when {
                            it is UIMessage.Regular && !it.isDeleted -> MessageOptionsMenuState.Message(it)
                            else -> MessageOptionsMenuState.NotAvailable
                        }
                    }
                    .onStart { emit(MessageOptionsMenuState.Loading) }
            } ?: flowOf(MessageOptionsMenuState.Loading)
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 500L, replayExpirationMillis = 0L),
            initialValue = MessageOptionsMenuState.Loading,
        )

    override fun observeMessageStateFlow(messageId: String): StateFlow<MessageOptionsMenuState> {
        currentIdFlow.value = messageId
        return stateFlow
    }
}

@Serializable
data class MessageOptionsMenuArgs(val conversationId: QualifiedID) : ScopedArgs {
    override val key = "$ARGS_KEY:$conversationId"

    companion object {
        const val ARGS_KEY = "MessageOptionsMenuArgsKey"
    }
}

sealed interface MessageOptionsMenuState {
    data object Loading : MessageOptionsMenuState
    data object NotAvailable : MessageOptionsMenuState
    data class Message(val message: UIMessage.Regular) : MessageOptionsMenuState
}
