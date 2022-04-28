package com.wire.android.ui.calling.initiating

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
import com.wire.android.ui.calling.getConversationName
import com.wire.kalium.logic.data.call.ConversationType
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.StartCallUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InitiatingCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val startCall: StartCallUseCase,
    private val endCall: EndCallUseCase,
    private val conversationDetails: ObserveConversationDetailsUseCase
) : ViewModel() {

    var callInitiatedState by mutableStateOf(InitiatingCallState())
        private set

    val conversationId: QualifiedID = savedStateHandle
        .get<String>(EXTRA_CONVERSATION_ID)!!
        .parseIntoQualifiedID()

    init {
        viewModelScope.launch {
            initiateCall()
            initializeScreenState()
        }
    }

    private suspend fun initializeScreenState() {
        conversationDetails(conversationId = conversationId)
            .collect {
                callInitiatedState = when (it) {
                    is ConversationDetails.Group -> {
                        callInitiatedState.copy(
                            conversationName = getConversationName(it.conversation.name),
                            conversationType = ConversationType.OneOnOne
                        )
                    }
                    is ConversationDetails.OneOne -> {
                        callInitiatedState.copy(
                            conversationName = getConversationName(it.otherUser.name),
                            avatarAssetId = UserAvatarAsset(it.otherUser.completePicture ?: ""),
                            conversationType = ConversationType.Conference
                        )
                    }
                    else -> throw IllegalStateException("Invalid conversation type")
                }
            }
    }

    private suspend fun initiateCall() {
        startCall(
            conversationId = conversationId,
            conversationType = callInitiatedState.conversationType
        )
    }

    fun hangUpCall() {
        viewModelScope.launch { endCall(conversationId) }
        navigateBack()
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

}
