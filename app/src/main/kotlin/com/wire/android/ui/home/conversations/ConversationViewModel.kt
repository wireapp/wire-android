package com.wire.android.ui.home.conversations

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.android.model.UserStatus
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.model.Message
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.User
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.message.MessageContent
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

    val conversationId: ConversationId? = savedStateHandle
        .getLiveData<String>(NavigationItem.CONVERSATION_ID_ARGUMENT)
        .value
        ?.let {
            val components = it.split("@")
            ConversationId(components.last(), components.first())
        }

    init {
        viewModelScope.launch {
            getMessages(conversationId!!) //TODO what if null???
                .collect { dbMessages ->
                    val messages = dbMessages
                        .toUIMessages()
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

    private fun List<com.wire.kalium.logic.data.message.Message>.toUIMessages(): List<Message> {
        return map { message ->
            Message(
                messageContent = com.wire.android.ui.home.conversations.model.MessageContent.TextMessage(
                    messageBody = com.wire.android.ui.home.conversations.model.MessageBody(
                        (message.content as? MessageContent.Text)?.value ?: "content is not available"
                    )
                ),
                messageHeader = com.wire.android.ui.home.conversations.model.MessageHeader(
                    "some user",
                    Membership.None,
                    true,
                    message.date,
                    MessageStatus.Untouched
                ),
                user = User(availabilityStatus = UserStatus.NONE)

            )
        }
    }
}
