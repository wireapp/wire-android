package com.wire.android.ui.home.conversations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.model.UserStatus
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.parseIntoQualifiedID
import com.wire.android.ui.home.conversations.model.*
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.feature.asset.SendImageUseCase
import com.wire.kalium.logic.feature.conversation.GetConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.GetRecentMessagesUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.wire.kalium.logic.data.id.QualifiedID as ConversationId

@HiltViewModel
class ConversationViewModel @Inject constructor(
    // TODO: here we can extract the ID provided to the screen and fetch the data for the conversation
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val getMessages: GetRecentMessagesUseCase,
    private val getConversationDetails: GetConversationDetailsUseCase,
    private val sendImageUseCase: SendImageUseCase,
    private val sendTextMessage: SendTextMessageUseCase
) : ViewModel() {

    var conversationViewState by mutableStateOf(ConversationViewState())
        private set

    val conversationId: ConversationId? = savedStateHandle
        .getLiveData<String>(EXTRA_CONVERSATION_ID)
        .value
        ?.parseIntoQualifiedID()

    init {
        viewModelScope.launch {
            getMessages(conversationId!!) // TODO what if null???
                .collect { dbMessages ->
                    conversationViewState = conversationViewState.copy(messages = dbMessages.toUIMessages())
                }
        }

        viewModelScope.launch {
            getConversationDetails(conversationId!!).let {
                when (it) {
                    is GetConversationDetailsUseCase.Result.Failure -> {
                        TODO("unhandled error case")
                    }
                    is GetConversationDetailsUseCase.Result.Success -> {
                        it.convFlow.collect { conversation ->
                            conversationViewState = conversationViewState.copy(conversationName = conversation.name ?: "Some Name")
                        }
                    }
                }
            }
        }
    }

    fun navigateBack() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

    fun onMessageChanged(message: String) {
        conversationViewState = conversationViewState.copy(messageText = message)
    }

    fun sendMessage() {
        val messageText = conversationViewState.messageText

        conversationViewState = conversationViewState.copy(messageText = "")
        viewModelScope.launch {
            // TODO what if conversationId is null???
            conversationId?.let { sendTextMessage(it, messageText) }

        }
    }

    fun sendAttachmentMessage(attachmentBundle: AttachmentBundle?) {
        viewModelScope.launch {
            attachmentBundle?.let {
                appLogger.d("> Attachment for conversationId: $conversationId has size: ${attachmentBundle.rawContent.size}")
                conversationId?.run {
                    sendImageUseCase(this, attachmentBundle.rawContent)
                }
            }
        }
    }

    private fun List<com.wire.kalium.logic.data.message.Message>.toUIMessages(): List<Message> {
        return map { message ->
            Message(
                messageContent = com.wire.android.ui.home.conversations.model.MessageContent.TextMessage(
                    messageBody = com.wire.android.ui.home.conversations.model.MessageBody(
                        (message.content as? MessageContent.Text)?.value ?: "content is not available"
                    )
                ),
                messageSource = MessageSource.CurrentUser,
                messageHeader = com.wire.android.ui.home.conversations.model.MessageHeader(
                    "Cool User",
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
