package com.wire.android.ui.calling

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.model.UserAvatarAsset
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.parseIntoQualifiedID
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.call.CallStatus
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.GetAllCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.UnMuteCallUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
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
    private val unMuteCall: UnMuteCallUseCase
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
                    else -> throw IllegalStateException("Invalid conversation type")
                }
            }
    }


    private suspend fun observeOngoingCall() {
        allCalls().collect {
            if (it.first().conversationId == conversationId)
                when (it.first().status) {
                    CallStatus.CLOSED -> navigateBack()
                    else -> { print("DO NOTHING") }
                }
        }
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
