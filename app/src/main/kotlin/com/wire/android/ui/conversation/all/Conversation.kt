package com.wire.android.ui.conversation.all

import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.conversation.common.EventBadgeFactory
import com.wire.android.ui.common.WhiteBackgroundWrapper
import com.wire.android.ui.conversation.all.model.Conversation
import com.wire.android.ui.conversation.all.model.NewActivity
import com.wire.android.ui.conversation.common.FolderHeader
import com.wire.android.ui.conversation.common.UserInfoLabel

@Preview
@Composable
fun Conversation(viewModel: ConversationViewModel = ConversationViewModel()) {
    val uiState by viewModel.state.collectAsState()

    ConversationScreen(uiState = uiState)
}

@Composable
private fun ConversationScreen(uiState: ConversationState) {
    Scaffold(
        floatingActionButton = { ConversationListFloatingActionButton() },
        content = { ConversationContent(uiState) }
    )
}

@Composable
private fun ConversationContent(uiState: ConversationState) {
    with(uiState) {
        LazyColumn {
            if (newActivities.isNotEmpty()) {
                item { FolderHeader(name = stringResource(R.string.conversation_label_new_activity)) }
                items(newActivities) { newActivity ->
                    NewActivityRowItem(
                        newActivity = newActivity
                    )
                }
            }

            if (conversations.isNotEmpty()) {
                conversations.forEach { (conversationFolder, conversationList) ->
                    item { FolderHeader(name = conversationFolder.folderName) }
                    items(conversationList) { conversation ->
                        ConversationRowItem(
                            conversation = conversation
                        )
                    }
                }
            }
        }
    }
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
private fun NewActivityRowItem(newActivity: NewActivity) {
    WhiteBackgroundWrapper(
        content = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserInfoLabel(conversationInfo = newActivity.conversation.conversationInfo, Modifier.padding(16.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    EventBadgeFactory(
                        eventType = newActivity.eventType,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                    )
                }
            }
        }, modifier = Modifier.padding(0.5.dp)
    )
}

@Composable
private fun ConversationRowItem(conversation: Conversation) {
    WhiteBackgroundWrapper(
        content = {
            UserInfoLabel(conversationInfo = conversation.conversationInfo, Modifier.padding(16.dp))
        }, modifier = Modifier.padding(0.5.dp)
    )
}



