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
package com.wire.android.ui.debug.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.navArgs
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.FetchConversationUseCase
import com.wire.kalium.logic.data.conversation.ResetMLSConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugConversationViewModel @Inject constructor(
    private val conversationDetails: ObserveConversationDetailsUseCase,
    private val resetMLSConversation: ResetMLSConversationUseCase,
    private val fetchConversation: FetchConversationUseCase,
    savedStateHandle: SavedStateHandle,
) : ActionsViewModel<DebugConversationScreenAction>() {

    val args: DebugConversationScreenNavArgs = savedStateHandle.navArgs()

    val conversationId = args.conversationId

    private val _state = MutableStateFlow(DebugConversationViewState())
    val state = _state.asStateFlow()

    init {
        loadConversationDetails()
    }

    private fun loadConversationDetails() = viewModelScope.launch {
        conversationDetails(conversationId)
            .collect { result ->
                if (result is ObserveConversationDetailsUseCase.Result.Success) {
                    _state.update { value ->
                        value.copy(
                            conversationId = conversationId.toString(),
                            conversationName = result.conversationDetails.conversation.name,
                            teamId = result.conversationDetails.conversation.teamId?.value,
                            mlsProtocolInfo = result.conversationDetails.conversation.protocol as? Conversation.ProtocolInfo.MLS,
                        )
                    }
                }
            }
    }

    fun resetMLSConversation() = viewModelScope.launch {
        resetMLSConversation(conversationId)
            .onSuccess {
                sendAction(ShowMessage("MLS conversation reset successfully."))
            }
            .onFailure { error ->
                appLogger.e("MLS conversation reset failed: $error")
                sendAction(ShowMessage("MLS conversation reset failed."))
            }
    }

    fun updateConversation() = viewModelScope.launch {
        fetchConversation.fetchWithTransaction(conversationId)
            .onSuccess {
                appLogger.i("MLS conversation fetch success")
            }
            .onFailure { error ->
                appLogger.e("MLS conversation fetch failed: $error")
            }
    }
}

sealed interface DebugConversationScreenAction
data class ShowMessage(val message: String) : DebugConversationScreenAction
data class DebugConversationViewState(
    val conversationId: String? = null,
    val conversationName: String? = null,
    val teamId: String? = null,
    val mlsProtocolInfo: Conversation.ProtocolInfo.MLS? = null,
)
