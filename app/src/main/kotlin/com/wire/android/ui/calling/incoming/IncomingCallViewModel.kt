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
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.calling.CallingNavArgs
import com.wire.android.ui.destinations.OngoingCallScreenDestination
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.GetIncomingCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
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
    private val incomingCalls: GetIncomingCallsUseCase,
    private val rejectCall: RejectCallUseCase,
    private val acceptCall: AnswerCallUseCase,
    private val callRinger: CallRinger,
    private val muteCall: MuteCallUseCase,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val endCall: EndCallUseCase,
) : ViewModel() {

    private val incomingCallNavArgs: CallingNavArgs = savedStateHandle.navArgs()
    private val conversationId: QualifiedID = incomingCallNavArgs.conversationId

    private lateinit var observeIncomingCallJob: Job
    private var establishedCallConversationId: ConversationId? = null

    var incomingCallState by mutableStateOf(IncomingCallState())
        private set

    fun init(onCallClosed: () -> Unit) {
        viewModelScope.launch {
            callRinger.ring(R.raw.ringing_from_them)
            observeIncomingCallJob = launch {
                observeIncomingCall(onCallClosed)
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

    private suspend fun observeIncomingCall(onCallClosed: () -> Unit) {
        incomingCalls().distinctUntilChanged().collect { calls ->
            calls.find { call -> call.conversationId == conversationId }.also {
                if (it == null) {
                    callRinger.stop()
                    onCallClosed()
                }
            }
        }
    }

    fun declineCall(onCallDeclined: () -> Unit) {
        viewModelScope.launch {
            observeIncomingCallJob.cancel()
            launch { rejectCall(conversationId = conversationId) }
            launch {
                callRinger.stop()
                onCallDeclined()
            }
        }
    }

    private fun showJoinCallAnywayDialog() {
        incomingCallState = incomingCallState.copy(shouldShowJoinCallAnywayDialog = true)
    }

    fun dismissJoinCallAnywayDialog() {
        incomingCallState = incomingCallState.copy(shouldShowJoinCallAnywayDialog = false)
    }

    fun acceptCallAnyway(onAccepted: () -> Unit) {
        viewModelScope.launch {
            establishedCallConversationId?.let {
                endCall(it)
                // we need to update mute state to false, so if the user re-join the call te mic will will be muted
                muteCall(it, false)
                delay(DELAY_END_CALL)
            }
            acceptCall(onAccepted)
        }
    }

    fun acceptCall(onAccepted: () -> Unit) {
        viewModelScope.launch {
            if (incomingCallState.hasEstablishedCall) {
                showJoinCallAnywayDialog()
            } else {
                callRinger.stop()

                dismissJoinCallAnywayDialog()
                observeIncomingCallJob.cancel()

                acceptCall(conversationId = conversationId)
                onAccepted()
            }
        }
    }

    companion object {
        const val DELAY_END_CALL = 200L
    }
}
