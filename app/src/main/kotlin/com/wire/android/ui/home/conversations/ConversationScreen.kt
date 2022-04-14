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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialog
import com.wire.android.ui.home.conversations.mock.getMockedMessages
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.MessageViewWrapper
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.messagecomposer.MessageComposeInputState
import com.wire.android.ui.home.messagecomposer.MessageComposer
import kotlinx.coroutines.launch

@Composable
fun ConversationScreen(
    conversationViewModel: ConversationViewModel
) {
    val uiState = conversationViewModel.conversationViewState

    ConversationScreen(
        conversationViewState = uiState,
        onMessageChanged = { message -> conversationViewModel.onMessageChanged(message) },
        onSendButtonClicked = { conversationViewModel.sendMessage() },
        onSendAttachment = { attachmentBundle -> conversationViewModel.sendAttachmentMessage(attachmentBundle) },
        onBackButtonClick = { conversationViewModel.navigateBack() },
        onDeleteMessage = conversationViewModel::showDeleteMessageDialog,
        onCallStart = { conversationViewModel.navigateToInitiatingCallScreen() }
    )
    DeleteMessageDialog(
        conversationViewModel = conversationViewModel
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
    onDeleteMessage: (String, Boolean) -> Unit,
    onCallStart: () -> Unit
) {
    val conversationScreenState = rememberConversationScreenState()
    val scope = rememberCoroutineScope()

    with(conversationViewState) {
        MenuModalSheetLayout(
            sheetState = conversationScreenState.modalBottomSheetState,
            menuItems = EditMessageMenuItems(
                isMyMessage = conversationScreenState.isSelectedMessageMyMessage,
                onCopyMessage = conversationScreenState::copyMessage,
                onDeleteMessage = {
                    onDeleteMessage(
                        conversationScreenState.selectedMessage?.messageHeader!!.messageId,
                        conversationScreenState.isSelectedMessageMyMessage
                    )
                }
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
    isMyMessage: Boolean,
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
        if (isMyMessage)
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
    messages: List<MessageViewWrapper>,
    onMessageChanged: (String) -> Unit,
    messageText: String,
    onSendButtonClicked: () -> Unit,
    onShowContextMenu: (MessageViewWrapper) -> Unit,
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

@Preview
@Composable
fun ConversationScreenPreview() {
    ConversationScreen(
        ConversationViewState(
            conversationName = "Some test conversation",
            messages = getMockedMessages(),
        ),
        {}, {}, {}, {}, { _: String, _: Boolean -> }
    ) {}
}
