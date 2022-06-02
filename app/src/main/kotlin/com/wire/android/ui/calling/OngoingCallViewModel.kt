package com.wire.android.ui.calling

import android.util.Log
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.feature.call.CallStatus
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
class OngoingCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val conversationDetails: ObserveConversationDetailsUseCase,
    private val allCalls: GetAllCallsUseCase,
    private val endCall: EndCallUseCase,
    private val muteCall: MuteCallUseCase,
    private val unMuteCall: UnMuteCallUseCase,
    private val setVideoPreview: SetVideoPreviewUseCase,
    private val updateVideoState: UpdateVideoStateUseCase
) : ViewModel() {

    var callEstablishedState by mutableStateOf(OngoingCallState())

    val conversationId: QualifiedID = savedStateHandle
        .get<String>(EXTRA_CONVERSATION_ID)!!
        .parseIntoQualifiedID()

    init {
        viewModelScope.launch {
            launch { initializeScreenState() }
            launch { observeOngoingCall() }
        }
    }

    private suspend fun initializeScreenState() {
        conversationDetails(conversationId = conversationId)
            .collect {
                callEstablishedState = when (it) {
                    is ConversationDetails.Group -> {
                        callEstablishedState.copy(
                            conversationName = it.conversation.name
                        )
                    }
                    is ConversationDetails.OneOne -> {
                        callEstablishedState.copy(
                            conversationName = it.otherUser.name,
                            avatarAssetId = it.otherUser.completePicture?.let { assetId -> UserAvatarAsset(assetId) }
                        )
                    }
                    is ConversationDetails.Self -> throw IllegalStateException("Invalid conversation type")
                    else -> throw IllegalStateException("Invalid conversation type")
                }
            }
    }

    private suspend fun observeOngoingCall() {
        allCalls().collect {
            if (it.first().conversationId == conversationId)
                when (it.first().status) {
                    CallStatus.CLOSED -> navigateBack()
                    else -> {
                        print("DO NOTHING")
                    }
                }
        }
    }

    fun hangUpCall() {
        viewModelScope.launch {
            endCall(conversationId)
            navigateBack()
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
            callEstablishedState = callEstablishedState.copy(isCameraOn = false)
        }
    }

    fun toggleVideo() {
        viewModelScope.launch {
            callEstablishedState = if (callEstablishedState.isCameraOn) {
                updateVideoState(conversationId, VideoState.STOPPED)
                callEstablishedState.copy(isCameraOn = false)
            } else {
                updateVideoState(conversationId, VideoState.STARTED)
                callEstablishedState.copy(isCameraOn = true)
            }
        }
    }

    private suspend fun navigateBack() {
        navigationManager.navigateBack()
    }

    fun muteOrUnMuteCall() {
        viewModelScope.launch {
            callEstablishedState = if (callEstablishedState.isMuted) {
                unMuteCall()
                callEstablishedState.copy(isMuted = false)
            } else {
                muteCall()
                callEstablishedState.copy(isMuted = true)
            }
        }
    }


}
