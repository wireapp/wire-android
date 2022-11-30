package com.wire.android.ui.home.conversations.messagedetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_IS_SELF_MESSAGE
import com.wire.android.navigation.EXTRA_MESSAGE_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReactionsForMessageUseCase
import com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReceiptsForMessageUseCase
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.message.receipt.ReceiptType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageDetailsViewModel @Inject constructor(
    qualifiedIdMapper: QualifiedIdMapper,
    override val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val observeReactionsForMessage: ObserveReactionsForMessageUseCase,
    private val observeReceiptsForMessage: ObserveReceiptsForMessageUseCase
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
        viewModelScope.launch {
            observeReceiptsForMessage(
                conversationId = conversationId,
                messageId = messageId,
                type = ReceiptType.READ
            ).collect {
                messageDetailsState = messageDetailsState.copy(
                    readReceiptsData = it
                )
            }
        }
    }

    fun navigateBack() = viewModelScope.launch {
        navigationManager.navigateBack()
    }
}
