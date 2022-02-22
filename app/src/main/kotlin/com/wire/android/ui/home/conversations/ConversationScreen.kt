package com.wire.android.ui.home.conversations

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.home.conversations.mock.mockMessages
import com.wire.android.ui.home.conversations.model.Message
import com.wire.android.ui.home.messagecomposer.MessageComposer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationViewModel: ConversationViewModel,
) {
    val uiState by conversationViewModel.conversationViewState.collectAsState()

    ConversationScreen(
        conversationViewState = uiState,
        onMessageChanged = { message -> conversationViewModel.onMessageChanged(message) },
        onSendButtonClicked = { conversationViewModel.sendMessage() },
        onBackButtonClick = { conversationViewModel.navigateBack() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationScreen(
    conversationViewState: ConversationViewState,
    onMessageChanged: (String) -> Unit,
    onSendButtonClicked: () -> Unit,
    onBackButtonClick: () -> Unit
) {
    with(conversationViewState) {
        Scaffold(
            topBar = { ConversationScreenTopAppBar(conversationName, onBackButtonClick, {}, {}, {}) },
            content = {
                ConversationScreenContent(
                    messages = messages,
                    onMessageChanged = onMessageChanged,
                    onSendButtonClicked = onSendButtonClicked
                )
            }
        )
    }
}

@Composable
private fun ConversationScreenContent(
    messages: List<Message>,
    onMessageChanged: (String) -> Unit,
    onSendButtonClicked: () -> Unit
) {
    MessageComposer(
        content = {
            LazyColumn {
                items(messages) { message ->
                    MessageItem(message = message)
                }
            }
        },
        onMessageChanged = onMessageChanged,
        onSendButtonClicked = onSendButtonClicked
    )
}

@Preview
@Composable
fun ConversationScreenPreview() {
    ConversationScreen(
        ConversationViewState(
            conversationName = "Some test conversation",
            messages = mockMessages,
        ), {}, {}, {})
}

