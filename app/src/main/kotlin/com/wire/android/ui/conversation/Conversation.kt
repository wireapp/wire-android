package com.wire.android.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.theme.WireColor

@Preview
@Composable
fun ConversationScreen(viewModel: ConversationViewModel = ConversationViewModel()) {
    val uiState by viewModel.state.collectAsState()

    ConversationContent(uiState = uiState)
}

@Composable
private fun ConversationContent(uiState: ConversationState) {
    Scaffold(
        floatingActionButton = { ConversationListFloatingActionButton() },
        content = { ConversationList(uiState.conversations) }
    )
}

@Composable
private fun ConversationListFloatingActionButton() {
    ExtendedFloatingActionButton(
        shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 30)),
        icon = { Icon(Icons.Filled.Add, "") },
        text = { Text(text = stringResource(R.string.label_new)) },
        onClick = { })
}

@Composable
private fun ConversationList(conversations: List<Conversation>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        items(conversations) { conversation ->
            ConversationRow(conversation)
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White)
            .padding(16.dp)
    ) {
        Text(text = conversation.name, fontWeight = FontWeight.W500)
    }
}

data class ConversationState(
    val conversations: List<Conversation> = emptyList()
)

data class Conversation(val name: String)
