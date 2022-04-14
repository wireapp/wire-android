package com.wire.android.ui.calling.incoming

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class IncomingCallViewModel @Inject constructor() : ViewModel() {
    var callState by mutableStateOf(IncomingCallState())
        private set

    init {
        //init with fake values
        callState = IncomingCallState(
            conversationName = "Fake OneToOne Conv.",
            avatarAssetByteArray = null,
            isMicrophoneOn = false,
            isCameraOn = false,
            isSpeakerOn = false
        )
    }
}
