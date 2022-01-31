package com.wire.android.ui.home.conversations

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.home.conversations.mock.mockMessages
import com.wire.android.ui.home.conversations.model.ConversationView
import com.wire.android.ui.home.conversations.model.Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationView: ConversationView,
    onBackButtonClick: () -> Unit
) {
    with(conversationView) {
        Scaffold(
            topBar = { ConversationScreenTopAppBar(name, onBackButtonClick, {}, {}, {}) },
            content = {
                ConversationScreenContent(messages = messages)
            })
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
    ConversationScreen(ConversationView(name = "Conversation title", messages = mockMessages)) {}
}

