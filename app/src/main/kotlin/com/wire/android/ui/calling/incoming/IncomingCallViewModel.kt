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
 *
 *
 */

package com.wire.android.ui.calling.incoming

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.media.CallRinger
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.GetIncomingCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    qualifiedIdMapper: QualifiedIdMapper,
    private val incomingCalls: GetIncomingCallsUseCase,
    private val rejectCall: RejectCallUseCase,
    private val acceptCall: AnswerCallUseCase,
    private val callRinger: CallRinger,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val endCall: EndCallUseCase,
) : ViewModel() {

    private val incomingCallConversationId: QualifiedID =
        qualifiedIdMapper.fromStringToQualifiedID(checkNotNull(savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)) {
            "No conversationId was provided via savedStateHandle to IncomingCallViewModel"
        })

    lateinit var observeIncomingCallJob: Job
    var establishedCallConversationId: ConversationId? = null

    var incomingCallState by mutableStateOf(IncomingCallState())

    init {
        viewModelScope.launch {
            callRinger.ring(R.raw.ringing_from_them)
            observeIncomingCallJob = launch {
                observeIncomingCall()
            }
            launch {
                observeEstablishedCall()
            }
        }
    }

    private fun observeEstablishedCall() = viewModelScope.launch {
        observeEstablishedCalls()
            .distinctUntilChanged()
            .collect {
                val hasEstablishedCall = it.isNotEmpty()
                establishedCallConversationId = if (it.isNotEmpty()) {
                    it.first().conversationId
                } else null
                incomingCallState = incomingCallState.copy(hasEstablishedCall = hasEstablishedCall)
            }
    }

    private suspend fun observeIncomingCall() {
        incomingCalls().distinctUntilChanged().collect { calls ->
            calls.find { call -> call.conversationId == incomingCallConversationId }.also {
                if (it == null) {
                    onCallClosed()
                }
            }
        }
    }

    private fun onCallClosed() {
        viewModelScope.launch {
            navigationManager.navigateBack()
            callRinger.stop()
        }
    }

    fun declineCall() {
        viewModelScope.launch {
            observeIncomingCallJob.cancel()
            launch { rejectCall(conversationId = incomingCallConversationId) }
            launch {
                navigationManager.navigateBack()
                callRinger.stop()
            }
        }
    }

    fun showJoinCallAnywayDialog() {
        incomingCallState = incomingCallState.copy(shouldShowJoinCallAnywayDialog = true)
    }

    fun dismissJoinCallAnywayDialog() {
        incomingCallState = incomingCallState.copy(shouldShowJoinCallAnywayDialog = false)
    }

    fun acceptCallAnyway() {
        viewModelScope.launch {
            establishedCallConversationId?.let {
                endCall(it)
                delay(200)
            }
            acceptCall()
        }
    }

    fun acceptCall() {
        viewModelScope.launch {
            if (incomingCallState.hasEstablishedCall) {
                showJoinCallAnywayDialog()
            } else {
                callRinger.stop()

                dismissJoinCallAnywayDialog()
                observeIncomingCallJob.cancel()

                acceptCall(conversationId = incomingCallConversationId)

                navigationManager.navigate(
                    command = NavigationCommand(
                        destination = NavigationItem.OngoingCall.getRouteWithArgs(listOf(incomingCallConversationId)),
                        backStackMode = BackStackMode.REMOVE_CURRENT_AND_REPLACE
                    )
                )
            }
        }
    }
}
