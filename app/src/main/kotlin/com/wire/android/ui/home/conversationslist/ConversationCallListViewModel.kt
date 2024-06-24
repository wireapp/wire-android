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

package com.wire.android.ui.home.conversationslist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.model.SnackBarMessage
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("MagicNumber", "TooManyFunctions", "LongParameterList")
@HiltViewModel
class ConversationCallListViewModel @Inject constructor(
    private val answerCall: AnswerCallUseCase,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val endCall: EndCallUseCase
) : ViewModel() {

    var conversationListCallState by mutableStateOf(ConversationListCallState())

    private val _infoMessage = MutableSharedFlow<SnackBarMessage>()
    val infoMessage = _infoMessage.asSharedFlow()

    var establishedCallConversationId: QualifiedID? = null
    private var conversationId: QualifiedID? = null

    private suspend fun observeEstablishedCall() {
        observeEstablishedCalls()
            .distinctUntilChanged()
            .collectLatest {
                val hasEstablishedCall = it.isNotEmpty()
                conversationListCallState = conversationListCallState.copy(
                    hasEstablishedCall = hasEstablishedCall
                )
                establishedCallConversationId = if (it.isNotEmpty()) {
                    it.first().conversationId
                } else {
                    null
                }
            }
    }

    init {
        viewModelScope.launch {
            observeEstablishedCall()
        }
    }

    fun joinAnyway(conversationId: ConversationId, onJoined: (ConversationId) -> Unit) {
        viewModelScope.launch {
            establishedCallConversationId?.let {
                endCall(it)
                delay(DELAY_END_CALL)
            }
            joinOngoingCall(conversationId, onJoined)
        }
    }

    fun joinOngoingCall(conversationId: ConversationId, onJoined: (ConversationId) -> Unit) {
        this.conversationId = conversationId
        if (conversationListCallState.hasEstablishedCall) {
            showJoinCallAnywayDialog()
        } else {
            dismissJoinCallAnywayDialog()
            viewModelScope.launch {
                answerCall(conversationId = conversationId)
            }
            onJoined(conversationId)
        }
    }

    private fun showJoinCallAnywayDialog() {
        conversationListCallState =
            conversationListCallState.copy(shouldShowJoinAnywayDialog = true)
    }

    fun dismissJoinCallAnywayDialog() {
        conversationListCallState =
            conversationListCallState.copy(shouldShowJoinAnywayDialog = false)
    }

    companion object {
        const val DELAY_END_CALL = 200L
    }
}
