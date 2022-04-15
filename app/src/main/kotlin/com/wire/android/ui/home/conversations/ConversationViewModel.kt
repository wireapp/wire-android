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
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.parseIntoQualifiedID
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageContent
import com.wire.android.ui.home.conversations.model.MessageContent.ImageMessage
import com.wire.android.ui.home.conversations.model.MessageContent.TextMessage
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.MessageViewWrapper
import com.wire.android.ui.home.conversations.model.User
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.extractImageParams
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.message.AssetContent.AssetMetadata.Image
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent.Asset
import com.wire.kalium.logic.data.message.MessageContent.Text
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.asset.SendImageMessageUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.GetRecentMessagesUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.wire.kalium.logic.data.id.QualifiedID as ConversationId

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class ConversationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val getMessages: GetRecentMessagesUseCase,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeMemberDetails: ObserveConversationMembersUseCase,
    private val sendImageMessage: SendImageMessageUseCase,
    private val sendTextMessage: SendTextMessageUseCase,
    private val getMessageAsset: GetMessageAssetUseCase,
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

    private val conversationId: ConversationId = savedStateHandle
        .get<String>(EXTRA_CONVERSATION_ID)!!
        .parseIntoQualifiedID()

    init {
        viewModelScope.launch {
            getMessages(conversationId).combine(observeMemberDetails(conversationId)) { messages, members ->
                messages.toUIMessages(members)
            }.collect { uiMessages ->
                conversationViewState = conversationViewState.copy(messages = uiMessages)
            }
        }

        viewModelScope.launch {
            observeConversationDetails(conversationId).collect { conversationDetails ->
                val conversationName = when (conversationDetails) {
                    is ConversationDetails.OneOne -> conversationDetails.otherUser.name.orEmpty()
                    else -> conversationDetails.conversation.name.orEmpty()
                }
                conversationViewState = conversationViewState.copy(conversationName = conversationName)
            }
        }
    }

    fun onMessageChanged(message: String) {
        conversationViewState = conversationViewState.copy(messageText = message)
    }

    fun sendMessage() {
        viewModelScope.launch {
            sendTextMessage(conversationId, conversationViewState.messageText)
        }
        conversationViewState = conversationViewState.copy(messageText = "")
    }

    fun sendAttachmentMessage(attachmentBundle: AttachmentBundle?) {
        viewModelScope.launch {
            attachmentBundle?.let {
                appLogger.d("> Attachment for conversationId: $conversationId has size: ${attachmentBundle.rawContent.size}")
                conversationId.run {
                    // TODO: Add an attachment bundle type to differentiate whether to invoke sendImageMessage or sendAssetMessage when the
                    //  rest of the attachment options have been completed
                    val (imgWidth, imgHeight) = extractImageParams(attachmentBundle.rawContent)
                    sendImageMessage(this, attachmentBundle.rawContent, imgWidth, imgHeight)
                }
            }
        }
    }

    fun showDeleteMessageDialog(messageId: String, isMyMessage: Boolean) =
        if (isMyMessage) {
            updateDialogState {
                it.copy(forEveryone = DeleteMessageDialogActiveState.Visible(messageId = messageId, conversationId = conversationId))
            }
        } else {
            updateDialogState {
                it.copy(forYourself = DeleteMessageDialogActiveState.Visible(messageId = messageId, conversationId = conversationId))
            }
        }

    fun showDeleteMessageForYourselfDialog(messageId: String) {
        updateDialogState { it.copy(forEveryone = DeleteMessageDialogActiveState.Hidden) }
        updateDialogState {
            it.copy(
                forYourself = DeleteMessageDialogActiveState.Visible(
                    messageId = messageId,
                    conversationId = conversationId
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
                        conversationId = conversationId,
                        loading = true
                    )
                )
            }
        } else {
            updateDialogState {
                it.copy(
                    forYourself = DeleteMessageDialogActiveState.Visible(
                        messageId = messageId,
                        conversationId = conversationId,
                        loading = true
                    )
                )
            }
        }
        deleteMessage(conversationId = conversationId, messageId = messageId, deleteForEveryone = deleteForEveryone)
        onDialogDismissed()
    }

    fun navigateBack() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

    fun navigateToInitiatingCallScreen() {
        viewModelScope.launch {
            conversationId.let {
                navigationManager.navigate(
                    command = NavigationCommand(
                        destination = NavigationItem.OngoingCall.getRouteWithArgs(listOf(it))
                    )
                )
            }
        }
    }

    private suspend fun List<Message>.toUIMessages(members: List<MemberDetails>): List<MessageViewWrapper> {
        return map { message ->
            val sender = members.findSender(message.senderUserId)
            MessageViewWrapper(
                messageContent = fromMessageModelToMessageContent(message),
                messageSource = MessageSource.CurrentUser,
                messageHeader = MessageHeader(
                    // TODO: Designs for deleted users?
                    username = sender?.name ?: "Deleted User",
                    membership = Membership.None,
                    isLegalHold = false,
                    time = message.date,
                    messageStatus = if (message.status == Message.Status.FAILED) MessageStatus.Failure else MessageStatus.Untouched,
                    messageId = message.id
                ),
                user = User(
                    availabilityStatus = UserStatus.NONE
                )
            )
        }
    }

    private suspend fun fromMessageModelToMessageContent(message: Message): MessageContent =
        when (val content = message.content) {
            is Asset -> {
                val assetId = content.value.remoteData.assetId
                val (imgWidth, imgHeight) = when (val metadata = content.value.metadata) {
                    is Image -> metadata.width to metadata.height
                    else -> 0 to 0
                }
                val decodedImgDataResult = getMessageAsset(
                    conversationId = message.conversationId,
                    messageId = message.id
                ).run {
                    when (this) {
                        is MessageAssetResult.Success -> decodedAsset
                        else -> null
                    }
                }
                when {
                    content.value.mimeType.contains("image") -> ImageMessage(decodedImgDataResult, width = imgWidth, height = imgHeight)
                    // TODO: To be changed once the error behavior has been defined with product
                    assetId.isEmpty() -> TextMessage(messageBody = MessageBody("The asset message could not be downloaded correctly"))
                    // TODO: Add generic asset message UI
                    else -> TextMessage(MessageBody("GENERIC ASSET MESSAGE"))
                }
            }
            is Text -> TextMessage(messageBody = MessageBody(content.value))
            else -> TextMessage(messageBody = MessageBody((content as? Text)?.value ?: "content is not available"))
        }
}
