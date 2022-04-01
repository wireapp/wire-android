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
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.Message
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.User
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.feature.conversation.GetConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
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
    private val sendTextMessage: SendTextMessageUseCase,
    private val deleteMessage: DeleteMessageUseCase
) : ViewModel() {

    var conversationViewState by mutableStateOf(ConversationViewState())
        private set

    var deleteMessageDialogsState: DeleteMessageState by mutableStateOf(
        DeleteMessageState.State(
            deleteMessageForYourselfDialogState = DeleteMessageDialogState.Hidden,
            deleteMessageDialogState = DeleteMessageDialogState.Hidden
        )
    )
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
                // TODO send attachment message for conversationId via use case
                appLogger.d("> Attachment for conversationId: $conversationId has size: ${attachmentBundle.rawContent.size}")
            }
        }
    }

    fun showDeleteMessageDialog(messageId: String) =
        updateDialogState {
            it.copy(deleteMessageDialogState = DeleteMessageDialogState.Visible(messageId = messageId, conversationId = conversationId!!))
        }

    fun showDeleteMessageForYourselfDialog(messageId: String) {
        //hide deleteMessageDialog
        updateDialogState { it.copy(deleteMessageDialogState = DeleteMessageDialogState.Hidden) }
        //show deleteMessageForYourselfDialog
        updateDialogState {
            it.copy(
                deleteMessageForYourselfDialogState = DeleteMessageDialogState.Visible(
                    messageId = messageId,
                    conversationId = conversationId!!
                )
            )
        }
    }

    fun onDialogDismissed() {
        updateStateIfDialogVisible { DeleteMessageDialogState.Hidden }
    }

    fun clearDeleteMessageError() {
        updateStateIfDialogVisible { it.copy(error = DeleteMessageError.None) }
    }

    private fun updateDialogState(newValue: (DeleteMessageState.State) -> DeleteMessageState) =
        (deleteMessageDialogsState as? DeleteMessageState.State)?.let { deleteMessageDialogsState = newValue(it) }

    private fun updateStateIfDialogVisible(newValue: (DeleteMessageDialogState.Visible) -> DeleteMessageDialogState) =
        updateDialogState {
            when {
                it.deleteMessageDialogState is DeleteMessageDialogState.Visible -> it.copy(deleteMessageDialogState = newValue(it.deleteMessageDialogState))
                it.deleteMessageForYourselfDialogState is DeleteMessageDialogState.Visible -> it.copy(
                    deleteMessageForYourselfDialogState = newValue(
                        it.deleteMessageForYourselfDialogState
                    )
                )
                else -> it
            }
        }

    fun deleteMessage(messageId: String, deleteForEveryone: Boolean) = viewModelScope.launch {
        //update dialogs state to loading
        if (deleteForEveryone) {
            updateDialogState {
                it.copy(
                    deleteMessageDialogState = DeleteMessageDialogState.Visible(
                        messageId = messageId,
                        conversationId = conversationId!!,
                        loading = true
                    )
                )
            }
        } else {
            updateDialogState {
                it.copy(
                    deleteMessageForYourselfDialogState = DeleteMessageDialogState.Visible(
                        messageId = messageId,
                        conversationId = conversationId!!,
                        loading = true
                    )
                )
            }
        }
        deleteMessage(conversationId = conversationId!!, messageId = messageId, deleteForEveryone = deleteForEveryone)
        //dismiss dialogs
        onDialogDismissed()
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
                    MessageStatus.Untouched,
                    messageId = message.id
                ),
                user = User(availabilityStatus = UserStatus.NONE)

            )
        }
    }
}
