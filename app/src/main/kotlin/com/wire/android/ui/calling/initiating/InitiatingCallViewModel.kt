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

package com.wire.android.ui.calling.initiating

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
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.IsLastCallClosedUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.StartCallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class InitiatingCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val startCall: StartCallUseCase,
    private val endCall: EndCallUseCase,
    private val isLastCallClosed: IsLastCallClosedUseCase,
    private val callRinger: CallRinger
) : ViewModel() {

    private val initiatingCallNavArgs: CallingNavArgs = savedStateHandle.navArgs()
    private val conversationId: QualifiedID = initiatingCallNavArgs.conversationId

    private val callStartTime: Long = Calendar.getInstance().timeInMillis
    private var wasCallHangUp: Boolean = false

    fun init(onCallClosed: () -> Unit, onCallEstablished: () -> Unit) {
        viewModelScope.launch {
            launch { initiateCall() }
            launch { observeStartedCall(onCallEstablished) }
            launch { observeClosedCall(onCallClosed) }
        }
    }

    private suspend fun observeStartedCall(onEstablished: () -> Unit) {
        observeEstablishedCalls()
            .map { calls -> calls.map { it.conversationId } }
            .distinctUntilChanged()
            .collect { conversationIds ->
                conversationIds.find { convId ->
                    convId == conversationId
                }?.let {
                    onCallEstablished(onEstablished)
                }
            }
    }

    private suspend fun observeClosedCall(onCallClosed: () -> Unit) {
        isLastCallClosed(
            conversationId = conversationId,
            startedTime = callStartTime
        ).collect { isCurrentCallClosed ->
            if (isCurrentCallClosed && wasCallHangUp.not()) {
                stopRingerAndMarkCallAsHangedUp()
                onCallClosed()
            }
        }
    }

    private fun stopRingerAndMarkCallAsHangedUp() {
        wasCallHangUp = true
        callRinger.stop()
    }

    private fun onCallEstablished(onEstablished: () -> Unit) {
        callRinger.ring(R.raw.ready_to_talk, isLooping = false, isIncomingCall = false)
        onEstablished()
    }

    internal suspend fun initiateCall() {
        val result = startCall(
            conversationId = conversationId
        )
        when (result) {
            StartCallUseCase.Result.Success -> callRinger.ring(resource = R.raw.ringing_from_me, isIncomingCall = false)
            StartCallUseCase.Result.SyncFailure -> {} // TODO: handle case where start call fails
        }
    }

    fun hangUpCall(onCompleted: () -> Unit) = viewModelScope.launch {
        launch { endCall(conversationId) }
        launch {
            stopRingerAndMarkCallAsHangedUp()
            onCompleted()
        }
    }
}
