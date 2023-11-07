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

package com.wire.android.ui.calling

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.mapper.UICallParticipantMapper
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.media.CallRinger
import com.wire.android.model.ImageAsset
import com.wire.android.ui.navArgs
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.call.ConversationType
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToBackCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToFrontCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsWithSortedParticipantsUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveSpeakerUseCase
import com.wire.kalium.logic.feature.call.usecase.SetVideoPreviewUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOffUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOnUseCase
import com.wire.kalium.logic.feature.call.usecase.UnMuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.UpdateVideoStateUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.util.PlatformView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class SharedCallingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val conversationDetails: ObserveConversationDetailsUseCase,
    private val allCalls: GetAllCallsWithSortedParticipantsUseCase,
    private val endCall: EndCallUseCase,
    private val muteCall: MuteCallUseCase,
    private val unMuteCall: UnMuteCallUseCase,
    private val updateVideoState: UpdateVideoStateUseCase,
    private val setVideoPreview: SetVideoPreviewUseCase,
    private val turnLoudSpeakerOff: TurnLoudSpeakerOffUseCase,
    private val turnLoudSpeakerOn: TurnLoudSpeakerOnUseCase,
    private val flipToFrontCamera: FlipToFrontCameraUseCase,
    private val flipToBackCamera: FlipToBackCameraUseCase,
    private val observeSpeaker: ObserveSpeakerUseCase,
    private val callRinger: CallRinger,
    private val uiCallParticipantMapper: UICallParticipantMapper,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val userTypeMapper: UserTypeMapper,
    private val currentScreenManager: CurrentScreenManager,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val callingNavArgs: CallingNavArgs = savedStateHandle.navArgs()
    val conversationId: QualifiedID = callingNavArgs.conversationId

    var callState by mutableStateOf(CallState(conversationId))

    init {
        viewModelScope.launch {
            val allCallsSharedFlow = allCalls().map {
                it.find { call ->
                    call.conversationId == conversationId &&
                            call.status != CallStatus.CLOSED &&
                            call.status != CallStatus.MISSED
                }
            }.flowOn(dispatchers.default()).shareIn(this, started = SharingStarted.Lazily)

            launch {
                observeConversationDetails(this)
            }
            launch {
                initCallState(allCallsSharedFlow)
            }
            launch {
                observeParticipants(allCallsSharedFlow)
            }
            launch {
                observeOnSpeaker(this)
            }
            launch {
                observeScreenState()
            }
        }
    }

    private suspend fun observeScreenState() {
        currentScreenManager.observeCurrentScreen(viewModelScope).collect {
            if (it == CurrentScreen.InBackground) {
                stopVideo()
            }
        }
    }

    private suspend fun observeConversationDetails(coroutineScope: CoroutineScope) {
        conversationDetails(conversationId = conversationId)
            .filterIsInstance<ObserveConversationDetailsUseCase.Result.Success>() // TODO handle StorageFailure
            .map { it.conversationDetails }
            .flowOn(dispatchers.default())
            .shareIn(coroutineScope, SharingStarted.WhileSubscribed(1))
            .collect { details ->
                callState = when (details) {
                    is ConversationDetails.Group -> {
                        callState.copy(
                            conversationName = getConversationName(details.conversation.name),
                            conversationType = ConversationType.Conference
                        )
                    }

                    is ConversationDetails.OneOne -> {
                        callState.copy(
                            conversationName = getConversationName(details.otherUser.name),
                            avatarAssetId = details.otherUser.completePicture?.let { assetId ->
                                ImageAsset.UserAvatarAsset(wireSessionImageLoader, assetId)
                            },
                            conversationType = ConversationType.OneOnOne,
                            membership = userTypeMapper.toMembership(details.otherUser.userType)
                        )
                    }

                    else -> throw IllegalStateException("Invalid conversation type")
                }
            }
    }

    private suspend fun observeOnSpeaker(coroutineScope: CoroutineScope) {
        observeSpeaker()
            .flowOn(dispatchers.default())
            .shareIn(coroutineScope, SharingStarted.WhileSubscribed(1))
            .collectLatest {
                callState = callState.copy(isSpeakerOn = it)
            }
    }

    private suspend fun initCallState(sharedFlow: SharedFlow<Call?>) {
        sharedFlow.first()?.let { call ->
            callState = callState.copy(
                callStatus = call.status,
                callerName = call.callerName,
                isCameraOn = call.isCameraOn,
                isCbrEnabled = call.isCbrEnabled && call.conversationType == Conversation.Type.ONE_ON_ONE
            )
        }
    }

    private suspend fun observeParticipants(sharedFlow: SharedFlow<Call?>) {
        sharedFlow.collect { call ->
            call?.let {
                callState = callState.copy(
                    isMuted = it.isMuted,
                    callStatus = it.status,
                    isCameraOn = it.isCameraOn,
                    isCbrEnabled = it.isCbrEnabled && call.conversationType == Conversation.Type.ONE_ON_ONE,
                    callerName = it.callerName,
                    participants = it.participants.map { participant ->
                        uiCallParticipantMapper.toUICallParticipant(
                            participant
                        )
                    }
                )
            }
        }
    }

    fun hangUpCall(onCompleted: () -> Unit) {
        viewModelScope.launch {
            onCompleted()
            endCall(conversationId)
            resetCallConfig()
            callRinger.stop()
        }
    }

    private suspend fun resetCallConfig() {
        // we need to update mute state to false, so if the user re-join the call te mic will will be muted
        muteCall(conversationId, false)
        if (callState.isCameraOn) {
            flipToFrontCamera(conversationId)
        }
        if (callState.isCameraOn || callState.isSpeakerOn) {
            turnLoudSpeakerOff()
        }
    }

    fun toggleSpeaker() {
        viewModelScope.launch {
            if (callState.isSpeakerOn) {
                appLogger.i("SharedCallingViewModel: turning off speaker..")
                callState = callState.copy(isSpeakerOn = false)
                turnLoudSpeakerOff()
            } else {
                appLogger.i("SharedCallingViewModel: turning on speaker..")
                callState = callState.copy(isSpeakerOn = true)
                turnLoudSpeakerOn()
            }
        }
    }

    fun flipCamera() {
        viewModelScope.launch {
            if (callState.isOnFrontCamera) {
                appLogger.i("SharedCallingViewModel: flipping to back facing camera..")
                callState = callState.copy(isOnFrontCamera = false)
                flipToBackCamera(conversationId)
            } else {
                appLogger.i("SharedCallingViewModel: flipping to front facing camera..")
                callState = callState.copy(isOnFrontCamera = true)
                flipToFrontCamera(conversationId)
            }
        }
    }

    fun toggleMute(isOnPreviewScreen: Boolean = false) {
        viewModelScope.launch {
            callState.isMuted?.let {
                if (it) {
                    appLogger.i("SharedCallingViewModel: un-muting call..")
                    callState = callState.copy(isMuted = false)
                    unMuteCall(conversationId, !isOnPreviewScreen)
                } else {
                    appLogger.i("SharedCallingViewModel: muting call..")
                    callState = callState.copy(isMuted = true)
                    muteCall(conversationId, !isOnPreviewScreen)
                }
            }
        }
    }

    fun toggleVideo() {
        viewModelScope.launch {
            appLogger.i("SharedCallingViewModel: toggling video to ${!callState.isCameraOn}..")
            callState = callState.copy(
                isCameraOn = !callState.isCameraOn
            )
        }
    }

    fun clearVideoPreview() {
        viewModelScope.launch {
            appLogger.i("SharedCallingViewModel: clearing video preview..")
            setVideoPreview(conversationId, PlatformView(null))
            updateVideoState(conversationId, VideoState.STOPPED)
        }
    }

    fun setVideoPreview(view: View?) {
        viewModelScope.launch(dispatchers.default()) {
            appLogger.i("SharedCallingViewModel: setting video preview..")
            setVideoPreview(conversationId, PlatformView(null))
            setVideoPreview(conversationId, PlatformView(view))
            updateVideoState(conversationId, VideoState.STARTED)
        }
    }

    fun stopVideo() {
        viewModelScope.launch {
            if (callState.isCameraOn) {
                appLogger.i("SharedCallingViewModel: stopping video..")
                callState = callState.copy(isCameraOn = false, isSpeakerOn = false)
                clearVideoPreview()
                turnLoudSpeakerOff()
            }
        }
    }
}
