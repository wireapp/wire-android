package com.wire.android.ui.home.conversations

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.home.conversations.mock.mockMessages
import com.wire.android.ui.home.conversations.model.Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationViewModel: ConversationViewModel,
) {
    val uiState by conversationViewModel.conversationState.collectAsState()

    ConversationScreen(uiState) { conversationViewModel.navigateBack() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationScreen(
    conversationState: ConversationState,
    onBackButtonClick: () -> Unit
) {
    with(conversationState) {
        Scaffold(
            topBar = { ConversationScreenTopAppBar(conversationName, onBackButtonClick, {}, {}, {}) },
            content = {
                ConversationScreenContent(messages)
            }
        )
    }
}

@Composable
private fun ConversationScreenContent(messages: List<Message>) {
    SurfaceBackgroundWrapper {
        LazyColumn {
            items(messages) { message ->
                MessageItem(message = message)
            }
        }
    }
}

@Preview
@Composable
fun ConversationScreenPreview() {
    ConversationScreen(
        ConversationState(
            conversationName = "Some test conversation",
            messages = mockMessages
        )
    ) {}
}

