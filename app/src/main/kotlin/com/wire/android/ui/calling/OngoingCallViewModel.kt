package com.wire.android.ui.calling

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.parseIntoQualifiedID
import com.wire.kalium.logic.feature.call.usecase.StartCallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.wire.kalium.logic.data.id.QualifiedID
import kotlinx.coroutines.launch

@HiltViewModel
class OngoingCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val startCallUseCase: StartCallUseCase
) : ViewModel() {

    var callEstablishedState by mutableStateOf(OngoingCallState())
        private set

    val conversationId: QualifiedID? = savedStateHandle
        .getLiveData<String>(EXTRA_CONVERSATION_ID)
        .value
        ?.parseIntoQualifiedID()

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
        conversationId?.let {
            //TODO pass conversation type
            startCallUseCase.invoke(it)
        }
    }
}
