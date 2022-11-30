package com.wire.android.ui.home.conversations.messagedetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.*
import com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReactionsForMessageUseCase
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageDetailsViewModel @Inject constructor(
    qualifiedIdMapper: QualifiedIdMapper,
    override val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val observeReactionsForMessage: ObserveReactionsForMessageUseCase
) : SavedStateViewModel(savedStateHandle) {

    private val conversationId: QualifiedID = qualifiedIdMapper
        .fromStringToQualifiedID(savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!)

    private val messageId: String = savedStateHandle.get<String>(EXTRA_MESSAGE_ID)!!

    private val isSelfMessage: Boolean = savedStateHandle.get<String>(EXTRA_IS_SELF_MESSAGE)!!.toBoolean()

    var messageDetailsState: MessageDetailsState by mutableStateOf(MessageDetailsState())

    init {
        viewModelScope.launch {
            messageDetailsState = messageDetailsState.copy(
                isSelfMessage = isSelfMessage
            )
        }
        viewModelScope.launch {
            observeReactionsForMessage(
                conversationId = conversationId,
                messageId = messageId
            ).collect {
                messageDetailsState = messageDetailsState.copy(
                    reactionsData = it
                )
            }
        }
    }

    fun navigateBack() = viewModelScope.launch {
        navigationManager.navigateBack()
    }
}
