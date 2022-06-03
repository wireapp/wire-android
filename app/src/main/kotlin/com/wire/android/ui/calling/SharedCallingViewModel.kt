package com.wire.android.ui.calling

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.media.CallRinger
import com.wire.android.model.ImageAsset
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.call.ConversationType
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.SetVideoPreviewUseCase
import com.wire.kalium.logic.feature.call.usecase.UnMuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.UpdateVideoStateUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.util.PlatformView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class SharedCallingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val conversationDetails: ObserveConversationDetailsUseCase,
    private val allCalls: GetAllCallsUseCase,
    private val endCall: EndCallUseCase,
    private val muteCall: MuteCallUseCase,
    private val unMuteCall: UnMuteCallUseCase,
    private val updateVideoState: UpdateVideoStateUseCase,
    private val setVideoPreview: SetVideoPreviewUseCase,
    private val callRinger: CallRinger
) : ViewModel() {

    var callState by mutableStateOf(CallState())

    val conversationId: QualifiedID = savedStateHandle
        .get<String>(EXTRA_CONVERSATION_ID)!!
        .parseIntoQualifiedID()

    init {
        viewModelScope.launch {
            launch {
                initializeScreenState()
            }
            launch {
                initializeCallingButtons()
            }
        }
    }

    private suspend fun initializeScreenState() {
        conversationDetails(conversationId = conversationId)
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
                            avatarAssetId = details.otherUser.completePicture?.let { assetId -> ImageAsset.UserAvatarAsset(assetId) },
                            conversationType = ConversationType.OneOnOne
                        )
                    }
                    else -> throw IllegalStateException("Invalid conversation type")
                }
            }
    }

    private suspend fun initializeCallingButtons() {
        allCalls().collect { calls ->
            calls.find { call ->
                call.conversationId == conversationId
            }?.let {
                // TODO update screen state
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
            navigateBack()
            callRinger.stop()
        }
    }

    fun toggleMute() {
        viewModelScope.launch {
            callState = if (callState.isMuted) {
                unMuteCall()
                callState.copy(isMuted = false)
            } else {
                muteCall()
                callState.copy(isMuted = true)
            }
        }
    }

    fun toggleVideo() {
        viewModelScope.launch {
            callState = if (callState.isCameraOn) {
                updateVideoState(conversationId, VideoState.STOPPED)
                callState.copy(isCameraOn = false)
            } else {
                updateVideoState(conversationId, VideoState.STARTED)
                callState.copy(isCameraOn = true)
            }
        }
    }

    fun setVideoPreview(view: View?) {
        viewModelScope.launch {
            setVideoPreview(conversationId, PlatformView(view))
        }
    }

    fun pauseVideo() {
        viewModelScope.launch {
            updateVideoState(conversationId, VideoState.PAUSED)
            setVideoPreview(null)
            callState = callState.copy(isCameraOn = false)
        }
    }

}
