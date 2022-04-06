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
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageContent.TextMessage
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.User
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.extractImageParams
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.message.MessageContent.Text
import com.wire.kalium.logic.feature.asset.SendImageMessageUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.GetRecentMessagesUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.wire.kalium.logic.data.id.QualifiedID as ConversationId

@Suppress("LongParameterList")
@HiltViewModel
class ConversationViewModel @Inject constructor(
    // TODO: here we can extract the ID provided to the screen and fetch the data for the conversation
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val getMessages: GetRecentMessagesUseCase,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val sendImageMessage: SendImageMessageUseCase,
    private val sendTextMessage: SendTextMessageUseCase,
    private val deleteMessage: DeleteMessageUseCase
) : ViewModel() {

    var conversationViewState by mutableStateOf(ConversationViewState())
        private set

    var deleteMessageDialogsState: DeleteMessageDialogsState by mutableStateOf(
        DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Hidden,
            forEveryone = DeleteMessageDialogActiveState.Hidden
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
            observeConversationDetails(conversationId!!).collect { conversationDetails ->
                val conversationName = when (conversationDetails) {
                    is ConversationDetails.OneOne -> conversationDetails.otherUser.name.orEmpty()
                    else -> conversationDetails.conversation.name.orEmpty()
                }
                conversationViewState = conversationViewState.copy(conversationName = conversationName)
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
                    // TODO: Add an attachment bundle type to differentiate whether to invoke sendImageMessage or sendAssetMessage when the
                    //  rest of the attachment options have been completed
                    val (imgWidth, imgHeight) = extractImageParams(attachmentBundle.rawContent)
                    sendImageMessage(this, attachmentBundle.rawContent, imgWidth, imgHeight)
                }
            }
        }
    }

    fun showDeleteMessageDialog(messageId: String) =
        updateDialogState {
            it.copy(forEveryone = DeleteMessageDialogActiveState.Visible(messageId = messageId, conversationId = conversationId!!))
        }

    fun showDeleteMessageForYourselfDialog(messageId: String) {
        updateDialogState { it.copy(forEveryone = DeleteMessageDialogActiveState.Hidden) }
        updateDialogState {
            it.copy(
                forYourself = DeleteMessageDialogActiveState.Visible(
                    messageId = messageId,
                    conversationId = conversationId!!
                )
            )
        }
    }

    fun onDialogDismissed() {
        updateDialogState {
            it.copy(
                forEveryone = DeleteMessageDialogActiveState.Hidden,
                forYourself = DeleteMessageDialogActiveState.Hidden
            )
        }
    }

    fun clearDeleteMessageError() {
        updateStateIfDialogVisible { it.copy(error = DeleteMessageError.None) }
    }

    private fun updateDialogState(newValue: (DeleteMessageDialogsState.States) -> DeleteMessageDialogsState) =
        (deleteMessageDialogsState as? DeleteMessageDialogsState.States)?.let { deleteMessageDialogsState = newValue(it) }

    private fun updateStateIfDialogVisible(newValue: (DeleteMessageDialogActiveState.Visible) -> DeleteMessageDialogActiveState) =
        updateDialogState {
            when {
                it.forEveryone is DeleteMessageDialogActiveState.Visible -> it.copy(forEveryone = newValue(it.forEveryone))
                it.forYourself is DeleteMessageDialogActiveState.Visible -> it.copy(
                    forYourself = newValue(
                        it.forYourself
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
                    forEveryone = DeleteMessageDialogActiveState.Visible(
                        messageId = messageId,
                        conversationId = conversationId!!,
                        loading = true
                    )
                )
            }
        } else {
            updateDialogState {
                it.copy(
                    forYourself = DeleteMessageDialogActiveState.Visible(
                        messageId = messageId,
                        conversationId = conversationId!!,
                        loading = true
                    )
                )
            }
        }
        deleteMessage(conversationId = conversationId!!, messageId = messageId, deleteForEveryone = deleteForEveryone)
        onDialogDismissed()
    }

    private fun List<com.wire.kalium.logic.data.message.Message>.toUIMessages(): List<Message> {
        return map { message ->
            Message(
                messageContent = TextMessage(
                    messageBody = MessageBody(
                        (message.content as? Text)?.value ?: "content is not available"
                    )
                ),
                messageSource = MessageSource.CurrentUser,
                messageHeader = MessageHeader(
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
