package com.wire.android.ui.calling

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.mapper.UICallParticipantMapper
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.media.CallRinger
import com.wire.android.model.ImageAsset
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.call.ConversationType
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.call.CallStatus
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList, TooManyFunctions")
@HiltViewModel
class SharedCallingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    qualifiedIdMapper: QualifiedIdMapper,
    private val conversationDetails: ObserveConversationDetailsUseCase,
    private val allCalls: GetAllCallsWithSortedParticipantsUseCase,
    private val endCall: EndCallUseCase,
    private val muteCall: MuteCallUseCase,
    private val unMuteCall: UnMuteCallUseCase,
    private val updateVideoState: UpdateVideoStateUseCase,
    private val setVideoPreview: SetVideoPreviewUseCase,
    private val turnLoudSpeakerOff: TurnLoudSpeakerOffUseCase,
    private val turnLoudSpeakerOn: TurnLoudSpeakerOnUseCase,
    private val observeSpeaker: ObserveSpeakerUseCase,
    private val callRinger: CallRinger,
    private val uiCallParticipantMapper: UICallParticipantMapper,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val userTypeMapper: UserTypeMapper,
    private val currentScreenManager: CurrentScreenManager
) : ViewModel() {

    var callState by mutableStateOf(CallState())

    val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        checkNotNull(savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)) {
            "No conversationId was provided via savedStateHandle to SharedCallingViewModel"
        }
    )

    init {
        viewModelScope.launch {
            launch {
                observeConversationDetails()
            }
            launch {
                observeCallState()
            }
            launch {
                observeOnSpeaker()
            }
            launch {
                observeOnMute()
            }
            launch {
                observeScreenState()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observeScreenState() {
        currentScreenManager.observeCurrentScreen(viewModelScope).collect {
            if (it == CurrentScreen.InBackground) {
                pauseVideo()
            } else if (it == CurrentScreen.OngoingCallScreen(conversationId))
                unPauseVideo()
        }
    }

    private suspend fun observeConversationDetails() {
        conversationDetails(conversationId = conversationId)
            .filterIsInstance<ObserveConversationDetailsUseCase.Result.Success>() // TODO handle StorageFailure
            .map { it.conversationDetails }
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

    private suspend fun observeOnSpeaker() {
        observeSpeaker().collect {
            callState = callState.copy(isSpeakerOn = it)
        }
    }

    private suspend fun observeOnMute() {
        snapshotFlow { callState.isMuted }.collectLatest {
            it?.let {
                if (it) {
                    muteCall(conversationId)
                } else {
                    unMuteCall(conversationId)
                }
            }
        }
    }

    private suspend fun observeCallState() {
        allCalls().collect { calls ->
            calls.find { call ->
                call.conversationId == conversationId &&
                        call.status != CallStatus.CLOSED &&
                        call.status != CallStatus.MISSED
            }?.let { call ->
                callState = callState.copy(
                    callerName = call.callerName,
                    isMuted = call.isMuted,
                    isCameraOn = call.isCameraOn,
                    participants = call.participants.map { uiCallParticipantMapper.toUICallParticipant(it) }
                )
            }
        }
    }

    fun navigateBack() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

    fun hangUpCall() {
        viewModelScope.launch {
            endCall(conversationId)
            callRinger.stop()
        }
    }

    fun toggleSpeaker() {
        viewModelScope.launch {
            callState = if (callState.isSpeakerOn) {
                turnLoudSpeakerOff()
                callState.copy(isSpeakerOn = false)
            } else {
                turnLoudSpeakerOn()
                callState.copy(isSpeakerOn = true)
            }
        }
    }

    fun toggleMute() {
        viewModelScope.launch {
            callState.isMuted?.let {
                callState = if (it) {
                    callState.copy(isMuted = false)
                } else {
                    callState.copy(isMuted = true)
                }
            }
        }
    }

    fun toggleVideo() {
        viewModelScope.launch {
            callState.isCameraOn?.let {
                callState = if (it) {
                    turnLoudSpeakerOff()
                    callState.copy(
                        isCameraOn = false,
                        isSpeakerOn = false
                    )
                } else {
                    turnLoudSpeakerOn()
                    callState.copy(
                        isCameraOn = true,
                        isSpeakerOn = true
                    )
                }
            }
        }
    }

    fun clearVideoPreview() {
        viewModelScope.launch {
            setVideoPreview(conversationId, PlatformView(null))
            updateVideoState(conversationId, VideoState.STOPPED)
        }
    }

    fun setVideoPreview(view: View?) {
        viewModelScope.launch {
            setVideoPreview(conversationId, PlatformView(null))
            setVideoPreview(conversationId, PlatformView(view))
            updateVideoState(conversationId, VideoState.STARTED)
        }
    }

    fun pauseVideo() {
        viewModelScope.launch {
            callState.isCameraOn?.let {
                if (it) {
                    updateVideoState(conversationId, VideoState.PAUSED)
                }
            }
        }
    }

    private fun unPauseVideo() {
        viewModelScope.launch {
            // We should turn on video only for established call
            callState.isCameraOn?.let {
                if (it && callState.participants.isNotEmpty())
                    updateVideoState(conversationId, VideoState.STARTED)
            }
        }
    }
}
