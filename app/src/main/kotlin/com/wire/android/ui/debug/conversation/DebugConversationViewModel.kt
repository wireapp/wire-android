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
import com.wire.kalium.logic.feature.debug.DebugFeedConfig
import com.wire.kalium.logic.feature.debug.DebugFeedConversationUseCase
import com.wire.kalium.logic.feature.debug.DebugFeedResult
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
    private val feedConversation: DebugFeedConversationUseCase,
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

    /**
     * Toggle: enable/disable messages feeder for this conversation.
     */
    fun onMessagesFeederToggle(enabled: Boolean) {
        _state.update { current ->
            current.copy(
                feedConfig = current.feedConfig.copy(messagesEnabled = enabled)
            )
        }
    }

    /**
     * Toggle: enable/disable reactions feeder for this conversation.
     */
    fun onReactionsFeederToggle(enabled: Boolean) {
        _state.update { current ->
            current.copy(
                feedConfig = current.feedConfig.copy(reactionsEnabled = enabled)
            )
        }
    }

    /**
     * Toggle: enable/disable unread events feeder for this conversation.
     */
    fun onUnreadEventsFeederToggle(enabled: Boolean) {
        _state.update { current ->
            current.copy(
                feedConfig = current.feedConfig.copy(unreadEventsEnabled = enabled)
            )
        }
    }

    /**
     * Toggle: enable/disable mentions feeder for this conversation.
     */
    fun onMentionsFeederToggle(enabled: Boolean) {
        _state.update { current ->
            current.copy(
                feedConfig = current.feedConfig.copy(mentionsEnabled = enabled)
            )
        }
    }

    fun showFeedersDialog(show: Boolean) {
        _state.update { current ->
            current.copy(
                feedConfig = current.feedConfig.copy(showDialog = show)
            )
        }
    }

    /**
     * Runs all selected feeders for the current conversation.
     *
     * This is debug-only: it can generate a lot of local data and should only be
     * triggered manually from the debug screen.
     */
    fun runFeedersForConversation() = viewModelScope.launch {
        val uiConfig = state.value.feedConfig

        // Map UI config -> domain config for the use case.
        val config = DebugFeedConfig(
            messages = uiConfig.messagesEnabled,
            reactions = uiConfig.reactionsEnabled,
            unreadEvents = uiConfig.unreadEventsEnabled,
            mentions = uiConfig.mentionsEnabled,
        )

        _state.update {
            it.copy(feedConfig = it.feedConfig.copy(isProcessing = true))
        }
        when (val response = feedConversation(conversationId, config)) {
            is DebugFeedResult.Failure -> {
                _state.update {
                    it.copy(feedConfig = DebugFeedConfigUiState())
                }
                sendAction(ShowMessage("Feeders failed: ${response.coreFailure}}"))
            }

            DebugFeedResult.Success -> {
                _state.update {
                    it.copy(feedConfig = DebugFeedConfigUiState())
                }
                sendAction(ShowMessage("Feeders executed successfully."))
            }
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
    val feedConfig: DebugFeedConfigUiState = DebugFeedConfigUiState(),
)

data class DebugFeedConfigUiState(
    val messagesEnabled: Boolean = false,
    val reactionsEnabled: Boolean = false,
    val unreadEventsEnabled: Boolean = false,
    val mentionsEnabled: Boolean = false,
    val isProcessing: Boolean = false,
    val showDialog: Boolean = false
)
