package com.wire.android.ui.calling

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OngoingCallViewModel @Inject constructor() : ViewModel() {
    var callEstablishedState by mutableStateOf(OngoingCallState())
    private set

    init {
        //init with fake values
        callEstablishedState = OngoingCallState(
            conversationName = "The Backlog Boys",
            avatarAssetByteArray = null,
            isMuted = false,
            isCameraOn = false,
            isSpeakerOn = false
        )
    }
}
