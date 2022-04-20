package com.wire.android.ui.calling

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.parseIntoQualifiedID
import com.wire.kalium.logic.feature.call.usecase.StartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.UnMuteCallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import kotlinx.coroutines.launch

@HiltViewModel
class OngoingCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val startCall: StartCallUseCase,
    private val endCall: EndCallUseCase,
    private val muteCall: MuteCallUseCase,
    private val unMuteCall: UnMuteCallUseCase
) : ViewModel() {

    var callEstablishedState by mutableStateOf(OngoingCallState())

    val conversationId: QualifiedID = savedStateHandle
        .get<String>(EXTRA_CONVERSATION_ID)!!
        .parseIntoQualifiedID()

    init {
        //init with fake values
        callEstablishedState = OngoingCallState(
            conversationName = "The Backlog Boys",
            avatarAssetByteArray = null,
            isMuted = false,
            isCameraOn = false,
            isSpeakerOn = false
        )
        viewModelScope.launch {
            initiateCall()
        }
    }

    private suspend fun initiateCall() {
        //TODO pass conversation type
        startCall(conversationId)
    }

    fun hangUpCall() {
        viewModelScope.launch {
            endCall(conversationId)
            navigateBack()
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
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
