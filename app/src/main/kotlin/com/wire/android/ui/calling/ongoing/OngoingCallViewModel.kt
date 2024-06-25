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

package com.wire.android.ui.calling.ongoing

import android.os.CountDownTimer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.CurrentAccount
import com.wire.android.ui.calling.CallingNavArgs
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.navArgs
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.data.call.CallClient
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.Call
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.RequestVideoStreamsUseCase
import com.wire.kalium.logic.feature.call.usecase.video.SetVideoSendStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class OngoingCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @CurrentAccount
    private val currentUserId: UserId,
    private val globalDataStore: GlobalDataStore,
    private val establishedCalls: ObserveEstablishedCallsUseCase,
    private val requestVideoStreams: RequestVideoStreamsUseCase,
    private val setVideoSendState: SetVideoSendStateUseCase,
    private val currentScreenManager: CurrentScreenManager
) : ViewModel() {

    private val ongoingCallNavArgs: CallingNavArgs = savedStateHandle.navArgs()
    private val conversationId: QualifiedID = ongoingCallNavArgs.conversationId

    var shouldShowDoubleTapToast: Boolean by mutableStateOf(false)
        private set
    private var doubleTapIndicatorCountDownTimer: CountDownTimer? = null

    var state by mutableStateOf(OngoingCallState())
        private set

    init {
        viewModelScope.launch {
            establishedCalls().first { it.isNotEmpty() }.run {
                initCameraState(this)
                // We start observing once we have an ongoing call
                observeCurrentCall()
            }
        }
        showDoubleTapToast()
    }

    private fun initCameraState(calls: List<Call>) {
        val currentCall = calls.find { call -> call.conversationId == conversationId }
        currentCall?.let {
            if (it.isCameraOn) {
                startSendingVideoFeed()
            } else {
                stopSendingVideoFeed()
            }
        }
    }

    fun startSendingVideoFeed() {
        viewModelScope.launch {
            setVideoSendState(conversationId, VideoState.STARTED)
        }
    }
    fun stopSendingVideoFeed() {
        viewModelScope.launch {
            setVideoSendState(conversationId, VideoState.STOPPED)
        }
    }

    private suspend fun observeCurrentCall() {
        establishedCalls()
            .distinctUntilChanged()
            .collect { calls ->
                val currentCall = calls.find { call -> call.conversationId == conversationId }
                val currentScreen = currentScreenManager.observeCurrentScreen(viewModelScope).first()
                val isCurrentlyOnOngoingScreen = currentScreen is CurrentScreen.OngoingCallScreen
                val isOnBackground = currentScreen is CurrentScreen.InBackground
                if (currentCall == null && (isCurrentlyOnOngoingScreen || isOnBackground)) {
                    state = state.copy(flowState = OngoingCallState.FlowState.CallClosed)
                }
            }
    }

    fun requestVideoStreams(participants: List<UICallParticipant>) {
        viewModelScope.launch {
            participants
                .filter {
                    it.isCameraOn || it.isSharingScreen
                }
                .also {
                    if (it.isNotEmpty()) {
                        val clients: List<CallClient> = it.map { uiParticipant ->
                            CallClient(uiParticipant.id.toString(), uiParticipant.clientId)
                        }
                        requestVideoStreams(conversationId, clients)
                    }
                }
        }
    }

    private fun startDoubleTapToastDisplayCountDown() {
        doubleTapIndicatorCountDownTimer?.cancel()
        doubleTapIndicatorCountDownTimer = object : CountDownTimer(DOUBLE_TAP_TOAST_DISPLAY_TIME, COUNT_DOWN_INTERVAL) {
            override fun onTick(p0: Long) {
                appLogger.i("startDoubleTapToastDisplayCountDown: $p0")
            }

            override fun onFinish() {
                shouldShowDoubleTapToast = false
                viewModelScope.launch {
                    globalDataStore.setShouldShowDoubleTapToastStatus(currentUserId.toString(), false)
                }
            }
        }
        doubleTapIndicatorCountDownTimer?.start()
    }

    private fun showDoubleTapToast() {
        viewModelScope.launch {
            delay(DELAY_TO_SHOW_DOUBLE_TAP_TOAST)
            shouldShowDoubleTapToast = globalDataStore.getShouldShowDoubleTapToast(currentUserId.toString())
            if (shouldShowDoubleTapToast) {
                startDoubleTapToastDisplayCountDown()
            }
        }
    }

    fun hideDoubleTapToast() {
        shouldShowDoubleTapToast = false
        viewModelScope.launch {
            globalDataStore.setShouldShowDoubleTapToastStatus(currentUserId.toString(), false)
        }
    }

    companion object {
        const val DOUBLE_TAP_TOAST_DISPLAY_TIME = 7000L
        const val COUNT_DOWN_INTERVAL = 1000L
        const val DELAY_TO_SHOW_DOUBLE_TAP_TOAST = 500L
    }
}
