package com.wire.android.ui.home.conversations

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.model.ConversationId
import com.wire.android.model.QualifiedIDReference
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.model.Message
import com.wire.kalium.logic.feature.conversation.GetConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.GetRecentMessagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    //TODO: here we can extract the ID provided to the screen and fetch the data for the conversation
    private val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val getMessages: GetRecentMessagesUseCase,
    private val getConversationDetails: GetConversationDetailsUseCase
) : ViewModel() {
//    private val _state = MutableStateFlow(ConversationViewState())

//    val conversationViewState: StateFlow<ConversationViewState>
//        get() = _state

    var conversationViewState by mutableStateOf(ConversationViewState())
        private set

    val conversationId = savedStateHandle
        .getLiveData<ConversationId>(NavigationItem.CONVERSATION_ID_ARGUMENT).value
        ?.toBaseID()

    init {
        viewModelScope.launch {
            getMessages(conversationId!!) //TODO what if null???
                .collect { dbMessages ->
                    val messages = dbMessages.map {
                        Message(
                            messageContent = com.wire.android.ui.home.conversations.model.MessageContent.TextMessage(
                                messageBody = com.wire.android.ui.home.conversations.model.MessageBody(
                                    it.content ?: ""
                                )
                            )
                        )
                    }
                    conversationViewState = conversationViewState.copy(messages = messages)
                }
        }

        viewModelScope.launch {
            getConversationDetails(conversationId!!)
                .collect { conversation ->
                    conversationViewState = conversationViewState.copy(conversationName = conversation.name ?: "")

                }
        }
    }

    fun navigateBack() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

    fun onMessageChanged(message: String) {
        //do something with the message
        Log.d("TEST", "message being typed: $message")
    }

    fun sendMessage() {
        //do something with the message
        Log.d("TEST", "send message button clicked")
    }

    private fun List<com.wire.kalium.persistence.dao.message.Message>.toUIMessages(): List<Message> {
        map { message ->
            Message(
                messageContent = com.wire.android.ui.home.conversations.model.MessageContent.TextMessage(
                    messageBody = com.wire.android.ui.home.conversations.model.MessageBody(
                        message.content ?: ""
                    )
                )
            )
        }
    }
}
