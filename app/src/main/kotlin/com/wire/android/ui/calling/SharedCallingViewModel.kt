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
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.call.ConversationType
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class SharedCallingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
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
) : ViewModel() {

    var callState by mutableStateOf(CallState())

    val conversationId: QualifiedID = savedStateHandle
        .get<String>(EXTRA_CONVERSATION_ID)!!
        .parseIntoQualifiedID()

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
            if (it) {
                muteCall(conversationId)
            } else {
                unMuteCall(conversationId)
            }
        }
    }

    private suspend fun observeCallState() {
        allCalls().collect { calls ->
            calls.find { call ->
                call.conversationId == conversationId
            }?.let {
                callState = callState.copy(
                    isMuted = it.isMuted,
                    isCameraOn = it.isCameraOn,
                    participants = it.participants.map { uiCallParticipantMapper.toUICallParticipant(it) }
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
            callState = if (callState.isMuted) {
                callState.copy(isMuted = false)
            } else {
                callState.copy(isMuted = true)
            }
        }
    }

    fun toggleVideo() {
        viewModelScope.launch {
            callState = if (callState.isCameraOn) {
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
            if (callState.isCameraOn) {
                updateVideoState(conversationId, VideoState.PAUSED)
                setVideoPreview(conversationId, PlatformView(null))
                callState = callState.copy(isCameraOn = false)
            }
        }
    }
}
