package com.wire.android.ui.home.conversations

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.home.conversations.mock.mockMessages
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.Message
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.messagecomposer.MessageComposer
import kotlinx.coroutines.launch
import com.wire.android.util.dialogErrorStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationViewModel: ConversationViewModel
) {
    val uiState = conversationViewModel.conversationViewState
    val deleteMessageDialogState = conversationViewModel.deleteMessageDialogsState
    ConversationScreen(
        conversationViewState = uiState,
        onMessageChanged = { message -> conversationViewModel.onMessageChanged(message) },
        onSendButtonClicked = { conversationViewModel.sendMessage() },
        onSendAttachment = { attachmentBundle -> conversationViewModel.sendAttachmentMessage(attachmentBundle) },
        onBackButtonClick = { conversationViewModel.navigateBack() },
        onDeleteMessage = conversationViewModel::showDeleteMessageDialog
    )
    if (deleteMessageDialogState is DeleteMessageState.State) {
        when {
            deleteMessageDialogState.deleteMessageDialogState is DeleteMessageDialogState.Visible -> {
                DeleteMessageDialog(
                    state = deleteMessageDialogState.deleteMessageDialogState,
                    onDialogDismiss = { conversationViewModel.onDialogDismissed() },
                    onDeleteForMe = {
                        conversationViewModel.showDeleteMessageForYourselfDialog(
                            deleteMessageDialogState.deleteMessageDialogState.messageId
                        )
                    },
                    onDeleteForEveryone = {
                        conversationViewModel.deleteMessage(
                            deleteMessageDialogState.deleteMessageDialogState.messageId,
                            true
                        )
                    },
                )
                if (deleteMessageDialogState.deleteMessageDialogState.error is DeleteMessageError.GenericError) {
                    val (title, message) = deleteMessageDialogState.deleteMessageDialogState.error.coreFailure.dialogErrorStrings(
                        LocalContext.current.resources
                    )
                    WireDialog(
                        title = title,
                        text = message,
                        onDismiss = { conversationViewModel.clearDeleteMessageError() },
                        optionButton1Properties = WireDialogButtonProperties(
                            onClick = { conversationViewModel.clearDeleteMessageError() },
                            text = stringResource(id = R.string.label_ok),
                            type = WireDialogButtonType.Primary,
                        )
                    )
                }
            }
            deleteMessageDialogState.deleteMessageForYourselfDialogState is DeleteMessageDialogState.Visible -> {

                if (deleteMessageDialogState.deleteMessageForYourselfDialogState.error is DeleteMessageError.GenericError) {
                    val (title, message) = deleteMessageDialogState.deleteMessageForYourselfDialogState.error.coreFailure.dialogErrorStrings(
                        LocalContext.current.resources
                    )
                    WireDialog(
                        title = title,
                        text = message,
                        onDismiss = { conversationViewModel.clearDeleteMessageError() },
                        optionButton1Properties = WireDialogButtonProperties(
                            onClick = { conversationViewModel.clearDeleteMessageError() },
                            text = stringResource(id = R.string.label_ok),
                            type = WireDialogButtonType.Primary,
                        )
                    )
                } else {
                    DeleteMessageForYourselfDialog(
                        state = deleteMessageDialogState.deleteMessageForYourselfDialogState,
                        onDialogDismiss = { conversationViewModel.onDialogDismissed() },
                        onDeleteForMe = {
                            conversationViewModel.deleteMessage(
                                deleteMessageDialogState.deleteMessageForYourselfDialogState.messageId,
                                false
                            )
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
private fun ConversationScreen(
    conversationViewState: ConversationViewState,
    onMessageChanged: (String) -> Unit,
    onSendButtonClicked: () -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onBackButtonClick: () -> Unit,
    onDeleteMessage: (String) -> Unit,

    ) {
    val conversationScreenState = rememberConversationScreenState()
    val scope = rememberCoroutineScope()

    with(conversationViewState) {
        MenuModalSheetLayout(
            sheetState = conversationScreenState.modalBottomSheetState,
            menuItems = EditMessageMenuItems(
                editMessageSource = conversationScreenState.editMessageSource,
                onCopyMessage = conversationScreenState::copyMessage,
                onDeleteMessage = { onDeleteMessage(conversationScreenState.editMessage?.messageHeader!!.messageId) }
            ),
            content = {
                Scaffold(
                    topBar = {
                        ConversationScreenTopAppBar(
                            title = conversationName,
                            onBackButtonClick = onBackButtonClick,
                            onDropDownClick = {},
                            onSearchButtonClick = {},
                            onVideoButtonClick = {}
                        )
                    },
                    snackbarHost = {
                        SwipeDismissSnackbarHost(
                            hostState = conversationScreenState.snackBarHostState,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    content = {
                        ConversationScreenContent(
                            messages = messages,
                            onMessageChanged = onMessageChanged,
                            messageText = conversationViewState.messageText,
                            onSendButtonClicked = onSendButtonClicked,
                            onShowContextMenu = { message -> conversationScreenState.showEditContextMenu(message) },
                            onSendAttachment = onSendAttachment,
                            onError = { errorMessage ->
                                scope.launch {
                                    conversationScreenState.snackBarHostState.showSnackbar(errorMessage)
                                }
                            }
                        )
                    }
                )
            }
        )
    }
}

@Composable
private fun EditMessageMenuItems(
    editMessageSource: MessageSource?,
    onCopyMessage: () -> Unit,
    onDeleteMessage: () -> Unit
): List<@Composable () -> Unit> {
    return buildList {
        add {
            MenuBottomSheetItem(
                icon = {
                    MenuItemIcon(
                        id = R.drawable.ic_copy,
                        contentDescription = stringResource(R.string.content_description_block_the_user),
                    )
                },
                title = stringResource(R.string.label_copy),
                onItemClick = onCopyMessage
            )
        }
        if (editMessageSource == MessageSource.CurrentUser)
            add {
                MenuBottomSheetItem(
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_edit,
                            contentDescription = stringResource(R.string.content_description_edit_the_message)
                        )
                    },
                    title = stringResource(R.string.label_edit),
                )
            }
        add {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                MenuBottomSheetItem(
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_delete,
                            contentDescription = stringResource(R.string.content_description_delete_the_message),
                        )
                    },
                    title = stringResource(R.string.label_delete),
                    onItemClick = onDeleteMessage
                )
            }
        }
    }
}

@Composable
private fun ConversationScreenContent(
    messages: List<Message>,
    onMessageChanged: (String) -> Unit,
    messageText: String,
    onSendButtonClicked: () -> Unit,
    onShowContextMenu: (Message) -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onError: (String) -> Unit
) {
    MessageComposer(
        content = {
            LazyColumn(
                reverseLayout = true,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
            ) {
                items(messages) { message ->
                    MessageItem(
                        message = message,
                        onLongClicked = { onShowContextMenu(message) }
                    )
                }
            }
        },
        messageText = messageText,
        onMessageChanged = onMessageChanged,
        onSendButtonClicked = onSendButtonClicked,
        onSendAttachment = onSendAttachment,
        onError = onError
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DeleteMessageDialog(
    state: DeleteMessageDialogState.Visible,
    onDialogDismiss: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
) {
    WireDialog(
        title = stringResource(R.string.delete_message_dialog_title),
        text = stringResource(R.string.delete_message_dialog_message),
        onDismiss = onDialogDismiss,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onDialogDismiss,
            text = stringResource(id = R.string.label_cancel),
            state = WireButtonState.Default
        ),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDeleteForMe,
            text = stringResource(R.string.label_delete_for_me),
            type = WireDialogButtonType.Primary,
            state = WireButtonState.Error
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = onDeleteForEveryone,
            text = stringResource(R.string.label_delete_for_everyone),
            type = WireDialogButtonType.Primary,
            state = if (state.loading) WireButtonState.Disabled else WireButtonState.Error,
            loading = state.loading
        ),
        buttonsHorizontalAlignment = false
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DeleteMessageForYourselfDialog(
    state: DeleteMessageDialogState.Visible,
    onDialogDismiss: () -> Unit,
    onDeleteForMe: () -> Unit,
) {
    WireDialog(
        title = stringResource(R.string.delete_message_for_yourself_dialog_title),
        text = stringResource(R.string.delete_message_for_yourself_dialog_message),
        onDismiss = onDialogDismiss,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onDialogDismiss,
            text = stringResource(id = R.string.label_cancel),
            state = WireButtonState.Default
        ),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDeleteForMe,
            text = stringResource(R.string.label_delete_for_me),
            type = WireDialogButtonType.Primary,
            state = if (state.loading) WireButtonState.Disabled else WireButtonState.Error,
            loading = state.loading
        )
    )
}

@Preview
@Composable
fun ConversationScreenPreview() {
    ConversationScreen(
        ConversationViewState(
            conversationName = "Some test conversation",
            messages = mockMessages,
        ),
        {}, {}, {}, {}
    ) {}
}
