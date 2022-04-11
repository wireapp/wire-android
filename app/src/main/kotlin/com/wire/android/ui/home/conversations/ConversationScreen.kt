package com.wire.android.ui.home.conversations

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.home.conversations.mock.mockMessages
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.Message
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.messagecomposer.MessageComposeInputState
import com.wire.android.ui.home.messagecomposer.MessageComposer
import com.wire.android.util.dialogErrorStrings
import com.wire.android.util.permission.rememberCallingRecordAudioRequestFlow
import kotlinx.coroutines.launch

@Composable
fun ConversationScreen(conversationViewModel: ConversationViewModel) {
    val audioPermissionCheck = AudioPermissionCheckFlow(conversationViewModel)
    val uiState = conversationViewModel.conversationViewState
    ConversationScreen(
        conversationViewState = uiState,
        onMessageChanged = { message -> conversationViewModel.onMessageChanged(message) },
        onSendButtonClicked = { conversationViewModel.sendMessage() },
        onSendAttachment = { attachmentBundle -> conversationViewModel.sendAttachmentMessage(attachmentBundle) },
        onBackButtonClick = { conversationViewModel.navigateBack() },
        onDeleteMessage = conversationViewModel::showDeleteMessageDialog,
        onCallStart = {
            audioPermissionCheck.launch()
        }
    )
    DeleteMessageDialog(
        conversationViewModel = conversationViewModel
    )
}

@Composable
private fun AudioPermissionCheckFlow(conversationViewModel: ConversationViewModel) =
    rememberCallingRecordAudioRequestFlow(onAudioPermissionGranted = {
        conversationViewModel.conversationId?.let { conversationViewModel.navigateToInitiatingCallScreen(it) }
    }) {
        //TODO display an error dialog
    }

@Composable
private fun DeleteMessageDialog(
    conversationViewModel: ConversationViewModel
) {
    val deleteMessageDialogsState = conversationViewModel.deleteMessageDialogsState

    if (deleteMessageDialogsState is DeleteMessageDialogsState.States) {
        when {
            deleteMessageDialogsState.forEveryone is DeleteMessageDialogActiveState.Visible -> {
                DeleteMessageDialog(
                    state = deleteMessageDialogsState.forEveryone,
                    onDialogDismiss = conversationViewModel::onDialogDismissed,
                    onDeleteForMe = conversationViewModel::showDeleteMessageForYourselfDialog,
                    onDeleteForEveryone = conversationViewModel::deleteMessage,
                )
                if (deleteMessageDialogsState.forEveryone.error is DeleteMessageError.GenericError) {
                    DeleteMessageErrorDialog(deleteMessageDialogsState.forEveryone.error, conversationViewModel::clearDeleteMessageError)
                }
            }
            deleteMessageDialogsState.forYourself is DeleteMessageDialogActiveState.Visible -> {

                if (deleteMessageDialogsState.forYourself.error is DeleteMessageError.GenericError) {
                    DeleteMessageErrorDialog(deleteMessageDialogsState.forYourself.error, conversationViewModel::clearDeleteMessageError)
                } else {
                    DeleteMessageForYourselfDialog(
                        state = deleteMessageDialogsState.forYourself,
                        onDialogDismiss = conversationViewModel::onDialogDismissed,
                        onDeleteForMe = conversationViewModel::deleteMessage
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteMessageErrorDialog(error: DeleteMessageError.GenericError, onDialogDismiss: () -> Unit) {
    val (title, message) = error.coreFailure.dialogErrorStrings(
        LocalContext.current.resources
    )
    WireDialog(
        title = title,
        text = message,
        onDismiss = onDialogDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDialogDismiss,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
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
    onCallStart: () -> Unit
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
                            onVideoButtonClick = { onCallStart() }
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
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    MessageComposer(
        content = {
            LazyColumn(
                state = lazyListState,
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
        onError = onError,
        onMessageComposerInputStateChange = { messageComposerState ->
            if (messageComposerState.to == MessageComposeInputState.Active
                && messageComposerState.from == MessageComposeInputState.Enabled
            ) {
                coroutineScope.launch { lazyListState.animateScrollToItem(messages.size) }
            }
        }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DeleteMessageDialog(
    state: DeleteMessageDialogActiveState.Visible,
    onDialogDismiss: () -> Unit,
    onDeleteForMe: (String) -> Unit,
    onDeleteForEveryone: (String, Boolean) -> Unit,
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
            onClick = { onDeleteForMe(state.messageId) },
            text = stringResource(R.string.label_delete_for_me),
            type = WireDialogButtonType.Primary,
            state = WireButtonState.Error
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = { onDeleteForEveryone(state.messageId, true) },
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
    state: DeleteMessageDialogActiveState.Visible,
    onDialogDismiss: () -> Unit,
    onDeleteForMe: (String, Boolean) -> Unit,
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
            onClick = { onDeleteForMe(state.messageId, false) },
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
        {}, {}, {}, {}, {}
    ) {}
}
