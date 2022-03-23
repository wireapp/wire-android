package com.wire.android.ui.home.conversations

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.home.conversations.mock.mockMessages
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.Message
import com.wire.android.ui.home.messagecomposer.MessageComposer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationViewModel: ConversationViewModel,
) {
    val uiState = conversationViewModel.conversationViewState

    ConversationScreen(
        conversationViewState = uiState,
        onMessageChanged = { message -> conversationViewModel.onMessageChanged(message) },
        onSendButtonClicked = { conversationViewModel.sendMessage() },
        onSendAttachment = { attachmentBundle -> conversationViewModel.sendAttachmentMessage(attachmentBundle) },
        onBackButtonClick = { conversationViewModel.navigateBack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationScreen(
    conversationViewState: ConversationViewState,
    onMessageChanged: (TextFieldValue) -> Unit,
    onSendButtonClicked: () -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onBackButtonClick: () -> Unit
) {
    with(conversationViewState) {
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            topBar = { ConversationScreenTopAppBar(conversationName, onBackButtonClick, {}, {}, {}) },
            snackbarHost = {
                SwipeDismissSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(Alignment.Bottom)
                )
            },
            content = {
                ConversationScreenContent(
                    messages = messages,
                    onMessageChanged = onMessageChanged,
                    messageText = conversationViewState.messageText,
                    onSendButtonClicked = onSendButtonClicked,
                    onSendAttachment = onSendAttachment,
                    snackbarHostState = snackbarHostState
                )
            }
        )
    }
}

@Composable
private fun ConversationScreenContent(
    messages: List<Message>,
    onMessageChanged: (TextFieldValue) -> Unit,
    messageText: TextFieldValue,
    onSendButtonClicked: () -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    MessageComposer(
        content = {
            LazyColumn(
                reverseLayout = true,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
            ) {
                items(messages) { message -> MessageItem(message = message) }
            }
        },
        messageText = messageText,
        onMessageChanged = onMessageChanged,
        onSendButtonClicked = onSendButtonClicked,
        onSendAttachment = onSendAttachment,
        snackbarHostState = snackbarHostState
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
        {}, {}, {}
    ) {}
}
